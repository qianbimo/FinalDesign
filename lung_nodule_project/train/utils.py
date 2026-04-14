"""
训练通用工具函数。
"""

from __future__ import annotations

import csv
import os
import random
from datetime import datetime
from typing import Dict, Iterable, List, Optional

import matplotlib.pyplot as plt
import numpy as np
import torch
from sklearn.metrics import accuracy_score, f1_score, precision_score, recall_score, roc_auc_score


def set_seed(seed: int) -> None:
    """固定随机种子，保证可复现。"""
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False


def now_str() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def prepare_run_dirs(task_name: str, config: Optional[Dict] = None, argv: Optional[List[str]] = None) -> Dict[str, str]:
    """
    统一准备本次运行目录。

    规则：
    1) 若设置环境变量 LUNG_RUN_DIR，则把结果落在该根目录下。
    2) 若同时设置 LUNG_RUN_STEP，则使用 <LUNG_RUN_DIR>/<LUNG_RUN_STEP>。
    3) 否则使用 workspace/runs/<task_name>_<timestamp>。
    """
    run_root_env = os.environ.get("LUNG_RUN_DIR", "").strip()
    run_step_env = os.environ.get("LUNG_RUN_STEP", "").strip()

    if run_root_env:
        run_root = run_root_env
        run_name = run_step_env if run_step_env else task_name
        run_dir = os.path.join(run_root, run_name)
    else:
        run_root = os.path.join(".", "workspace", "runs")
        run_dir = os.path.join(run_root, f"{task_name}_{now_tag()}")

    logs_dir = os.path.join(run_dir, "logs")
    figures_dir = os.path.join(run_dir, "figures")
    ckpt_dir = os.path.join(run_dir, "checkpoints")
    for p in (run_root, run_dir, logs_dir, figures_dir, ckpt_dir):
        ensure_dir(p)

    cmd_txt = os.path.join(run_dir, "command.txt")
    if argv is not None:
        with open(cmd_txt, "w", encoding="utf-8") as f:
            f.write(" ".join(argv) + "\n")

    if config is not None:
        try:
            import yaml

            with open(os.path.join(run_dir, "config_snapshot.yaml"), "w", encoding="utf-8") as f:
                yaml.safe_dump(config, f, allow_unicode=True, sort_keys=False)
        except Exception:
            pass

    return {
        "run_root": run_root,
        "run_dir": run_dir,
        "logs_dir": logs_dir,
        "figures_dir": figures_dir,
        "ckpt_dir": ckpt_dir,
    }


def dice_score_from_logits(logits: torch.Tensor, target: torch.Tensor, eps: float = 1e-6) -> float:
    prob = torch.sigmoid(logits)
    pred = (prob > 0.5).float()
    inter = (pred * target).sum(dim=(1, 2, 3, 4))
    union = pred.sum(dim=(1, 2, 3, 4)) + target.sum(dim=(1, 2, 3, 4))
    dice = (2.0 * inter + eps) / (union + eps)
    return float(dice.mean().item())


def iou_score_from_logits(logits: torch.Tensor, target: torch.Tensor, eps: float = 1e-6) -> float:
    prob = torch.sigmoid(logits)
    pred = (prob > 0.5).float()
    inter = (pred * target).sum(dim=(1, 2, 3, 4))
    union = pred.sum(dim=(1, 2, 3, 4)) + target.sum(dim=(1, 2, 3, 4)) - inter
    iou = (inter + eps) / (union + eps)
    return float(iou.mean().item())


def classification_metrics(y_true: np.ndarray, y_prob: np.ndarray, threshold: float = 0.5) -> Dict[str, float]:
    y_true = y_true.astype(np.int64)
    y_prob = y_prob.astype(np.float32)
    y_pred = (y_prob >= threshold).astype(np.int64)
    out = {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
        "f1": float(f1_score(y_true, y_pred, zero_division=0)),
    }
    try:
        out["auc"] = float(roc_auc_score(y_true, y_prob))
    except Exception:
        out["auc"] = 0.0
    return out


def save_checkpoint(payload: Dict, ckpt_path: str) -> None:
    ensure_dir(os.path.dirname(ckpt_path) or ".")
    torch.save(payload, ckpt_path)


def append_log_line(log_path: str, text: str) -> None:
    ensure_dir(os.path.dirname(log_path) or ".")
    with open(log_path, "a", encoding="utf-8") as f:
        f.write(text.rstrip() + "\n")


def init_csv(csv_path: str, fieldnames: List[str]) -> None:
    ensure_dir(os.path.dirname(csv_path) or ".")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()


def append_csv(csv_path: str, row: Dict[str, object], fieldnames: List[str]) -> None:
    with open(csv_path, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writerow(row)


def plot_curves(curves: Dict[str, Iterable[float]], save_path: str, title: str, ylabel: str = "Value") -> None:
    ensure_dir(os.path.dirname(save_path) or ".")
    plt.figure(figsize=(8, 5))
    for name, vals in curves.items():
        plt.plot(list(vals), label=name)
    plt.title(title)
    plt.xlabel("Epoch")
    plt.ylabel(ylabel)
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(save_path, dpi=200)
    plt.close()
