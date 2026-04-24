"""
Randomly pick 3 samples from segmentation dataset and visualize:
Images | Label | Prediction
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import random
import sys
from datetime import datetime
from typing import Dict, List, Tuple

import matplotlib.pyplot as plt
import numpy as np
import torch
import yaml
from scipy.ndimage import label as cc_label

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)

from infer.infer_seg import load_seg_model, run_sliding_window_inference


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "validate_seg_random3") -> str:
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


def _read_pair_list(list_path: str) -> List[Tuple[str, str]]:
    pairs: List[Tuple[str, str]] = []
    with open(list_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) != 2:
                raise ValueError(f"Invalid list row (expect image_path mask_path): {line}")
            pairs.append((parts[0], parts[1]))
    return pairs


def _filter_positive_pairs(pairs: List[Tuple[str, str]]) -> List[Tuple[str, str]]:
    """Keep only samples whose label mask contains nodule voxels."""
    out: List[Tuple[str, str]] = []
    for img_path, mask_path in pairs:
        try:
            m = np.load(mask_path, mmap_mode="r")
            if int((m > 0).sum()) > 0:
                out.append((img_path, mask_path))
        except Exception:
            # Skip unreadable samples in strict positive-only mode.
            continue
    return out


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


def _center_slice_from_mask(mask_zyx: np.ndarray) -> int | None:
    """
    Return z index of nodule center slice.
    Strategy: largest connected component centroid along z.
    """
    m = (mask_zyx > 0).astype(np.uint8)
    if int(m.sum()) <= 0:
        return None

    cc, n = cc_label(m)
    if int(n) <= 0:
        pts = np.argwhere(m > 0)
        if pts.size == 0:
            return None
        return int(round(float(pts[:, 0].mean())))

    counts = np.bincount(cc.reshape(-1))
    if len(counts) <= 1:
        pts = np.argwhere(m > 0)
        if pts.size == 0:
            return None
        return int(round(float(pts[:, 0].mean())))

    counts[0] = 0
    best_id = int(np.argmax(counts))
    pts = np.argwhere(cc == best_id)
    if pts.size == 0:
        return None
    return int(round(float(pts[:, 0].mean())))


def _dice_iou(pred: np.ndarray, target: np.ndarray, eps: float = 1e-6) -> Tuple[float, float]:
    p = (pred > 0).astype(np.uint8)
    t = (target > 0).astype(np.uint8)
    inter = float((p & t).sum())
    p_sum = float(p.sum())
    t_sum = float(t.sum())
    union = float((p | t).sum())
    dice = (2.0 * inter + eps) / (p_sum + t_sum + eps)
    iou = (inter + eps) / (union + eps)
    return float(dice), float(iou)


def _mask_rgb(mask_2d: np.ndarray) -> np.ndarray:
    m = (mask_2d > 0).astype(np.float32)
    rgb = np.zeros((m.shape[0], m.shape[1], 3), dtype=np.float32)
    rgb[..., 1] = m  # green
    rgb[..., 0] = 0.35 * m  # a little red for visibility
    return rgb


def validate_random_samples(
    config: Dict,
    ckpt_path: str,
    list_path: str,
    num_samples: int,
    seed: int,
    out_root: str,
    require_positive: bool = True,
    bg_mode: str = "white",
) -> Dict:
    _ensure_dir(out_root)
    fig_dir = os.path.join(out_root, "figures")
    pred_dir = os.path.join(out_root, "predictions")
    _ensure_dir(fig_dir)
    _ensure_dir(pred_dir)

    pairs = _read_pair_list(list_path)
    if require_positive:
        pairs = _filter_positive_pairs(pairs)
    if len(pairs) == 0:
        if require_positive:
            raise ValueError(f"No positive (nodule) samples found in list: {list_path}")
        raise ValueError(f"No samples in list: {list_path}")
    k = max(1, min(int(num_samples), len(pairs)))
    rng = random.Random(int(seed))
    sampled = rng.sample(pairs, k=k)

    device = _pick_device(str(config.get("device", "auto")).lower())
    model = load_seg_model(config, ckpt_path=ckpt_path, device=device)
    patch_zyx = tuple(config.get("infer", {}).get("patch_size_zyx", [96, 96, 96]))
    stride_zyx = tuple(config.get("infer", {}).get("stride_zyx", [64, 64, 64]))

    rows = []
    vis_items = []
    for idx, (img_path, mask_path) in enumerate(sampled, start=1):
        volume = np.load(img_path).astype(np.float32)
        label = (np.load(mask_path) > 0).astype(np.uint8)
        pred = run_sliding_window_inference(model, volume, patch_zyx, stride_zyx, device)

        case_name = os.path.splitext(os.path.basename(img_path))[0]
        pred_path = os.path.join(pred_dir, f"{idx:02d}_{case_name}_pred.npy")
        np.save(pred_path, pred.astype(np.uint8))

        dice, iou = _dice_iou(pred, label)
        z = _center_slice_from_mask(label)
        if z is None:
            z = _center_slice_from_mask(pred)
        if z is None:
            z = int(volume.shape[0] // 2)

        rows.append(
            {
                "sample_idx": idx,
                "case_name": case_name,
                "image_path": img_path,
                "label_path": mask_path,
                "pred_path": pred_path,
                "slice_z": z,
                "dice_3d": f"{dice:.6f}",
                "iou_3d": f"{iou:.6f}",
            }
        )
        vis_items.append((case_name, volume[z], label[z], pred[z], dice, iou))

    # Save metrics table
    csv_path = os.path.join(out_root, "metrics_random_samples.csv")
    fields = ["sample_idx", "case_name", "image_path", "label_path", "pred_path", "slice_z", "dice_3d", "iou_3d"]
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)

    # Render a 3-column figure: Images | Label | Prediction
    n = len(vis_items)
    fig, axes = plt.subplots(nrows=n, ncols=3, figsize=(10, 3.4 * n), dpi=180)
    if n == 1:
        axes = np.expand_dims(axes, axis=0)

    bg_mode = str(bg_mode).lower().strip()
    if bg_mode not in {"white", "transparent"}:
        bg_mode = "white"
    if bg_mode == "transparent":
        fig.patch.set_alpha(0.0)
    else:
        fig.patch.set_facecolor("white")
    col_titles = ["Images", "Label", "Prediction"]
    for c in range(3):
        axes[0, c].set_title(col_titles[c], fontsize=14, fontweight="bold")

    for r, (case_name, img2d, lab2d, pred2d, dice, iou) in enumerate(vis_items):
        # image
        axes[r, 0].imshow(_norm01(img2d), cmap="gray")
        axes[r, 0].set_xticks([])
        axes[r, 0].set_yticks([])
        axes[r, 0].set_facecolor("black")
        # label
        axes[r, 1].imshow(_mask_rgb(lab2d))
        axes[r, 1].set_xticks([])
        axes[r, 1].set_yticks([])
        axes[r, 1].set_facecolor("black")

        # prediction
        axes[r, 2].imshow(_mask_rgb(pred2d))
        axes[r, 2].set_xticks([])
        axes[r, 2].set_yticks([])
        axes[r, 2].set_facecolor("black")

    plt.tight_layout(pad=1.2)
    fig_path = os.path.join(fig_dir, "seg_random3_compare.png")
    plt.savefig(
        fig_path,
        bbox_inches="tight",
        transparent=(bg_mode == "transparent"),
        facecolor=("white" if bg_mode == "white" else None),
    )
    plt.close(fig)

    # Save summary json
    dice_all = [float(r["dice_3d"]) for r in rows]
    iou_all = [float(r["iou_3d"]) for r in rows]
    result = {
        "config": {
            "ckpt_path": ckpt_path,
            "list_path": list_path,
            "num_samples": n,
            "seed": seed,
            "require_positive": bool(require_positive),
            "bg_mode": bg_mode,
            "patch_zyx": list(patch_zyx),
            "stride_zyx": list(stride_zyx),
            "device": str(device),
        },
        "outputs": {"figure": fig_path, "metrics_csv": csv_path},
        "summary": {
            "mean_dice_3d": float(np.mean(dice_all)),
            "mean_iou_3d": float(np.mean(iou_all)),
        },
        "samples": rows,
    }
    json_path = os.path.join(out_root, "summary.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    return result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Random 3-sample segmentation validation and visualization")
    parser.add_argument("--config", type=str, default="configs/seg_config.yaml", help="seg config path")
    parser.add_argument("--ckpt", type=str, default="workspace/weights/seg_best.pth", help="seg checkpoint path")
    parser.add_argument("--list-path", type=str, default="", help="pair list path; default uses config.data.val_list")
    parser.add_argument("--num-samples", type=int, default=3, help="number of random samples")
    parser.add_argument("--seed", type=int, default=42, help="random seed")
    parser.add_argument("--out-root", type=str, default="", help="output root directory")
    parser.add_argument(
        "--require-positive",
        type=int,
        default=1,
        help="1: only sample cases with positive label mask; 0: sample from all",
    )
    parser.add_argument(
        "--bg",
        type=str,
        default="white",
        choices=["white", "transparent"],
        help="figure background mode",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    list_path = args.list_path.strip() if args.list_path else str(cfg["data"]["val_list"])
    out_root = args.out_root.strip() if args.out_root else _resolve_run_dir("validate_seg_random3")

    res = validate_random_samples(
        config=cfg,
        ckpt_path=args.ckpt,
        list_path=list_path,
        num_samples=args.num_samples,
        seed=args.seed,
        out_root=out_root,
        require_positive=bool(int(args.require_positive)),
        bg_mode=args.bg,
    )
    print(json.dumps(res, ensure_ascii=False, indent=2))
