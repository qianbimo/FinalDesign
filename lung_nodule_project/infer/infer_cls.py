"""
分类推理脚本: 输入单个 ROI，输出 benign/malignant 概率。
输出默认落到 workspace/runs/<infer_cls_时间>/predictions。
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime
from typing import Dict

import numpy as np
import SimpleITK as sitk
import torch
import yaml
from scipy.ndimage import zoom

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)

from models.cnn_transformer_baseline import CNNOnlyClassifier, CNNTransformerClassifier
from models.mamba_classifier import MambaClassifier


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "infer_cls") -> str:
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


def _norm_roi(x: np.ndarray) -> np.ndarray:
    x = x.astype(np.float32)
    x = np.nan_to_num(x, nan=0.0, posinf=0.0, neginf=0.0)
    if x.min() >= 0 and x.max() <= 1.0:
        return x
    lo, hi = np.percentile(x, [1, 99])
    x = np.clip(x, lo, hi)
    mn, mx = float(x.min()), float(x.max())
    if mx > mn:
        x = (x - mn) / (mx - mn)
    else:
        x = np.zeros_like(x, dtype=np.float32)
    return x.astype(np.float32)


def _resize_64(roi_zyx: np.ndarray) -> np.ndarray:
    tz, ty, tx = 64, 64, 64
    zf = tz / max(1, roi_zyx.shape[0])
    yf = ty / max(1, roi_zyx.shape[1])
    xf = tx / max(1, roi_zyx.shape[2])
    return zoom(roi_zyx, (zf, yf, xf), order=1).astype(np.float32)


def load_roi(roi_path: str) -> np.ndarray:
    p = roi_path.lower()
    if p.endswith(".npy"):
        roi = np.load(roi_path).astype(np.float32)
    else:
        img = sitk.ReadImage(roi_path)
        roi = sitk.GetArrayFromImage(img).astype(np.float32)
    if roi.shape != (64, 64, 64):
        roi = _resize_64(roi)
    return _norm_roi(roi)


def _build_model(model_type: str, cfg: Dict, num_classes: int):
    mcfg = cfg["model"]
    model_type = model_type.lower()
    if model_type == "cnn":
        return CNNOnlyClassifier(
            in_channels=int(mcfg["in_channels"]),
            num_classes=num_classes,
            backbone_depth=int(mcfg.get("backbone_depth", 18)),
            feat_dim=int(mcfg.get("feat_dim", 256)),
            dropout=float(mcfg.get("dropout", 0.2)),
        )
    if model_type == "cnn_transformer":
        return CNNTransformerClassifier(
            in_channels=int(mcfg["in_channels"]),
            num_classes=num_classes,
            backbone_depth=int(mcfg.get("backbone_depth", 18)),
            hidden_dim=int(mcfg.get("feat_dim", 256)),
            num_layers=2,
            dropout=float(mcfg.get("dropout", 0.2)),
        )
    return MambaClassifier(
        in_channels=int(mcfg["in_channels"]),
        num_classes=num_classes,
        backbone_depth=int(mcfg.get("backbone_depth", 18)),
        mamba_dim=int(mcfg.get("mamba_dim", 256)),
        mamba_layers=int(mcfg.get("mamba_layers", 2)),
        dropout=float(mcfg.get("dropout", 0.2)),
    )


def infer_cls(roi_path: str, config: Dict, ckpt_path: str, model_type: str) -> Dict:
    device = _pick_device(str(config.get("device", "auto")).lower())
    loss_type = str(config["train"].get("loss_type", "ce")).lower()
    num_classes = 1 if loss_type == "bce" else int(config["model"].get("num_classes", 2))
    model = _build_model(model_type, config, num_classes=num_classes).to(device)

    state = torch.load(ckpt_path, map_location="cpu")
    if isinstance(state, dict) and "model" in state:
        state = state["model"]
    model.load_state_dict(state, strict=False)
    model.eval()

    roi = load_roi(roi_path)
    x = torch.from_numpy(roi[None, None, ...]).to(device)
    with torch.no_grad():
        logits = model(x)
        if loss_type == "bce":
            logit = logits.squeeze(1) if logits.ndim == 2 else logits
            prob_malignant = float(torch.sigmoid(logit).item())
        else:
            prob_malignant = float(torch.softmax(logits, dim=1)[0, 1].item())

    pred = "malignant" if prob_malignant >= 0.5 else "benign"
    return {
        "pred_class": pred,
        "prob_malignant": prob_malignant,
        "prob_benign": 1.0 - prob_malignant,
        "model_type": model_type,
        "roi_path": roi_path,
    }


def save_cls_result(result: Dict, out_txt: str, out_json: str) -> None:
    _ensure_dir(os.path.dirname(out_txt) or ".")
    _ensure_dir(os.path.dirname(out_json) or ".")
    with open(out_txt, "w", encoding="utf-8") as f:
        f.write(f"Predicted Class: {result['pred_class']}\n")
        f.write(f"Malignant Probability: {result['prob_malignant']:.6f}\n")
        f.write(f"Benign Probability: {result['prob_benign']:.6f}\n")
        f.write(f"Model Type: {result['model_type']}\n")
        f.write(f"ROI Path: {result['roi_path']}\n")
    with open(out_json, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Classification inference")
    parser.add_argument("--config", type=str, default="configs/cls_config.yaml")
    parser.add_argument("--roi", type=str, required=True, help="ROI 文件(.npy/.nii/.nii.gz)")
    parser.add_argument("--ckpt", type=str, default="workspace/weights/cls_best_mamba.pth")
    parser.add_argument("--model", type=str, default="mamba", help="mamba / cnn / cnn_transformer")
    parser.add_argument("--out_txt", type=str, default="")
    parser.add_argument("--out_json", type=str, default="")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)

    run_dir = _resolve_run_dir("infer_cls")
    pred_dir = os.path.join(run_dir, "predictions")
    _ensure_dir(pred_dir)

    out_txt = args.out_txt.strip() if args.out_txt else ""
    out_json = args.out_json.strip() if args.out_json else ""
    if not out_txt:
        out_txt = os.path.join(pred_dir, "cls_result.txt")
    if not out_json:
        out_json = os.path.join(pred_dir, "cls_result.json")

    res = infer_cls(args.roi, cfg, args.ckpt, model_type=args.model)
    save_cls_result(res, out_txt=out_txt, out_json=out_json)
    print("分类推理完成:", res)
    print("run_dir:", run_dir)
