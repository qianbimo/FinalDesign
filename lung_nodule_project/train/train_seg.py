"""
分割训练脚本（LUNA16 -> 3D Res-UNet）。
运行产物统一写入：
workspace/runs/<train_seg_时间戳>/
"""

from __future__ import annotations

import argparse
import os
import sys
import time
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

from datasets.dataset_seg import SegmentationDataset
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
    """Dice + BCE 组合损失。"""

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


def _crop_with_padding(x: torch.Tensor, z1: int, y1: int, x1: int, dz: int, dy: int, dx: int) -> torch.Tensor:
    """从 [B,C,D,H,W] 裁剪 patch，越界补 0。"""
    b, c, d, h, w = x.shape
    out = x.new_zeros((b, c, dz, dy, dx))
    z2, y2, x2 = z1 + dz, y1 + dy, x1 + dx
    sz1, sy1, sx1 = max(0, z1), max(0, y1), max(0, x1)
    sz2, sy2, sx2 = min(d, z2), min(h, y2), min(w, x2)
    dz1, dy1, dx1 = sz1 - z1, sy1 - y1, sx1 - x1
    dz2, dy2, dx2 = dz1 + (sz2 - sz1), dy1 + (sy2 - sy1), dx1 + (sx2 - sx1)
    if sz1 < sz2 and sy1 < sy2 and sx1 < sx2:
        out[:, :, dz1:dz2, dy1:dy2, dx1:dx2] = x[:, :, sz1:sz2, sy1:sy2, sx1:sx2]
    return out


def _sample_patch(
    image: torch.Tensor,
    mask: torch.Tensor,
    patch_zyx: Tuple[int, int, int],
    force_positive_prob: float = 0.7,
) -> Tuple[torch.Tensor, torch.Tensor]:
    """随机采样 patch，优先采样正样本附近。"""
    _, _, d, h, w = image.shape
    pd, ph, pw = patch_zyx
    pos = (mask > 0.5).nonzero(as_tuple=False)
    use_pos = pos.numel() > 0 and np.random.rand() < force_positive_prob
    if use_pos:
        idx = pos[np.random.randint(0, pos.shape[0])]
        z1, y1, x1 = int(idx[2]) - pd // 2, int(idx[3]) - ph // 2, int(idx[4]) - pw // 2
    else:
        z1 = np.random.randint(0, max(1, d - pd + 1)) if d >= pd else (d - pd) // 2
        y1 = np.random.randint(0, max(1, h - ph + 1)) if h >= ph else (h - ph) // 2
        x1 = np.random.randint(0, max(1, w - pw + 1)) if w >= pw else (w - pw) // 2
    return (
        _crop_with_padding(image, z1, y1, x1, pd, ph, pw),
        _crop_with_padding(mask, z1, y1, x1, pd, ph, pw),
    )


def train_seg(config: Dict, override_epochs: int = -1) -> None:
    set_seed(int(config.get("seed", 42)))
    device = _pick_device(str(config.get("device", "auto")).lower())

    data_cfg = config["data"]
    train_cfg = config["train"]
    model_cfg = config["model"]
    loss_cfg = config["loss"]

    train_list = data_cfg["train_list"]
    val_list = data_cfg["val_list"]
    if not os.path.exists(train_list) or not os.path.exists(val_list):
        raise FileNotFoundError("未找到 train/val 列表，请先运行 datasets/preprocess_luna16.py")

    epochs = int(train_cfg["epochs"]) if override_epochs <= 0 else int(override_epochs)
    batch_size = int(train_cfg["batch_size"])
    num_workers = int(train_cfg["num_workers"])
    amp = bool(train_cfg.get("amp", True)) and device.type == "cuda"
    patch_zyx = tuple(config.get("infer", {}).get("patch_size_zyx", [96, 96, 96]))

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

    train_ds = SegmentationDataset(train_list, augment=True)
    val_ds = SegmentationDataset(val_list, augment=False)
    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers, pin_memory=True)
    val_loader = DataLoader(val_ds, batch_size=1, shuffle=False, num_workers=num_workers, pin_memory=True)

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
            image, mask = _sample_patch(image, mask, patch_zyx=patch_zyx, force_positive_prob=0.7)

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
                image, mask = _sample_patch(image, mask, patch_zyx=patch_zyx, force_positive_prob=1.0)
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
    print(f"分割训练完成，best_dice={best_dice:.4f}")
    print(f"Run outputs: {run_dir}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Segmentation (Res-UNet3D)")
    parser.add_argument("--config", type=str, default="configs/seg_config.yaml", help="分割配置文件")
    parser.add_argument("--epochs", type=int, default=-1, help="覆盖配置中的 epoch")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    train_seg(cfg, override_epochs=args.epochs)
