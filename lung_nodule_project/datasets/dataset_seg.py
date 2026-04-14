"""
分割任务 Dataset。
"""

from __future__ import annotations

import random
from typing import List, Tuple

import numpy as np
import torch
from torch.utils.data import Dataset


def _read_pair_list(list_path: str) -> List[Tuple[str, str]]:
    items: List[Tuple[str, str]] = []
    with open(list_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) != 2:
                raise ValueError(f"列表行格式错误（应为 image_path mask_path）：{line}")
            items.append((parts[0], parts[1]))
    return items


class SegmentationDataset(Dataset):
    """读取预处理后的 image/mask .npy 对。"""

    def __init__(self, list_path: str, augment: bool = False):
        self.items = _read_pair_list(list_path)
        self.augment = augment

    def __len__(self) -> int:
        return len(self.items)

    def _random_flip(self, image: np.ndarray, mask: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        # 三个维度随机翻转
        for axis in [0, 1, 2]:
            if random.random() < 0.5:
                image = np.flip(image, axis=axis).copy()
                mask = np.flip(mask, axis=axis).copy()
        return image, mask

    def __getitem__(self, idx: int):
        img_path, mask_path = self.items[idx]
        image = np.load(img_path).astype(np.float32)  # z,y,x
        mask = np.load(mask_path).astype(np.float32)  # z,y,x
        mask = (mask > 0).astype(np.float32)

        if self.augment:
            image, mask = self._random_flip(image, mask)

        # 转成 [C,D,H,W]，并确保为 contiguous + 独立 storage
        image = np.ascontiguousarray(image[None, ...])
        mask = np.ascontiguousarray(mask[None, ...])
        image_t = torch.tensor(image, dtype=torch.float32).contiguous()
        mask_t = torch.tensor(mask, dtype=torch.float32).contiguous()
        return image_t, mask_t
