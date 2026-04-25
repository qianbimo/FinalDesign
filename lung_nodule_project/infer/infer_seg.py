"""
分割推理脚本: 输入单个 CT，输出 mask(.npy，可选 .nii.gz)。
输出默认落到 workspace/runs/<infer_seg_时间>/masks。
"""

from __future__ import annotations

import argparse
import os
import sys
from datetime import datetime
from typing import Dict, Optional, Tuple

import numpy as np
import SimpleITK as sitk
import torch
import yaml

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)

from models.resunet3d import ResUNet3D


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "infer_seg") -> str:
    run_root_env = os.environ.get("LUNG_RUN_DIR", "").strip()
    run_step_env = os.environ.get("LUNG_RUN_STEP", "").strip()
    if run_root_env:
        run_name = run_step_env if run_step_env else default_task
        run_dir = os.path.join(run_root_env, run_name)
    else:
        run_dir = os.path.join(".", "workspace", "runs", f"{default_task}_{_now_tag()}")
    _ensure_dir(run_dir)
    return run_dir


def _pick_device(device_cfg: str) -> torch.device:
    if device_cfg == "cuda":
        return torch.device("cuda")
    if device_cfg == "cpu":
        return torch.device("cpu")
    return torch.device("cuda" if torch.cuda.is_available() else "cpu")


def _resample_image(img: sitk.Image, target_spacing_xyz: Tuple[float, float, float]) -> sitk.Image:
    old_spacing = np.array(img.GetSpacing(), dtype=np.float64)
    old_size = np.array(img.GetSize(), dtype=np.int32)
    target_spacing = np.array(target_spacing_xyz, dtype=np.float64)
    new_size = np.maximum(1, np.round(old_size * old_spacing / target_spacing)).astype(np.int32)

    rf = sitk.ResampleImageFilter()
    rf.SetOutputSpacing(tuple(float(x) for x in target_spacing))
    rf.SetSize([int(v) for v in new_size.tolist()])
    rf.SetOutputOrigin(img.GetOrigin())
    rf.SetOutputDirection(img.GetDirection())
    rf.SetTransform(sitk.Transform())
    rf.SetInterpolator(sitk.sitkLinear)
    rf.SetDefaultPixelValue(-1024)
    return rf.Execute(img)


def _window_norm(vol_zyx: np.ndarray, hu_clip: Tuple[float, float]) -> np.ndarray:
    lo, hi = float(hu_clip[0]), float(hu_clip[1])
    x = np.clip(vol_zyx.astype(np.float32), lo, hi)
    x = (x - lo) / (hi - lo + 1e-8)
    return x.astype(np.float32)


def load_and_preprocess_ct(ct_path: str, seg_cfg: Dict) -> Tuple[np.ndarray, Dict]:
    """加载并预处理 CT，返回 zyx 体数据和元信息。支持 .mhd/.nii/.nii.gz/.npy。"""
    ext = ct_path.lower()
    target_spacing = tuple(seg_cfg["preprocess"].get("target_spacing_xyz", [1.0, 1.0, 1.0]))
    hu_clip = tuple(seg_cfg["preprocess"].get("hu_clip", [-1200, 600]))

    meta = {
        "origin": (0.0, 0.0, 0.0),
        "spacing": target_spacing,
        "direction": (1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0),
    }
    if ext.endswith(".npy"):
        vol = np.load(ct_path).astype(np.float32)
    else:
        img = sitk.ReadImage(ct_path)
        img = _resample_image(img, target_spacing_xyz=target_spacing)
        vol = sitk.GetArrayFromImage(img).astype(np.float32)
        meta["origin"] = img.GetOrigin()
        meta["spacing"] = img.GetSpacing()
        meta["direction"] = img.GetDirection()

    vol = _window_norm(vol, hu_clip=hu_clip)
    return vol, meta


def load_seg_model(seg_cfg: Dict, ckpt_path: str, device: torch.device) -> ResUNet3D:
    mcfg = seg_cfg["model"]
    model = ResUNet3D(
        in_channels=int(mcfg["in_channels"]),
        out_channels=int(mcfg["out_channels"]),
        base_channels=int(mcfg["base_channels"]),
    ).to(device)
    state = torch.load(ckpt_path, map_location="cpu")
    if isinstance(state, dict) and "model" in state:
        state = state["model"]
    model.load_state_dict(state, strict=False)
    model.eval()
    return model


def run_sliding_window_inference(
    model: ResUNet3D,
    volume_zyx: np.ndarray,
    patch_zyx: Tuple[int, int, int],
    stride_zyx: Tuple[int, int, int],
    device: torch.device,
    prob_threshold: float = 0.5,
) -> Tuple[np.ndarray, np.ndarray]:
    """滑窗推理并对重叠区域做平均。"""
    vol = volume_zyx.astype(np.float32)
    d, h, w = vol.shape
    pd, ph, pw = [int(v) for v in patch_zyx]
    sd, sh, sw = [int(v) for v in stride_zyx]

    prob_sum = np.zeros_like(vol, dtype=np.float32)
    prob_cnt = np.zeros_like(vol, dtype=np.float32)

    z_starts = list(range(0, max(1, d - pd + 1), max(1, sd)))
    y_starts = list(range(0, max(1, h - ph + 1), max(1, sh)))
    x_starts = list(range(0, max(1, w - pw + 1), max(1, sw)))
    if len(z_starts) == 0 or z_starts[-1] != max(0, d - pd):
        z_starts.append(max(0, d - pd))
    if len(y_starts) == 0 or y_starts[-1] != max(0, h - ph):
        y_starts.append(max(0, h - ph))
    if len(x_starts) == 0 or x_starts[-1] != max(0, w - pw):
        x_starts.append(max(0, w - pw))

    with torch.no_grad():
        for z in z_starts:
            for y in y_starts:
                for x in x_starts:
                    patch = vol[z : z + pd, y : y + ph, x : x + pw]
                    z2 = min(d, z + pd)
                    y2 = min(h, y + ph)
                    x2 = min(w, x + pw)
                    vd = z2 - z
                    vh = y2 - y
                    vw = x2 - x
                    if patch.shape != (pd, ph, pw):
                        pad = np.zeros((pd, ph, pw), dtype=np.float32)
                        pad[:vd, :vh, :vw] = patch
                        patch = pad
                    inp = torch.from_numpy(patch[None, None, ...]).to(device)
                    logits = model(inp)
                    prob = torch.sigmoid(logits)[0, 0].cpu().numpy().astype(np.float32)
                    prob_sum[z:z2, y:y2, x:x2] += prob[:vd, :vh, :vw]
                    prob_cnt[z:z2, y:y2, x:x2] += 1.0

    prob_avg = prob_sum / np.maximum(prob_cnt, 1e-6)
    mask = (prob_avg > float(prob_threshold)).astype(np.uint8)
    return mask, prob_avg


def save_mask_outputs(mask_zyx: np.ndarray, out_npy: str, out_nii: Optional[str], meta: Dict) -> None:
    _ensure_dir(os.path.dirname(out_npy) or ".")
    np.save(out_npy, mask_zyx.astype(np.uint8))
    if out_nii:
        _ensure_dir(os.path.dirname(out_nii) or ".")
        img = sitk.GetImageFromArray(mask_zyx.astype(np.uint8))
        img.SetOrigin(tuple(meta["origin"]))
        img.SetSpacing(tuple(meta["spacing"]))
        img.SetDirection(tuple(meta["direction"]))
        sitk.WriteImage(img, out_nii)


def infer_seg(
    ct_path: str,
    config: Dict,
    ckpt_path: str,
    out_npy: str,
    out_nii: Optional[str] = None,
) -> Dict:
    device = _pick_device(str(config.get("device", "auto")).lower())
    vol, meta = load_and_preprocess_ct(ct_path, config)
    model = load_seg_model(config, ckpt_path, device)
    patch_zyx = tuple(config.get("infer", {}).get("patch_size_zyx", [96, 96, 96]))
    stride_zyx = tuple(config.get("infer", {}).get("stride_zyx", [64, 64, 64]))
    prob_threshold = float(config.get("infer", {}).get("prob_threshold", 0.5))
    mask, prob_map = run_sliding_window_inference(
        model,
        vol,
        patch_zyx,
        stride_zyx,
        device,
        prob_threshold=prob_threshold,
    )
    save_mask_outputs(mask, out_npy=out_npy, out_nii=out_nii, meta=meta)
    return {
        "mask_path": out_npy,
        "mask_nii_path": out_nii,
        "volume": vol,
        "mask": mask,
        "prob_map": prob_map,
        "prob_threshold": prob_threshold,
        "meta": meta,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Segmentation inference")
    parser.add_argument("--config", type=str, default="configs/seg_config.yaml")
    parser.add_argument("--ct", type=str, required=True, help="输入 CT 文件(.mhd/.nii/.nii.gz/.npy)")
    parser.add_argument("--ckpt", type=str, default="workspace/weights/seg_best.pth", help="分割权重")
    parser.add_argument("--out_npy", type=str, default="")
    parser.add_argument("--out_nii", type=str, default="", help="可选，输出 nii.gz")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)

    run_dir = _resolve_run_dir("infer_seg")
    mask_dir = os.path.join(run_dir, "masks")
    _ensure_dir(mask_dir)

    out_npy = args.out_npy.strip() if args.out_npy else ""
    out_nii = args.out_nii.strip() if args.out_nii else ""
    if not out_npy:
        out_npy = os.path.join(mask_dir, "pred_mask.npy")
    out_nii_path: Optional[str] = out_nii if out_nii else os.path.join(mask_dir, "pred_mask.nii.gz")

    result = infer_seg(args.ct, cfg, args.ckpt, out_npy=out_npy, out_nii=out_nii_path)
    print("分割推理完成:", result["mask_path"])
    print("run_dir:", run_dir)
