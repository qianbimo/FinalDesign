"""
完整串联推理: CT -> 分割 -> ROI 提取 -> 分类 -> 可视化与结果导出。
输出默认落到 workspace/runs/<pipeline_predict_时间>/。
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime
from typing import Dict, Optional, Tuple

import matplotlib.pyplot as plt
import numpy as np
import yaml
from scipy.ndimage import label as cc_label

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)

from infer.infer_cls import infer_cls, save_cls_result
from infer.infer_seg import infer_seg
from infer.visualize import save_ct_slice_png, save_overlay_png, save_roi_center_png


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "pipeline_predict") -> str:
    run_root_env = os.environ.get("LUNG_RUN_DIR", "").strip()
    run_step_env = os.environ.get("LUNG_RUN_STEP", "").strip()
    if run_root_env:
        run_name = run_step_env if run_step_env else default_task
        run_dir = os.path.join(run_root_env, run_name)
    else:
        run_dir = os.path.join(".", "workspace", "runs", f"{default_task}_{_now_tag()}")
    _ensure_dir(run_dir)
    return run_dir


def _crop_with_padding(
    vol_zyx: np.ndarray,
    center_zyx: Tuple[int, int, int],
    size_zyx: Tuple[int, int, int] = (64, 64, 64),
) -> np.ndarray:
    d, h, w = size_zyx
    cz, cy, cx = [int(v) for v in center_zyx]
    z1, y1, x1 = cz - d // 2, cy - h // 2, cx - w // 2
    z2, y2, x2 = z1 + d, y1 + h, x1 + w

    out = np.zeros((d, h, w), dtype=vol_zyx.dtype)
    sz1, sy1, sx1 = max(0, z1), max(0, y1), max(0, x1)
    sz2, sy2, sx2 = min(vol_zyx.shape[0], z2), min(vol_zyx.shape[1], y2), min(vol_zyx.shape[2], x2)
    dz1, dy1, dx1 = sz1 - z1, sy1 - y1, sx1 - x1
    dz2, dy2, dx2 = dz1 + (sz2 - sz1), dy1 + (sy2 - sy1), dx1 + (sx2 - sx1)
    if sz1 < sz2 and sy1 < sy2 and sx1 < sx2:
        out[dz1:dz2, dy1:dy2, dx1:dx2] = vol_zyx[sz1:sz2, sy1:sy2, sx1:sx2]
    return out


def _largest_component_center(mask_zyx: np.ndarray) -> Tuple[Tuple[int, int, int], Optional[Tuple[int, int, int, int, int, int]]]:
    """找最大连通域中心，返回中心点和 bbox(z1,z2,y1,y2,x1,x2)。"""
    mask = (mask_zyx > 0).astype(np.uint8)
    cc, n = cc_label(mask)
    if n <= 0:
        center = (mask.shape[0] // 2, mask.shape[1] // 2, mask.shape[2] // 2)
        return center, None

    best_id = 1
    best_size = 0
    for i in range(1, n + 1):
        s = int((cc == i).sum())
        if s > best_size:
            best_size = s
            best_id = i

    pts = np.argwhere(cc == best_id)
    z1, y1, x1 = pts.min(axis=0).tolist()
    z2, y2, x2 = pts.max(axis=0).tolist()
    cz, cy, cx = pts.mean(axis=0)
    center = (int(round(cz)), int(round(cy)), int(round(cx)))
    return center, (z1, z2, y1, y2, x1, x2)


def _save_annotated_png(
    volume_zyx: np.ndarray,
    mask_zyx: np.ndarray,
    out_png: str,
    pred_class: str,
    prob: float,
    bbox_zyx: Optional[Tuple[int, int, int, int, int, int]],
) -> None:
    """在原图上绘制分割区域与分类结果。"""
    _ensure_dir(os.path.dirname(out_png) or ".")
    vol = volume_zyx.astype(np.float32)
    mn, mx = float(vol.min()), float(vol.max())
    if mx > mn:
        vol = (vol - mn) / (mx - mn)
    else:
        vol = np.zeros_like(vol, dtype=np.float32)

    if bbox_zyx is None:
        z = vol.shape[0] // 2
        y1, y2, x1, x2 = 10, vol.shape[1] - 10, 10, vol.shape[2] - 10
    else:
        z1, z2, y1, y2, x1, x2 = bbox_zyx
        z = (z1 + z2) // 2

    img = vol[z]
    m = (mask_zyx[z] > 0).astype(np.float32)

    plt.figure(figsize=(7, 7))
    ax = plt.gca()
    ax.imshow(img, cmap="gray")
    ax.imshow(m, cmap="Reds", alpha=0.30)
    rect = plt.Rectangle((x1, y1), max(2, x2 - x1), max(2, y2 - y1), fill=False, edgecolor="lime", linewidth=2.0)
    ax.add_patch(rect)
    ax.set_title(f"Pred: {pred_class} | P(malignant)={prob:.4f}")
    ax.axis("off")
    plt.tight_layout()
    plt.savefig(out_png, dpi=220)
    plt.close()


def pipeline_predict(
    ct_path: str,
    seg_config_path: str,
    cls_config_path: str,
    seg_ckpt: str,
    cls_ckpt: str,
    model_type: str,
    out_root: str,
) -> Dict:
    out_mask_dir = os.path.join(out_root, "masks")
    out_fig_dir = os.path.join(out_root, "figures")
    out_pred_dir = os.path.join(out_root, "predictions")
    _ensure_dir(out_mask_dir)
    _ensure_dir(out_fig_dir)
    _ensure_dir(out_pred_dir)

    with open(seg_config_path, "r", encoding="utf-8") as f:
        seg_cfg = yaml.safe_load(f)

    seg_npy = os.path.join(out_mask_dir, "pipeline_mask.npy")
    seg_nii = os.path.join(out_mask_dir, "pipeline_mask.nii.gz")
    seg_out = infer_seg(ct_path, seg_cfg, seg_ckpt, out_npy=seg_npy, out_nii=seg_nii)
    volume_zyx = seg_out["volume"]
    mask_zyx = seg_out["mask"]

    center, bbox_zyx = _largest_component_center(mask_zyx)
    roi_zyx = _crop_with_padding(volume_zyx, center_zyx=center, size_zyx=(64, 64, 64))
    roi_path = os.path.join(out_pred_dir, "pipeline_roi.npy")
    np.save(roi_path, roi_zyx.astype(np.float32))

    with open(cls_config_path, "r", encoding="utf-8") as f:
        cls_cfg = yaml.safe_load(f)
    cls_result = infer_cls(roi_path, cls_cfg, cls_ckpt, model_type=model_type)

    ct_png = os.path.join(out_fig_dir, "pipeline_ct_slice.png")
    ov_png = os.path.join(out_fig_dir, "pipeline_overlay.png")
    roi_png = os.path.join(out_fig_dir, "pipeline_roi.png")
    ann_png = os.path.join(out_fig_dir, "pipeline_annotated.png")
    save_ct_slice_png(volume_zyx, ct_png, slice_idx=center[0])
    save_overlay_png(volume_zyx, mask_zyx, ov_png, slice_idx=center[0])
    save_roi_center_png(roi_zyx, roi_png, pred_class=cls_result["pred_class"], prob=cls_result["prob_malignant"])
    _save_annotated_png(
        volume_zyx,
        mask_zyx,
        ann_png,
        pred_class=cls_result["pred_class"],
        prob=cls_result["prob_malignant"],
        bbox_zyx=bbox_zyx,
    )

    out_txt = os.path.join(out_pred_dir, "pipeline_result.txt")
    out_json = os.path.join(out_pred_dir, "pipeline_result.json")
    final_result = {
        "ct_path": ct_path,
        "mask_npy": seg_npy,
        "mask_nii": seg_nii,
        "roi_path": roi_path,
        "model_type": model_type,
        "pred_class": cls_result["pred_class"],
        "prob_malignant": cls_result["prob_malignant"],
        "prob_benign": cls_result["prob_benign"],
        "figures": {
            "ct_slice": ct_png,
            "overlay": ov_png,
            "roi": roi_png,
            "annotated": ann_png,
        },
        "out_root": out_root,
    }
    save_cls_result(final_result, out_txt=out_txt, out_json=out_json)
    return final_result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Pipeline predict: seg + cls")
    parser.add_argument("--ct", type=str, required=True, help="输入 CT 文件")
    parser.add_argument("--seg_config", type=str, default="configs/seg_config.yaml")
    parser.add_argument("--cls_config", type=str, default="configs/cls_config.yaml")
    parser.add_argument("--seg_ckpt", type=str, default="workspace/runs/full_full_mamba_20260423_170713/train_seg/checkpoints/seg_best.pth") #workspace/weights/seg_best.pth
    parser.add_argument("--cls_ckpt", type=str, default="workspace/runs/full_full_mamba_20260423_170713/train_cls/checkpoints/cls_best_mamba.pth")#"workspace/runs/full_full_mamba_20260423_170713/train_cls/checkpoints/cls_best_mamba.pth"
    parser.add_argument("--model", type=str, default="mamba", help="mamba/cnn/cnn_transformer")
    parser.add_argument("--out_root", type=str, default="", help="输出根目录(默认自动使用 runs)")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    out_root = args.out_root.strip() if args.out_root else ""
    if not out_root:
        out_root = _resolve_run_dir("pipeline_predict")

    result = pipeline_predict(
        ct_path=args.ct,
        seg_config_path=args.seg_config,
        cls_config_path=args.cls_config,
        seg_ckpt=args.seg_ckpt,
        cls_ckpt=args.cls_ckpt,
        model_type=args.model,
        out_root=out_root,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))
