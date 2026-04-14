"""
分类训练脚本（LIDC-IDRI_1176 -> 良恶性）。
支持模型切换：mamba / cnn / cnn_transformer。
运行产物统一写入：
workspace/runs/<train_cls_模型名_时间戳>/
"""

from __future__ import annotations

import argparse
import os
import sys
import time
from typing import Dict

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

from datasets.dataset_cls import ClassificationDataset
from models.cnn_transformer_baseline import CNNOnlyClassifier, CNNTransformerClassifier
from models.mamba_classifier import MambaClassifier
from train.utils import (
    append_csv,
    append_log_line,
    classification_metrics,
    ensure_dir,
    init_csv,
    now_str,
    plot_curves,
    prepare_run_dirs,
    save_checkpoint,
    set_seed,
)


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
    wd = float(train_cfg.get("weight_decay", 1e-4))
    if name == "sgd":
        return SGD(model.parameters(), lr=lr, momentum=0.9, weight_decay=wd)
    return Adam(model.parameters(), lr=lr, weight_decay=wd)


def _build_scheduler(optimizer, train_cfg: Dict, epochs: int):
    name = str(train_cfg.get("scheduler", "cosine")).lower()
    if name == "step":
        return StepLR(optimizer, step_size=max(1, epochs // 3), gamma=0.5)
    return CosineAnnealingLR(optimizer, T_max=max(1, epochs))


def _build_model(model_type: str, cfg: Dict, num_classes: int) -> nn.Module:
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


def train_cls(config: Dict, model_override: str = "", epochs_override: int = -1, batch_size_override: int = -1) -> None:
    set_seed(int(config.get("seed", 42)))
    device = _pick_device(str(config.get("device", "auto")).lower())

    data_cfg = config["data"]
    train_cfg = config["train"]

    train_list = data_cfg["train_list"]
    val_list = data_cfg["val_list"]
    if not os.path.exists(train_list) or not os.path.exists(val_list):
        raise FileNotFoundError("未找到 train/val 列表，请先运行 datasets/preprocess_lidc.py")

    model_type = model_override.lower() if model_override else str(train_cfg.get("model_type", "mamba")).lower()
    loss_type = str(train_cfg.get("loss_type", "ce")).lower()
    if loss_type not in {"ce", "bce"}:
        raise ValueError("loss_type 仅支持 ce 或 bce")

    epochs = int(train_cfg["epochs"]) if epochs_override <= 0 else int(epochs_override)
    batch_size = int(train_cfg["batch_size"]) if batch_size_override <= 0 else int(batch_size_override)
    num_workers = int(train_cfg["num_workers"])
    amp = bool(train_cfg.get("amp", True)) and device.type == "cuda"
    grad_clip = float(train_cfg.get("grad_clip", 0.0))

    run_info = prepare_run_dirs(f"train_cls_{model_type}", config, argv=sys.argv)
    run_dir = run_info["run_dir"]
    run_logs_dir = run_info["logs_dir"]
    run_fig_dir = run_info["figures_dir"]
    run_ckpt_dir = run_info["ckpt_dir"]

    global_weight_dir = str(train_cfg.get("save_dir", "./workspace/weights"))
    ensure_dir(global_weight_dir)

    log_path = os.path.join(run_logs_dir, "train_cls.log")
    csv_path = os.path.join(run_logs_dir, "train_cls_metrics.csv")
    fields = ["epoch", "lr", "train_loss", "val_loss", "acc", "precision", "recall", "f1", "auc", "epoch_sec"]
    init_csv(csv_path, fields)
    append_log_line(log_path, f"[{now_str()}] Start cls training, device={device}, epochs={epochs}, model={model_type}, run_dir={run_dir}")

    train_ds = ClassificationDataset(train_list, augment=True)
    val_ds = ClassificationDataset(val_list, augment=False)
    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers, pin_memory=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers, pin_memory=True)

    neg_cnt, pos_cnt = train_ds.label_counts()
    pos_cnt = max(1, pos_cnt)
    neg_cnt = max(1, neg_cnt)
    imbalance_ratio = neg_cnt / pos_cnt

    num_classes = 1 if loss_type == "bce" else int(config["model"].get("num_classes", 2))
    model = _build_model(model_type, config, num_classes=num_classes).to(device)
    optimizer = _build_optimizer(model, train_cfg)
    scheduler = _build_scheduler(optimizer, train_cfg, epochs)
    scaler = torch.cuda.amp.GradScaler(enabled=amp)

    use_class_weight = bool(train_cfg.get("use_class_weight", True))
    if loss_type == "ce":
        if use_class_weight:
            weights = torch.tensor([1.0, imbalance_ratio], dtype=torch.float32, device=device)
            criterion = nn.CrossEntropyLoss(weight=weights)
        else:
            criterion = nn.CrossEntropyLoss()
    else:
        if use_class_weight:
            pos_weight = torch.tensor([imbalance_ratio], dtype=torch.float32, device=device)
            criterion = nn.BCEWithLogitsLoss(pos_weight=pos_weight)
        else:
            criterion = nn.BCEWithLogitsLoss()

    best_auc = -1.0
    history = {"train_loss": [], "val_loss": [], "acc": [], "auc": [], "f1": []}

    for epoch in range(1, epochs + 1):
        t0 = time.time()
        model.train()
        train_losses = []
        for roi, label in tqdm(train_loader, desc=f"[Cls][Epoch {epoch}/{epochs}] Train", leave=False):
            roi = roi.to(device, non_blocking=True)
            label = label.to(device, non_blocking=True)

            optimizer.zero_grad(set_to_none=True)
            with torch.cuda.amp.autocast(enabled=amp):
                logits = model(roi)
                if loss_type == "ce":
                    loss = criterion(logits, label)
                else:
                    logits_1 = logits.squeeze(1) if logits.ndim == 2 else logits
                    loss = criterion(logits_1, label.float())

            scaler.scale(loss).backward()
            if grad_clip > 0:
                scaler.unscale_(optimizer)
                torch.nn.utils.clip_grad_norm_(model.parameters(), grad_clip)
            scaler.step(optimizer)
            scaler.update()
            train_losses.append(float(loss.item()))

        model.eval()
        val_losses = []
        y_true, y_prob = [], []
        with torch.no_grad():
            for roi, label in tqdm(val_loader, desc=f"[Cls][Epoch {epoch}/{epochs}] Val", leave=False):
                roi = roi.to(device, non_blocking=True)
                label = label.to(device, non_blocking=True)
                logits = model(roi)
                if loss_type == "ce":
                    loss = criterion(logits, label)
                    prob = torch.softmax(logits, dim=1)[:, 1]
                else:
                    logits_1 = logits.squeeze(1) if logits.ndim == 2 else logits
                    loss = criterion(logits_1, label.float())
                    prob = torch.sigmoid(logits_1)
                val_losses.append(float(loss.item()))
                y_true.extend(label.cpu().numpy().tolist())
                y_prob.extend(prob.cpu().numpy().tolist())

        scheduler.step()
        train_loss = float(np.mean(train_losses)) if train_losses else 0.0
        val_loss = float(np.mean(val_losses)) if val_losses else 0.0
        m = classification_metrics(np.asarray(y_true, dtype=np.int64), np.asarray(y_prob, dtype=np.float32))
        lr = float(optimizer.param_groups[0]["lr"])
        epoch_sec = time.time() - t0

        history["train_loss"].append(train_loss)
        history["val_loss"].append(val_loss)
        history["acc"].append(m["accuracy"])
        history["auc"].append(m["auc"])
        history["f1"].append(m["f1"])

        msg = (
            f"[Cls][Epoch {epoch}/{epochs}] train_loss={train_loss:.4f} val_loss={val_loss:.4f} "
            f"acc={m['accuracy']:.4f} precision={m['precision']:.4f} recall={m['recall']:.4f} "
            f"f1={m['f1']:.4f} auc={m['auc']:.4f}"
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
                "acc": f"{m['accuracy']:.6f}",
                "precision": f"{m['precision']:.6f}",
                "recall": f"{m['recall']:.6f}",
                "f1": f"{m['f1']:.6f}",
                "auc": f"{m['auc']:.6f}",
                "epoch_sec": f"{epoch_sec:.2f}",
            },
            fields,
        )

        best_payload = {"model": model.state_dict(), "epoch": epoch, "best_auc": best_auc, "config": config, "model_type": model_type}
        if m["auc"] > best_auc:
            best_auc = m["auc"]
            best_payload["best_auc"] = best_auc
            save_checkpoint(best_payload, os.path.join(run_ckpt_dir, f"cls_best_{model_type}.pth"))
            save_checkpoint(best_payload, os.path.join(global_weight_dir, f"cls_best_{model_type}.pth"))

        last_payload = {"model": model.state_dict(), "epoch": epoch, "best_auc": best_auc, "config": config, "model_type": model_type}
        save_checkpoint(last_payload, os.path.join(run_ckpt_dir, f"cls_last_{model_type}.pth"))
        save_checkpoint(last_payload, os.path.join(global_weight_dir, f"cls_last_{model_type}.pth"))

    plot_curves(
        {"train_loss": history["train_loss"], "val_loss": history["val_loss"]},
        os.path.join(run_fig_dir, f"cls_loss_curve_{model_type}.png"),
        title=f"Classification Loss ({model_type})",
        ylabel="Loss",
    )
    plot_curves(
        {"acc": history["acc"]},
        os.path.join(run_fig_dir, f"cls_acc_curve_{model_type}.png"),
        title=f"Classification Accuracy ({model_type})",
        ylabel="Accuracy",
    )
    plot_curves(
        {"auc": history["auc"]},
        os.path.join(run_fig_dir, f"cls_auc_curve_{model_type}.png"),
        title=f"Classification AUC ({model_type})",
        ylabel="AUC",
    )
    append_log_line(log_path, f"[{now_str()}] Finished cls training, best_auc={best_auc:.6f}, model={model_type}, run_dir={run_dir}")
    print(f"分类训练完成，model={model_type}, best_auc={best_auc:.4f}")
    print(f"Run outputs: {run_dir}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Classification")
    parser.add_argument("--config", type=str, default="configs/cls_config.yaml", help="分类配置文件")
    parser.add_argument("--model", type=str, default="", help="覆盖模型类型：mamba/cnn/cnn_transformer")
    parser.add_argument("--epochs", type=int, default=-1, help="覆盖配置中的 epoch")
    parser.add_argument("--batch-size", type=int, default=-1, help="override batch size")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    train_cls(cfg, model_override=args.model, epochs_override=args.epochs, batch_size_override=args.batch_size)
