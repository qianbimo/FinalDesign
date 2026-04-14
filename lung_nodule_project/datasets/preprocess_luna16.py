"""
LUNA16 预处理脚本。

功能:
1. 读取 .mhd/.raw CT
2. 读取 annotations.csv
3. 根据结节中心与直径生成 3D 球形 mask(0/1)
4. 重采样到统一 spacing(默认 1mm^3)
5. HU 截断并归一化
6. 保存为 .npy，并生成 train/val/test 列表(8/2/0)
7. 运行日志写入 workspace/runs/<...>/logs
"""

from __future__ import annotations

import argparse
import json
import os
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

import numpy as np
import pandas as pd
import SimpleITK as sitk
import yaml
from tqdm import tqdm


def _ensure_dir(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)


def _load_yaml(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _now_tag() -> str:
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def _resolve_run_dir(default_task: str = "preprocess_luna") -> str:
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


def _find_series_map(luna_root: str) -> Dict[str, str]:
    """扫描 subset* 目录，建立 seriesuid -> mhd 路径映射。"""
    root = Path(luna_root)
    mapping: Dict[str, str] = {}
    for sub in sorted(root.glob("subset*")):
        for p in sorted(sub.glob("*.mhd")):
            mapping[p.stem] = str(p)
    return mapping


def _resample_image(
    image: sitk.Image,
    target_spacing_xyz: Tuple[float, float, float],
    is_mask: bool,
) -> sitk.Image:
    """将 SITK 图像重采样到目标 spacing。"""
    old_spacing = np.array(list(image.GetSpacing()), dtype=np.float64)
    old_size = np.array(list(image.GetSize()), dtype=np.int32)
    target_spacing = np.array(target_spacing_xyz, dtype=np.float64)
    new_size = np.maximum(1, np.round(old_size * old_spacing / target_spacing)).astype(np.int32)

    resampler = sitk.ResampleImageFilter()
    resampler.SetOutputSpacing(tuple(float(x) for x in target_spacing))
    resampler.SetSize([int(x) for x in new_size.tolist()])
    resampler.SetOutputDirection(image.GetDirection())
    resampler.SetOutputOrigin(image.GetOrigin())
    resampler.SetTransform(sitk.Transform())
    resampler.SetDefaultPixelValue(0)
    resampler.SetInterpolator(sitk.sitkNearestNeighbor if is_mask else sitk.sitkLinear)
    return resampler.Execute(image)


def _build_spherical_mask(image: sitk.Image, ann_rows: pd.DataFrame) -> sitk.Image:
    """
    按注释中心与直径生成球形 mask。
    注释坐标为世界坐标(mm)，先映射到体素坐标。
    """
    size_x, size_y, size_z = image.GetSize()
    spacing_x, spacing_y, spacing_z = image.GetSpacing()
    mask = np.zeros((size_z, size_y, size_x), dtype=np.uint8)  # z,y,x

    for _, row in ann_rows.iterrows():
        cx_mm = float(row["coordX"])
        cy_mm = float(row["coordY"])
        cz_mm = float(row["coordZ"])
        diameter_mm = float(row["diameter_mm"])
        radius_mm = max(0.5, diameter_mm / 2.0)

        cx, cy, cz = image.TransformPhysicalPointToContinuousIndex((cx_mm, cy_mm, cz_mm))

        rx = int(np.ceil(radius_mm / spacing_x))
        ry = int(np.ceil(radius_mm / spacing_y))
        rz = int(np.ceil(radius_mm / spacing_z))

        x1, x2 = max(0, int(np.floor(cx - rx))), min(size_x - 1, int(np.ceil(cx + rx)))
        y1, y2 = max(0, int(np.floor(cy - ry))), min(size_y - 1, int(np.ceil(cy + ry)))
        z1, z2 = max(0, int(np.floor(cz - rz))), min(size_z - 1, int(np.ceil(cz + rz)))
        if x1 > x2 or y1 > y2 or z1 > z2:
            continue

        zz, yy, xx = np.meshgrid(
            np.arange(z1, z2 + 1, dtype=np.float32),
            np.arange(y1, y2 + 1, dtype=np.float32),
            np.arange(x1, x2 + 1, dtype=np.float32),
            indexing="ij",
        )
        dx = (xx - cx) * spacing_x
        dy = (yy - cy) * spacing_y
        dz = (zz - cz) * spacing_z
        dist2 = dx * dx + dy * dy + dz * dz
        sphere = (dist2 <= radius_mm * radius_mm).astype(np.uint8)
        block = mask[z1 : z2 + 1, y1 : y2 + 1, x1 : x2 + 1]
        mask[z1 : z2 + 1, y1 : y2 + 1, x1 : x2 + 1] = np.maximum(block, sphere)

    mask_img = sitk.GetImageFromArray(mask)
    mask_img.CopyInformation(image)
    return mask_img


def _window_normalize(volume_zyx: np.ndarray, hu_clip: Tuple[float, float]) -> np.ndarray:
    """肺窗截断 + 归一化到 [0, 1]。"""
    lo, hi = float(hu_clip[0]), float(hu_clip[1])
    vol = np.clip(volume_zyx.astype(np.float32), lo, hi)
    vol = (vol - lo) / (hi - lo + 1e-8)
    return vol.astype(np.float32)


def _process_single_series(
    series_uid: str,
    mhd_path: str,
    ann_df: pd.DataFrame,
    image_out_dir: str,
    mask_out_dir: str,
    target_spacing_xyz: Tuple[float, float, float],
    hu_clip: Tuple[float, float],
    overwrite: bool,
) -> Tuple[str, str]:
    image_path = os.path.join(image_out_dir, f"{series_uid}.npy")
    mask_path = os.path.join(mask_out_dir, f"{series_uid}.npy")
    if (not overwrite) and os.path.exists(image_path) and os.path.exists(mask_path):
        return image_path, mask_path

    image = sitk.ReadImage(mhd_path)
    ann_rows = ann_df[ann_df["seriesuid"] == series_uid]
    mask = _build_spherical_mask(image, ann_rows)

    image_rs = _resample_image(image, target_spacing_xyz, is_mask=False)
    mask_rs = _resample_image(mask, target_spacing_xyz, is_mask=True)

    image_zyx = sitk.GetArrayFromImage(image_rs).astype(np.float32)
    mask_zyx = sitk.GetArrayFromImage(mask_rs).astype(np.uint8)
    image_zyx = _window_normalize(image_zyx, hu_clip=hu_clip)
    mask_zyx = (mask_zyx > 0).astype(np.uint8)

    np.save(image_path, image_zyx)
    np.save(mask_path, mask_zyx)
    return image_path, mask_path


def _write_split_lists(
    pairs: Sequence[Tuple[str, str]],
    train_list_path: str,
    val_list_path: str,
    test_list_path: str,
    split_ratio: Tuple[float, float, float],
    seed: int,
) -> None:
    """按 8/2/0 生成列表文件，test 为空占位。"""
    pairs = list(pairs)
    random.Random(seed).shuffle(pairs)

    n = len(pairs)
    r_train, _, _ = split_ratio
    n_train = int(round(n * r_train))
    n_train = min(max(0, n_train), n)

    train_pairs = pairs[:n_train]
    val_pairs = pairs[n_train:]
    test_pairs: List[Tuple[str, str]] = []

    def _write(path: str, seq: Sequence[Tuple[str, str]]) -> None:
        with open(path, "w", encoding="utf-8") as f:
            for img_p, msk_p in seq:
                f.write(f"{img_p} {msk_p}\n")

    _write(train_list_path, train_pairs)
    _write(val_list_path, val_pairs)
    _write(test_list_path, test_pairs)


def run_preprocess(config: Dict) -> None:
    seed = int(config.get("seed", 42))
    random.seed(seed)
    np.random.seed(seed)

    run_dir = _resolve_run_dir("preprocess_luna")
    run_log = os.path.join(run_dir, "logs", "run.log")
    summary_json = os.path.join(run_dir, "logs", "summary.json")
    _append_run_log(run_log, f"[START] preprocess_luna seed={seed}")

    data_cfg = config["data"]
    pp_cfg = config["preprocess"]
    luna_root = data_cfg["luna_root"]
    ann_csv = data_cfg["annotations_csv"]
    image_dir = data_cfg["image_dir"]
    mask_dir = data_cfg["mask_dir"]
    train_list = data_cfg["train_list"]
    val_list = data_cfg["val_list"]
    test_list = data_cfg["test_list"]

    _ensure_dir(data_cfg["processed_root"])
    _ensure_dir(image_dir)
    _ensure_dir(mask_dir)

    if not os.path.exists(ann_csv):
        raise FileNotFoundError(f"未找到 annotations.csv: {ann_csv}")
    ann_df = pd.read_csv(ann_csv)

    series_map = _find_series_map(luna_root)
    if not series_map:
        raise FileNotFoundError(f"在 {luna_root}/subset* 下未找到 .mhd 文件")

    max_cases = int(pp_cfg.get("max_cases", -1))
    series_items = sorted(series_map.items(), key=lambda x: x[0])
    if max_cases > 0:
        series_items = series_items[:max_cases]

    target_spacing_xyz = tuple(pp_cfg.get("target_spacing_xyz", [1.0, 1.0, 1.0]))
    hu_clip = tuple(pp_cfg.get("hu_clip", [-1200, 600]))
    overwrite = bool(pp_cfg.get("overwrite", False))
    num_workers = int(pp_cfg.get("num_workers", 4))

    pairs: List[Tuple[str, str]] = []
    futures = []
    with ThreadPoolExecutor(max_workers=max(1, num_workers)) as ex:
        for uid, mhd_path in series_items:
            futures.append(
                ex.submit(
                    _process_single_series,
                    uid,
                    mhd_path,
                    ann_df,
                    image_dir,
                    mask_dir,
                    target_spacing_xyz,
                    hu_clip,
                    overwrite,
                )
            )
        for fut in tqdm(as_completed(futures), total=len(futures), desc="LUNA16 preprocessing"):
            pairs.append(fut.result())

    pairs.sort(key=lambda x: Path(x[0]).stem)
    split_ratio = tuple(pp_cfg.get("split_ratio", [0.8, 0.2, 0.0]))
    _write_split_lists(pairs, train_list, val_list, test_list, split_ratio, seed)

    with open(train_list, "r", encoding="utf-8") as f:
        train_count = sum(1 for line in f if line.strip())
    with open(val_list, "r", encoding="utf-8") as f:
        val_count = sum(1 for line in f if line.strip())
    with open(test_list, "r", encoding="utf-8") as f:
        test_count = sum(1 for line in f if line.strip())

    summary = {
        "task": "preprocess_luna",
        "run_dir": run_dir,
        "processed_root": data_cfg["processed_root"],
        "num_total": len(pairs),
        "num_train": train_count,
        "num_val": val_count,
        "num_test": test_count,
        "seed": seed,
    }
    with open(summary_json, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    _append_run_log(run_log, f"[DONE] total={len(pairs)} train={train_count} val={val_count} test={test_count}")

    print("预处理完成:")
    print(f"- 样本数: {len(pairs)}")
    print(f"- 图像目录: {image_dir}")
    print(f"- 掩码目录: {mask_dir}")
    print(f"- train_list: {train_list}")
    print(f"- val_list: {val_list}")
    print(f"- test_list(空): {test_list}")
    print(f"- run_dir: {run_dir}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="LUNA16 预处理")
    parser.add_argument("--config", type=str, default="configs/seg_config.yaml", help="分割配置文件路径")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    cfg = _load_yaml(args.config)
    run_preprocess(cfg)
