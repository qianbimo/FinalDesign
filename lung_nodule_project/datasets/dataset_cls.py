"""
分类任务 Dataset。
"""

from __future__ import annotations

import random
from typing import List, Tuple

import numpy as np
import torch
from torch.utils.data import Dataset


def _read_cls_list(list_path: str) -> List[Tuple[str, int]]:
    items: List[Tuple[str, int]] = []
    with open(list_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) != 2:
                raise ValueError(f"列表行格式错误（应为 roi_path label）：{line}")
            items.append((parts[0], int(parts[1])))
    return items


class ClassificationDataset(Dataset):
    """
    读取分类 ROI .npy，输出 [1,D,H,W] + label。
    """

    def __init__(self, list_path: str, augment: bool = False):
        self.items = _read_cls_list(list_path)
        self.augment = augment

    def __len__(self) -> int:
        return len(self.items)

    def label_counts(self) -> Tuple[int, int]:
        labels = [y for _, y in self.items]
        neg = int(sum(1 for y in labels if y == 0))
        pos = int(sum(1 for y in labels if y == 1))
        return neg, pos

    def _random_aug(self, roi: np.ndarray) -> np.ndarray:
        # 随机翻转 + 轻微强度扰动。
        for axis in [0, 1, 2]:
            if random.random() < 0.5:
                roi = np.flip(roi, axis=axis).copy()
        if random.random() < 0.5:
            scale = 1.0 + random.uniform(-0.10, 0.10)
            shift = random.uniform(-0.08, 0.08)
            roi = np.clip(roi * scale + shift, 0.0, 1.0)
        return roi

    def __getitem__(self, idx: int):
        roi_path, label = self.items[idx]
        roi = np.load(roi_path).astype(np.float32)  # z,y,x
        if self.augment:
            roi = self._random_aug(roi)
        roi_t = torch.tensor(np.ascontiguousarray(roi[None, ...]), dtype=torch.float32).contiguous()  # [1,D,H,W]
        label_t = torch.tensor(int(label), dtype=torch.long)
        return roi_t, label_t
