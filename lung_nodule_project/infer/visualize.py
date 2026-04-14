"""
推理可视化工具。
"""

from __future__ import annotations

import os
from typing import Optional

import matplotlib.pyplot as plt
import numpy as np


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _norm01(x: np.ndarray) -> np.ndarray:
    x = x.astype(np.float32)
    x = np.nan_to_num(x, nan=0.0, posinf=0.0, neginf=0.0)
    lo, hi = np.percentile(x, [1, 99])
    x = np.clip(x, lo, hi)
    mn, mx = float(x.min()), float(x.max())
    if mx > mn:
        x = (x - mn) / (mx - mn)
    else:
        x = np.zeros_like(x, dtype=np.float32)
    return x


def save_ct_slice_png(volume_zyx: np.ndarray, out_png: str, slice_idx: Optional[int] = None) -> int:
    """保存 CT 单层图像。"""
    _ensure_dir(os.path.dirname(out_png) or ".")
    vol = _norm01(volume_zyx)
    z = int(vol.shape[0] // 2 if slice_idx is None else np.clip(slice_idx, 0, vol.shape[0] - 1))
    img = vol[z]
    plt.figure(figsize=(6, 6))
    plt.imshow(img, cmap="gray")
    plt.axis("off")
    plt.title(f"CT Slice z={z}")
    plt.tight_layout()
    plt.savefig(out_png, dpi=220)
    plt.close()
    return z


def save_overlay_png(volume_zyx: np.ndarray, mask_zyx: np.ndarray, out_png: str, slice_idx: Optional[int] = None) -> int:
    """保存 CT + mask 叠加图。"""
    _ensure_dir(os.path.dirname(out_png) or ".")
    vol = _norm01(volume_zyx)
    m = (mask_zyx > 0).astype(np.float32)
    z = int(vol.shape[0] // 2 if slice_idx is None else np.clip(slice_idx, 0, vol.shape[0] - 1))
    img = vol[z]
    ms = m[z]
    plt.figure(figsize=(6, 6))
    plt.imshow(img, cmap="gray")
    plt.imshow(ms, cmap="Reds", alpha=0.35)
    plt.axis("off")
    plt.title(f"Overlay Slice z={z}")
    plt.tight_layout()
    plt.savefig(out_png, dpi=220)
    plt.close()
    return z


def save_roi_center_png(roi_zyx: np.ndarray, out_png: str, pred_class: str, prob: float) -> int:
    """保存 ROI 中心切片，并显示分类结果。"""
    _ensure_dir(os.path.dirname(out_png) or ".")
    roi = _norm01(roi_zyx)
    z = roi.shape[0] // 2
    img = roi[z]
    plt.figure(figsize=(6, 6))
    plt.imshow(img, cmap="gray")
    plt.axis("off")
    plt.title(f"ROI Center z={z}\nPred: {pred_class}, Prob={prob:.4f}")
    plt.tight_layout()
    plt.savefig(out_png, dpi=220)
    plt.close()
    return z
