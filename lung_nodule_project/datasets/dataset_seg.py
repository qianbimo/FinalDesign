"""
Segmentation patch-level dataset and index utilities.
"""

from __future__ import annotations

import os
import random
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

import numpy as np
import torch
from torch.utils.data import Dataset
from tqdm import tqdm


def _read_pair_list(list_path: str) -> List[Tuple[str, str]]:
    items: List[Tuple[str, str]] = []
    with open(list_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) != 2:
                raise ValueError(f"Invalid list row (expect image_path mask_path): {line}")
            items.append((parts[0], parts[1]))
    return items


def _starts(length: int, patch: int, stride: int) -> List[int]:
    if length <= patch:
        return [0]
    out = list(range(0, max(1, length - patch + 1), max(1, stride)))
    last = length - patch
    if out[-1] != last:
        out.append(last)
    return out


def _crop_with_padding(arr_zyx: np.ndarray, z: int, y: int, x: int, patch_zyx: Tuple[int, int, int]) -> np.ndarray:
    pd, ph, pw = [int(v) for v in patch_zyx]
    d, h, w = arr_zyx.shape
    out = np.zeros((pd, ph, pw), dtype=arr_zyx.dtype)

    z2, y2, x2 = z + pd, y + ph, x + pw
    sz1, sy1, sx1 = max(0, z), max(0, y), max(0, x)
    sz2, sy2, sx2 = min(d, z2), min(h, y2), min(w, x2)

    dz1, dy1, dx1 = sz1 - z, sy1 - y, sx1 - x
    dz2, dy2, dx2 = dz1 + (sz2 - sz1), dy1 + (sy2 - sy1), dx1 + (sx2 - sx1)

    if sz1 < sz2 and sy1 < sy2 and sx1 < sx2:
        out[dz1:dz2, dy1:dy2, dx1:dx2] = arr_zyx[sz1:sz2, sy1:sy2, sx1:sx2]
    return out


def _default_meta_path(index_path: str) -> str:
    return str(Path(index_path).with_suffix(Path(index_path).suffix + ".meta.json"))


def build_patch_index_from_pairs(
    pairs: Sequence[Tuple[str, str]],
    index_path: str,
    patch_size_zyx: Tuple[int, int, int] = (96, 96, 32),
    stride_zyx: Tuple[int, int, int] = (48, 48, 16),
    neg_pos_ratio: float = 1.0,
    max_neg_per_case: int = 64,
    seed: int = 42,
    overwrite: bool = False,
    desc: str = "Build patch index",
) -> Dict[str, int]:
    """
    Build patch index file.

    Each line format:
      image_path mask_path z y x is_pos
    """
    from json import dump, load

    index_path = str(index_path)
    meta_path = _default_meta_path(index_path)
    Path(index_path).parent.mkdir(parents=True, exist_ok=True)

    if (not overwrite) and os.path.exists(index_path) and os.path.exists(meta_path):
        with open(meta_path, "r", encoding="utf-8") as f:
            meta = load(f)
        meta["reused"] = 1
        return {k: int(v) if isinstance(v, (int, np.integer)) else v for k, v in meta.items()}

    rng = random.Random(seed)
    num_pos = 0
    num_neg = 0
    num_cases = 0
    num_records = 0

    with open(index_path, "w", encoding="utf-8") as fw:
        for img_path, mask_path in tqdm(pairs, desc=desc, leave=False):
            num_cases += 1
            mask = np.load(mask_path, mmap_mode="r")
            d, h, w = [int(v) for v in mask.shape]
            pd, ph, pw = [int(v) for v in patch_size_zyx]
            sd, sh, sw = [int(v) for v in stride_zyx]

            zs = _starts(d, pd, sd)
            ys = _starts(h, ph, sh)
            xs = _starts(w, pw, sw)

            pos_starts: List[Tuple[int, int, int]] = []
            neg_starts: List[Tuple[int, int, int]] = []

            for z in zs:
                for y in ys:
                    for x in xs:
                        patch = mask[z : z + pd, y : y + ph, x : x + pw]
                        if np.any(patch > 0):
                            pos_starts.append((z, y, x))
                        else:
                            neg_starts.append((z, y, x))

            for z, y, x in pos_starts:
                fw.write(f"{img_path} {mask_path} {z} {y} {x} 1\n")
            num_pos += len(pos_starts)
            num_records += len(pos_starts)

            if len(neg_starts) > 0:
                if len(pos_starts) > 0:
                    keep = max(1, int(round(len(pos_starts) * float(neg_pos_ratio))))
                else:
                    keep = max(1, int(max_neg_per_case))
                if max_neg_per_case > 0:
                    keep = min(keep, int(max_neg_per_case))
                keep = min(keep, len(neg_starts))

                if keep > 0:
                    chosen = rng.sample(neg_starts, keep) if keep < len(neg_starts) else neg_starts
                    for z, y, x in chosen:
                        fw.write(f"{img_path} {mask_path} {z} {y} {x} 0\n")
                    num_neg += len(chosen)
                    num_records += len(chosen)

    meta = {
        "num_cases": int(num_cases),
        "num_records": int(num_records),
        "num_pos": int(num_pos),
        "num_neg": int(num_neg),
        "patch_size_zyx": list(map(int, patch_size_zyx)),
        "stride_zyx": list(map(int, stride_zyx)),
        "neg_pos_ratio": float(neg_pos_ratio),
        "max_neg_per_case": int(max_neg_per_case),
        "seed": int(seed),
        "reused": 0,
    }
    with open(meta_path, "w", encoding="utf-8") as f:
        dump(meta, f, ensure_ascii=False, indent=2)
    return meta


def build_patch_index_from_list(
    list_path: str,
    index_path: str,
    patch_size_zyx: Tuple[int, int, int] = (96, 96, 32),
    stride_zyx: Tuple[int, int, int] = (48, 48, 16),
    neg_pos_ratio: float = 1.0,
    max_neg_per_case: int = 64,
    seed: int = 42,
    overwrite: bool = False,
    desc: str = "Build patch index",
) -> Dict[str, int]:
    pairs = _read_pair_list(list_path)
    return build_patch_index_from_pairs(
        pairs=pairs,
        index_path=index_path,
        patch_size_zyx=patch_size_zyx,
        stride_zyx=stride_zyx,
        neg_pos_ratio=neg_pos_ratio,
        max_neg_per_case=max_neg_per_case,
        seed=seed,
        overwrite=overwrite,
        desc=desc,
    )


def load_patch_index(index_path: str) -> Tuple[List[Tuple[str, str, int, int, int, int]], int, int]:
    records: List[Tuple[str, str, int, int, int, int]] = []
    num_pos, num_neg = 0, 0
    with open(index_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) == 6:
                img_path, mask_path, z, y, x, is_pos = parts
            elif len(parts) == 5:
                # backward compatibility
                img_path, mask_path, z, y, x = parts
                is_pos = "-1"
            else:
                raise ValueError(f"Invalid patch index row: {line}")
            rec = (img_path, mask_path, int(z), int(y), int(x), int(is_pos))
            records.append(rec)
            if rec[5] == 1:
                num_pos += 1
            elif rec[5] == 0:
                num_neg += 1
    return records, num_pos, num_neg


class SegmentationPatchDataset(Dataset):
    """
    Patch-level segmentation dataset.

    Prefer loading prebuilt index for fast startup.
    """

    def __init__(
        self,
        list_path: Optional[str] = None,
        index_path: Optional[str] = None,
        patch_size_zyx: Tuple[int, int, int] = (96, 96, 32),
        stride_zyx: Tuple[int, int, int] = (48, 48, 16),
        neg_pos_ratio: float = 1.0,
        max_neg_per_case: int = 64,
        augment: bool = False,
        seed: int = 42,
        max_cases: int = -1,
        force_rebuild_index: bool = False,
        desc: str = "seg_patch_index",
    ):
        if index_path and os.path.exists(index_path) and (not force_rebuild_index):
            records, num_pos, num_neg = load_patch_index(index_path)
            self.patch_records = records
            self.num_pos = int(num_pos)
            self.num_neg = int(num_neg)
        else:
            if list_path is None:
                raise ValueError("Either list_path or existing index_path must be provided")
            pairs = _read_pair_list(list_path)
            if max_cases > 0:
                pairs = pairs[:max_cases]

            tmp_index_path = index_path if index_path else os.path.join(os.getcwd(), "tmp_patch_index.txt")
            build_patch_index_from_pairs(
                pairs=pairs,
                index_path=tmp_index_path,
                patch_size_zyx=patch_size_zyx,
                stride_zyx=stride_zyx,
                neg_pos_ratio=neg_pos_ratio,
                max_neg_per_case=max_neg_per_case,
                seed=seed,
                overwrite=True,
                desc=desc,
            )
            records, num_pos, num_neg = load_patch_index(tmp_index_path)
            self.patch_records = records
            self.num_pos = int(num_pos)
            self.num_neg = int(num_neg)

        self.patch_size_zyx = tuple(int(v) for v in patch_size_zyx)
        self.augment = augment
        self.rng = random.Random(seed)

    def __len__(self) -> int:
        return len(self.patch_records)

    def _random_flip(self, image: np.ndarray, mask: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        for axis in [0, 1, 2]:
            if random.random() < 0.5:
                image = np.flip(image, axis=axis).copy()
                mask = np.flip(mask, axis=axis).copy()
        return image, mask

    def __getitem__(self, idx: int):
        img_path, mask_path, z, y, x, _ = self.patch_records[idx]
        image = np.load(img_path, mmap_mode="r")
        mask = np.load(mask_path, mmap_mode="r")

        image_patch = _crop_with_padding(image, z, y, x, self.patch_size_zyx).astype(np.float32)
        mask_patch = _crop_with_padding(mask, z, y, x, self.patch_size_zyx).astype(np.float32)
        mask_patch = (mask_patch > 0).astype(np.float32)

        if self.augment:
            image_patch, mask_patch = self._random_flip(image_patch, mask_patch)

        image_t = torch.tensor(np.ascontiguousarray(image_patch[None, ...]), dtype=torch.float32)
        mask_t = torch.tensor(np.ascontiguousarray(mask_patch[None, ...]), dtype=torch.float32)
        return image_t, mask_t


# backward compatibility
SegmentationDataset = SegmentationPatchDataset
