"""
Segmentation training script (LUNA16 -> 3D Res-UNet).
All outputs are saved under workspace/runs/<train_seg_timestamp>/
"""

from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path
from typing import Dict, Tuple

import numpy as np
import torch
import torch.nn as nn
import yaml
from torch.optim import Adam, SGD
from torch.optim.lr_scheduler import CosineAnnealingLR, StepLR
from torch.utils.data import DataLoader
from tqdm import tqdm

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)

from datasets.dataset_seg import SegmentationPatchDataset, build_patch_index_from_list
from models.resunet3d import ResUNet3D
from train.utils import (
    append_csv,
    append_log_line,
    dice_score_from_logits,
    ensure_dir,
    init_csv,
    iou_score_from_logits,
    now_str,
    plot_curves,
    prepare_run_dirs,
    save_checkpoint,
    set_seed,
)


class DiceBCELoss(nn.Module):
    """Dice + BCE combined loss."""

    def __init__(self, dice_weight: float, bce_weight: float, bce_pos_weight: float):
        super().__init__()
        self.dice_weight = float(dice_weight)
        self.bce_weight = float(bce_weight)
        self.register_buffer("pos_w", torch.tensor([float(bce_pos_weight)], dtype=torch.float32))

    def forward(self, logits: torch.Tensor, target: torch.Tensor) -> torch.Tensor:
        bce = nn.functional.binary_cross_entropy_with_logits(logits, target, pos_weight=self.pos_w)
        prob = torch.sigmoid(logits)
        inter = (prob * target).sum(dim=(1, 2, 3, 4))
        den = prob.sum(dim=(1, 2, 3, 4)) + target.sum(dim=(1, 2, 3, 4))
        dice = 1.0 - (2.0 * inter + 1e-6) / (den + 1e-6)
        return self.dice_weight * dice.mean() + self.bce_weight * bce


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _pick_device(device_cfg: str) -> torch.device:
    if device_cfg == "cuda":
        return torch.device("cuda")
    if device_cfg == "cpu":
        return torch.device("cpu")
    return torch.device("cuda" if torch.cuda.is_available() else "cpu")


def _build_optimizer(model: nn.Module, train_cfg: Dict):
    name = str(train_cfg.get("optimizer", "adam")).lower()
    lr = float(train_cfg["learning_rate"])
    wd = float(train_cfg.get("weight_decay", 1e-5))
    if name == "sgd":
        return SGD(model.parameters(), lr=lr, momentum=0.9, weight_decay=wd)
    return Adam(model.parameters(), lr=lr, weight_decay=wd)


def _build_scheduler(optimizer, train_cfg: Dict, epochs: int):
    name = str(train_cfg.get("scheduler", "cosine")).lower()
    if name == "step":
        return StepLR(optimizer, step_size=max(1, epochs // 3), gamma=0.5)
    return CosineAnnealingLR(optimizer, T_max=max(1, epochs))


def _safe_float_tag(v: float) -> str:
    return str(v).replace(".", "p")


def _resolve_patch_index_paths(config: Dict) -> Tuple[str, str, str]:
    data_cfg = config["data"]
    train_cfg = config.get("train", {})

    patch_size = tuple(train_cfg.get("patch_size_zyx", [96, 96, 32]))
    stride = tuple(train_cfg.get("patch_stride_zyx", [48, 48, 16]))
    neg_pos_ratio = float(train_cfg.get("neg_pos_ratio", 1.0))
    max_neg_per_case = int(train_cfg.get("max_neg_per_case", 64))

    index_dir = train_cfg.get("patch_index_dir", os.path.join(data_cfg["processed_root"], "patch_index"))
    Path(index_dir).mkdir(parents=True, exist_ok=True)

    default_name = (
        f"p{patch_size[0]}x{patch_size[1]}x{patch_size[2]}_"
        f"s{stride[0]}x{stride[1]}x{stride[2]}_"
        f"npr{_safe_float_tag(neg_pos_ratio)}_mn{max_neg_per_case}"
    )
    train_index = train_cfg.get("train_patch_index", os.path.join(index_dir, f"train_{default_name}.txt"))
    val_index = train_cfg.get("val_patch_index", os.path.join(index_dir, f"val_{default_name}.txt"))
    return index_dir, train_index, val_index


def train_seg(config: Dict, override_epochs: int = -1, batch_size_override: int = -1) -> None:
    seed = int(config.get("seed", 42))
    set_seed(seed)
    device = _pick_device(str(config.get("device", "auto")).lower())

    data_cfg = config["data"]
    train_cfg = config["train"]
    model_cfg = config["model"]
    loss_cfg = config["loss"]

    train_list = data_cfg["train_list"]
    val_list = data_cfg["val_list"]
    if not os.path.exists(train_list) or not os.path.exists(val_list):
        raise FileNotFoundError("Missing train/val list, run datasets/preprocess_luna16.py first.")

    epochs = int(train_cfg["epochs"]) if override_epochs <= 0 else int(override_epochs)
    batch_size = int(train_cfg["batch_size"]) if batch_size_override <= 0 else int(batch_size_override)
    num_workers = int(train_cfg.get("num_workers", 0))
    pin_memory = bool(train_cfg.get("pin_memory", False))
    amp = bool(train_cfg.get("amp", True)) and device.type == "cuda"

    patch_zyx = tuple(train_cfg.get("patch_size_zyx", [96, 96, 32]))
    stride_zyx = tuple(train_cfg.get("patch_stride_zyx", [48, 48, 16]))
    neg_pos_ratio = float(train_cfg.get("neg_pos_ratio", 1.0))
    max_neg_per_case = int(train_cfg.get("max_neg_per_case", 64))

    run_info = prepare_run_dirs("train_seg", config, argv=sys.argv)
    run_dir = run_info["run_dir"]
    run_logs_dir = run_info["logs_dir"]
    run_fig_dir = run_info["figures_dir"]
    run_ckpt_dir = run_info["ckpt_dir"]

    global_weight_dir = str(train_cfg.get("save_dir", "./workspace/weights"))
    ensure_dir(global_weight_dir)

    log_path = os.path.join(run_logs_dir, "train_seg.log")
    csv_path = os.path.join(run_logs_dir, "train_seg_metrics.csv")
    fields = ["epoch", "lr", "train_loss", "val_loss", "dice", "iou", "epoch_sec"]
    init_csv(csv_path, fields)
    append_log_line(log_path, f"[{now_str()}] Start seg training, device={device}, epochs={epochs}, run_dir={run_dir}")

    # Try to reuse prebuilt patch index cache
    index_dir, train_patch_index, val_patch_index = _resolve_patch_index_paths(config)
    append_log_line(log_path, f"[{now_str()}] patch_index_dir={index_dir}")

    if not os.path.exists(train_patch_index):
        append_log_line(log_path, f"[{now_str()}] train patch index missing, building once...")
        build_patch_index_from_list(
            list_path=train_list,
            index_path=train_patch_index,
            patch_size_zyx=patch_zyx,
            stride_zyx=stride_zyx,
            neg_pos_ratio=neg_pos_ratio,
            max_neg_per_case=max_neg_per_case,
            seed=seed,
            overwrite=True,
            desc="Build train patch index (on-demand)",
        )
    if not os.path.exists(val_patch_index):
        append_log_line(log_path, f"[{now_str()}] val patch index missing, building once...")
        build_patch_index_from_list(
            list_path=val_list,
            index_path=val_patch_index,
            patch_size_zyx=patch_zyx,
            stride_zyx=stride_zyx,
            neg_pos_ratio=1.0,
            max_neg_per_case=max_neg_per_case,
            seed=seed + 1,
            overwrite=True,
            desc="Build val patch index (on-demand)",
        )

    train_ds = SegmentationPatchDataset(index_path=train_patch_index, patch_size_zyx=patch_zyx, augment=True, seed=seed)
    val_ds = SegmentationPatchDataset(index_path=val_patch_index, patch_size_zyx=patch_zyx, augment=False, seed=seed + 1)

    append_log_line(
        log_path,
        f"[{now_str()}] Using index cache: train={train_patch_index}, val={val_patch_index}; "
        f"train_samples={len(train_ds)} (pos={train_ds.num_pos}, neg={train_ds.num_neg}), "
        f"val_samples={len(val_ds)} (pos={val_ds.num_pos}, neg={val_ds.num_neg})",
    )
    print(
        f"[Seg] IndexCache train={len(train_ds)} (pos={train_ds.num_pos}, neg={train_ds.num_neg}), "
        f"val={len(val_ds)} (pos={val_ds.num_pos}, neg={val_ds.num_neg})"
    )

    train_loader = DataLoader(
        train_ds,
        batch_size=batch_size,
        shuffle=True,
        num_workers=num_workers,
        pin_memory=pin_memory,
        drop_last=False,
    )
    val_loader = DataLoader(
        val_ds,
        batch_size=max(1, min(batch_size, 4)),
        shuffle=False,
        num_workers=num_workers,
        pin_memory=pin_memory,
        drop_last=False,
    )

    model = ResUNet3D(
        in_channels=int(model_cfg["in_channels"]),
        out_channels=int(model_cfg["out_channels"]),
        base_channels=int(model_cfg["base_channels"]),
    ).to(device)
    criterion = DiceBCELoss(
        dice_weight=float(loss_cfg["dice_weight"]),
        bce_weight=float(loss_cfg["bce_weight"]),
        bce_pos_weight=float(loss_cfg.get("bce_pos_weight", 1.0)),
    ).to(device)
    optimizer = _build_optimizer(model, train_cfg)
    scheduler = _build_scheduler(optimizer, train_cfg, epochs)
    scaler = torch.cuda.amp.GradScaler(enabled=amp)
    grad_clip = float(train_cfg.get("grad_clip", 0.0))

    best_dice = -1.0
    history = {"train_loss": [], "val_loss": [], "dice": [], "iou": []}

    for epoch in range(1, epochs + 1):
        t0 = time.time()
        model.train()
        train_losses = []
        for image, mask in tqdm(train_loader, desc=f"[Seg][Epoch {epoch}/{epochs}] Train", leave=False):
            image = image.to(device, non_blocking=True)
            mask = mask.to(device, non_blocking=True)

            optimizer.zero_grad(set_to_none=True)
            with torch.cuda.amp.autocast(enabled=amp):
                logits = model(image)
                loss = criterion(logits, mask)
            scaler.scale(loss).backward()
            if grad_clip > 0:
                scaler.unscale_(optimizer)
                torch.nn.utils.clip_grad_norm_(model.parameters(), grad_clip)
            scaler.step(optimizer)
            scaler.update()
            train_losses.append(float(loss.item()))

        model.eval()
        val_losses, dices, ious = [], [], []
        with torch.no_grad():
            for image, mask in tqdm(val_loader, desc=f"[Seg][Epoch {epoch}/{epochs}] Val", leave=False):
                image = image.to(device, non_blocking=True)
                mask = mask.to(device, non_blocking=True)
                logits = model(image)
                loss = criterion(logits, mask)
                val_losses.append(float(loss.item()))
                dices.append(dice_score_from_logits(logits, mask))
                ious.append(iou_score_from_logits(logits, mask))

        scheduler.step()
        train_loss = float(np.mean(train_losses)) if train_losses else 0.0
        val_loss = float(np.mean(val_losses)) if val_losses else 0.0
        dice = float(np.mean(dices)) if dices else 0.0
        iou = float(np.mean(ious)) if ious else 0.0
        lr = float(optimizer.param_groups[0]["lr"])
        epoch_sec = time.time() - t0

        history["train_loss"].append(train_loss)
        history["val_loss"].append(val_loss)
        history["dice"].append(dice)
        history["iou"].append(iou)

        msg = (
            f"[Seg][Epoch {epoch}/{epochs}] "
            f"train_loss={train_loss:.4f} val_loss={val_loss:.4f} dice={dice:.4f} iou={iou:.4f}"
        )
        print(msg)
        append_log_line(log_path, f"[{now_str()}] {msg}")
        append_csv(
            csv_path,
            {
                "epoch": epoch,
                "lr": f"{lr:.8f}",
                "train_loss": f"{train_loss:.6f}",
                "val_loss": f"{val_loss:.6f}",
                "dice": f"{dice:.6f}",
                "iou": f"{iou:.6f}",
                "epoch_sec": f"{epoch_sec:.2f}",
            },
            fields,
        )

        best_payload = {"model": model.state_dict(), "epoch": epoch, "best_dice": best_dice, "config": config}
        if dice > best_dice:
            best_dice = dice
            best_payload["best_dice"] = best_dice
            save_checkpoint(best_payload, os.path.join(run_ckpt_dir, "seg_best.pth"))
            save_checkpoint(best_payload, os.path.join(global_weight_dir, "seg_best.pth"))

        last_payload = {"model": model.state_dict(), "epoch": epoch, "best_dice": best_dice, "config": config}
        save_checkpoint(last_payload, os.path.join(run_ckpt_dir, "seg_last.pth"))
        save_checkpoint(last_payload, os.path.join(global_weight_dir, "seg_last.pth"))

    plot_curves(
        {"train_loss": history["train_loss"], "val_loss": history["val_loss"]},
        os.path.join(run_fig_dir, "seg_loss_curve.png"),
        title="Segmentation Loss Curve",
        ylabel="Loss",
    )
    plot_curves(
        {"dice": history["dice"]},
        os.path.join(run_fig_dir, "seg_dice_curve.png"),
        title="Segmentation Dice Curve",
        ylabel="Dice",
    )
    plot_curves(
        {"iou": history["iou"]},
        os.path.join(run_fig_dir, "seg_iou_curve.png"),
        title="Segmentation IoU Curve",
        ylabel="IoU",
    )

    append_log_line(log_path, f"[{now_str()}] Finished seg training, best_dice={best_dice:.6f}, run_dir={run_dir}")
    print(f"Seg training done, best_dice={best_dice:.4f}")
    print(f"Run outputs: {run_dir}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Segmentation (Res-UNet3D)")
    parser.add_argument("--config", type=str, default="configs/seg_config.yaml", help="seg config")
    parser.add_argument("--epochs", type=int, default=-1, help="override epoch")
    parser.add_argument("--batch-size", type=int, default=-1, help="override batch size")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    train_seg(cfg, override_epochs=args.epochs, batch_size_override=args.batch_size)
