"""
LIDC-IDRI_1176 预处理脚本（基于 .npy + .zip）。

功能:
1. 读取 LIDC-IDRI_1176.npy 元数据
2. malignancy 标签映射: 1~2->0, 4~5->1, 3丢弃
3. 从 zip 解压后的 nii.gz 读取 patch
4. 支持 center / diameter_bbox 两种裁剪
5. 保存为 .npy，并输出 train/val/test 列表(8/2/0)
6. 运行日志写入 workspace/runs/<...>/logs
"""

from __future__ import annotations

import argparse
import json
import os
import random
import zipfile
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

import numpy as np
import SimpleITK as sitk
import yaml
from scipy.ndimage import zoom
from sklearn.model_selection import train_test_split
from tqdm import tqdm


def _ensure_dir(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "preprocess_lidc") -> str:
    run_root_env = os.environ.get("LUNG_RUN_DIR", "").strip()
    run_step_env = os.environ.get("LUNG_RUN_STEP", "").strip()
    if run_root_env:
        run_name = run_step_env if run_step_env else default_task
        run_dir = os.path.join(run_root_env, run_name)
    else:
        run_dir = os.path.join(".", "workspace", "runs", f"{default_task}_{_now_tag()}")
    _ensure_dir(run_dir)
    _ensure_dir(os.path.join(run_dir, "logs"))
    return run_dir


def _append_run_log(run_log_path: str, text: str) -> None:
    with open(run_log_path, "a", encoding="utf-8") as f:
        f.write(text.rstrip() + "\n")


def _maybe_convert_mnt_path(path: str) -> str:
    """兼容 Windows 路径和 /mnt/<drive>/ 路径。"""
    if os.path.exists(path):
        return path
    p = path.replace("\\", "/")
    if p.startswith("/mnt/") and len(p) > 6:
        drive = p[5].upper()
        rest = p[6:].lstrip("/")
        rest_win = rest.replace("/", "\\")
        win = f"{drive}:\\{rest_win}"
        if os.path.exists(win):
            return win
    return path


def _ensure_extracted(zip_path: str, extracted_root: str, do_extract: bool) -> str:
    """确保 zip 已解压到 extracted_root/LIDC-IDRI。"""
    zip_path = _maybe_convert_mnt_path(zip_path)
    extracted_root = _maybe_convert_mnt_path(extracted_root)
    if not os.path.exists(zip_path):
        raise FileNotFoundError(f"未找到 LIDC zip 文件: {zip_path}")

    lidc_dir = os.path.join(extracted_root, "LIDC-IDRI")
    if os.path.isdir(lidc_dir):
        return lidc_dir

    if not do_extract:
        raise FileNotFoundError(f"未找到解压目录 {lidc_dir}，且 extract_zip_if_needed=false")

    _ensure_dir(extracted_root)
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(extracted_root)

    if not os.path.isdir(lidc_dir):
        raise FileNotFoundError(f"解压后仍未找到目录: {lidc_dir}")
    return lidc_dir


def _malignancy_to_label(malignancy_scores: Sequence[float]) -> int:
    """按中位数映射标签。"""
    scores = np.asarray(malignancy_scores, dtype=np.float32)
    if scores.size == 0:
        return -1
    med = float(np.median(scores))
    if med < 3.0:
        return 0
    if med > 3.0:
        return 1
    return -1


def _hu_clip_norm(volume: np.ndarray, hu_clip: Tuple[float, float]) -> np.ndarray:
    lo, hi = float(hu_clip[0]), float(hu_clip[1])
    x = np.clip(volume.astype(np.float32), lo, hi)
    x = (x - lo) / (hi - lo + 1e-8)
    return x.astype(np.float32)


def _crop_with_padding(vol_zyx: np.ndarray, center_zyx: Tuple[int, int, int], size_zyx: Tuple[int, int, int]) -> np.ndarray:
    """中心裁剪，越界区域补零。"""
    d, h, w = [int(v) for v in size_zyx]
    cz, cy, cx = [int(v) for v in center_zyx]
    z1, y1, x1 = cz - d // 2, cy - h // 2, cx - w // 2
    z2, y2, x2 = z1 + d, y1 + h, x1 + w

    out = np.zeros((d, h, w), dtype=vol_zyx.dtype)
    sz1, sy1, sx1 = max(0, z1), max(0, y1), max(0, x1)
    sz2, sy2, sx2 = min(vol_zyx.shape[0], z2), min(vol_zyx.shape[1], y2), min(vol_zyx.shape[2], x2)
    dz1, dy1, dx1 = sz1 - z1, sy1 - y1, sx1 - x1
    dz2, dy2, dx2 = dz1 + (sz2 - sz1), dy1 + (sy2 - sy1), dx1 + (sx2 - sx1)
    if sz1 < sz2 and sy1 < sy2 and sx1 < sx2:
        out[dz1:dz2, dy1:dy2, dx1:dx2] = vol_zyx[sz1:sz2, sy1:sy2, sx1:sx2]
    return out


def _resize_to_64(roi_zyx: np.ndarray) -> np.ndarray:
    tz, ty, tx = 64, 64, 64
    zf = tz / max(1, roi_zyx.shape[0])
    yf = ty / max(1, roi_zyx.shape[1])
    xf = tx / max(1, roi_zyx.shape[2])
    return zoom(roi_zyx, (zf, yf, xf), order=1).astype(np.float32)


def _crop_center_mode(vol_zyx: np.ndarray, roi_size_zyx: Tuple[int, int, int]) -> np.ndarray:
    center = (vol_zyx.shape[0] // 2, vol_zyx.shape[1] // 2, vol_zyx.shape[2] // 2)
    return _crop_with_padding(vol_zyx, center, roi_size_zyx)


def _crop_diameter_bbox_mode(vol_zyx: np.ndarray, spacing_xyz: Tuple[float, float, float], diameter_list: Sequence[float]) -> np.ndarray:
    """
    近似“轮廓范围裁剪”：
    1) 以 patch 中心为核心
    2) 根据直径估计上下文范围
    3) 最后重采样到 64^3
    """
    d_mm = float(np.median(np.asarray(diameter_list, dtype=np.float32))) if len(diameter_list) > 0 else 10.0
    context_mm = max(16.0, d_mm * 4.0)
    sx, sy, sz = [float(v) for v in spacing_xyz]  # x,y,z
    size_x = max(16, int(round(context_mm / sx)))
    size_y = max(16, int(round(context_mm / sy)))
    size_z = max(16, int(round(context_mm / sz)))
    center = (vol_zyx.shape[0] // 2, vol_zyx.shape[1] // 2, vol_zyx.shape[2] // 2)
    roi = _crop_with_padding(vol_zyx, center, (size_z, size_y, size_x))
    return _resize_to_64(roi)


def _write_list(path: str, items: Sequence[Tuple[str, int]]) -> None:
    with open(path, "w", encoding="utf-8") as f:
        for fp, label in items:
            f.write(f"{fp} {int(label)}\n")


def run_preprocess(config: Dict) -> None:
    seed = int(config.get("seed", 42))
    random.seed(seed)
    np.random.seed(seed)

    run_dir = _resolve_run_dir("preprocess_lidc")
    run_log = os.path.join(run_dir, "logs", "run.log")
    summary_json = os.path.join(run_dir, "logs", "summary.json")
    _append_run_log(run_log, f"[START] preprocess_lidc seed={seed}")

    data_cfg = config["data"]
    pp_cfg = config["preprocess"]

    meta_path = _maybe_convert_mnt_path(data_cfg["lidc_meta_npy"])
    zip_path = _maybe_convert_mnt_path(data_cfg["lidc_zip"])
    extracted_root = _maybe_convert_mnt_path(data_cfg["extracted_root"])
    processed_root = _maybe_convert_mnt_path(data_cfg["processed_root"])
    benign_dir = _maybe_convert_mnt_path(data_cfg["benign_dir"])
    malignant_dir = _maybe_convert_mnt_path(data_cfg["malignant_dir"])
    train_list = _maybe_convert_mnt_path(data_cfg["train_list"])
    val_list = _maybe_convert_mnt_path(data_cfg["val_list"])
    test_list = _maybe_convert_mnt_path(data_cfg["test_list"])

    _ensure_dir(processed_root)
    _ensure_dir(benign_dir)
    _ensure_dir(malignant_dir)

    if not os.path.exists(meta_path):
        raise FileNotFoundError(f"未找到 LIDC 元数据 npy: {meta_path}")
    lidc_dir = _ensure_extracted(zip_path, extracted_root, bool(pp_cfg.get("extract_zip_if_needed", True)))

    dataset = np.load(meta_path, allow_pickle=True)
    max_cases = int(pp_cfg.get("max_cases", -1))
    if max_cases > 0:
        dataset = dataset[:max_cases]

    roi_size_zyx = tuple(pp_cfg.get("roi_size_zyx", [64, 64, 64]))
    crop_mode = str(pp_cfg.get("crop_mode", "center")).lower()
    hu_clip = tuple(pp_cfg.get("hu_clip", [-1200, 600]))
    overwrite = bool(pp_cfg.get("overwrite", False))

    out_items: List[Tuple[str, int]] = []
    skipped_uncertain = 0
    for idx, item in enumerate(tqdm(dataset, desc="LIDC1176 preprocessing")):
        entry = dict(item)
        label = _malignancy_to_label(entry.get("Malignancy", []))
        if label < 0:
            skipped_uncertain += 1
            continue

        filename = str(entry["Filename"])
        img_path = os.path.join(lidc_dir, filename)
        if not os.path.exists(img_path):
            alt = os.path.join(extracted_root, filename)
            if os.path.exists(alt):
                img_path = alt
            else:
                raise FileNotFoundError(f"未找到 patch 文件: {filename}")

        series_uid = str(entry.get("SeriesInstanceUID", f"series_{idx:04d}"))
        stem = f"{series_uid}_{idx:04d}"
        cls_dir = malignant_dir if label == 1 else benign_dir
        out_path = os.path.join(cls_dir, f"{stem}.npy")
        if (not overwrite) and os.path.exists(out_path):
            out_items.append((out_path, label))
            continue

        sitk_img = sitk.ReadImage(img_path)
        vol_zyx = sitk.GetArrayFromImage(sitk_img).astype(np.float32)
        spacing_xyz = tuple(float(v) for v in sitk_img.GetSpacing())
        vol_zyx = _hu_clip_norm(vol_zyx, hu_clip=hu_clip)

        if crop_mode == "diameter_bbox":
            roi = _crop_diameter_bbox_mode(vol_zyx, spacing_xyz, entry.get("Diameter", []))
        else:
            roi = _crop_center_mode(vol_zyx, roi_size_zyx=roi_size_zyx)
            if roi.shape != tuple(roi_size_zyx):
                roi = _resize_to_64(roi)

        if roi.shape != (64, 64, 64):
            roi = _resize_to_64(roi)

        np.save(out_path, roi.astype(np.float32))
        out_items.append((out_path, label))

    labels = np.array([y for _, y in out_items], dtype=np.int64)
    idx_all = np.arange(len(out_items))
    if len(idx_all) == 0:
        train_idx = np.array([], dtype=np.int64)
        val_idx = np.array([], dtype=np.int64)
    elif len(np.unique(labels)) < 2:
        train_idx = idx_all
        val_idx = np.array([], dtype=np.int64)
    else:
        val_ratio = float(pp_cfg.get("split_ratio", [0.8, 0.2, 0.0])[1])
        train_idx, val_idx = train_test_split(
            idx_all,
            test_size=val_ratio,
            random_state=seed,
            shuffle=True,
            stratify=labels,
        )

    train_items = [out_items[i] for i in train_idx.tolist()]
    val_items = [out_items[i] for i in val_idx.tolist()]
    test_items: List[Tuple[str, int]] = []

    _write_list(train_list, train_items)
    _write_list(val_list, val_items)
    _write_list(test_list, test_items)

    n_b = int(sum(1 for _, y in out_items if y == 0))
    n_m = int(sum(1 for _, y in out_items if y == 1))
    summary = {
        "task": "preprocess_lidc",
        "run_dir": run_dir,
        "processed_root": processed_root,
        "num_total": len(out_items),
        "num_benign": n_b,
        "num_malignant": n_m,
        "num_uncertain_dropped": skipped_uncertain,
        "num_train": len(train_items),
        "num_val": len(val_items),
        "num_test": len(test_items),
        "seed": seed,
    }
    with open(summary_json, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    _append_run_log(
        run_log,
        f"[DONE] total={len(out_items)} benign={n_b} malignant={n_m} dropped={skipped_uncertain} train={len(train_items)} val={len(val_items)}",
    )

    print("预处理完成:")
    print(f"- 有效样本: {len(out_items)} (benign={n_b}, malignant={n_m})")
    print(f"- 丢弃不确定(3分): {skipped_uncertain}")
    print(f"- train: {len(train_items)}")
    print(f"- val: {len(val_items)}")
    print(f"- test(空): {len(test_items)}")
    print(f"- run_dir: {run_dir}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="LIDC-IDRI_1176 预处理")
    parser.add_argument("--config", type=str, default="configs/cls_config.yaml", help="分类配置文件路径")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    run_preprocess(cfg)
