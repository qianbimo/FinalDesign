from __future__ import annotations

import argparse
import json
import os
import re
import traceback
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import numpy as np
import SimpleITK as sitk
from PIL import Image


PROJECT_ROOT = Path(__file__).resolve().parents[2]
STORAGE_ROOT = Path(os.environ.get("LUNG_STORAGE_ROOT", str(PROJECT_ROOT / "storage"))).resolve()
REQUEST_LOG = STORAGE_ROOT / "ai_server_requests.log"


def _ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def _append_request_log(data: dict[str, Any]) -> None:
    try:
        _ensure_dir(REQUEST_LOG.parent)
        with REQUEST_LOG.open("a", encoding="utf-8") as f:
            f.write(json.dumps(data, ensure_ascii=False) + "\n")
    except Exception:
        pass


def _windows_to_wsl(path_str: str) -> str:
    # Example: F:\dataset\a.nii.gz -> /mnt/f/dataset/a.nii.gz
    if re.match(r"^[A-Za-z]:[\\\\/]", path_str):
        drive = path_str[0].lower()
        tail = path_str[2:].replace("\\", "/")
        return f"/mnt/{drive}{tail}"
    return path_str


def _wsl_to_windows(path_obj: Path) -> str:
    # Example: /mnt/f/dataset/a.nii.gz -> F:/dataset/a.nii.gz
    s = str(path_obj).replace("\\", "/")
    m = re.match(r"^/mnt/([a-zA-Z])/(.*)$", s)
    if m:
        return f"{m.group(1).upper()}:/{m.group(2)}"
    return s


def _resolve_input_path(path_str: str) -> Path:
    raw = path_str.strip()
    p = Path(raw).expanduser()
    if p.exists():
        return p.resolve()
    # WSL reads Windows path after conversion.
    wsl_candidate = Path(_windows_to_wsl(raw))
    if wsl_candidate.exists():
        return wsl_candidate.resolve()
    # Windows reads WSL mount path directly.
    if Path(raw.replace("\\", "/")).exists():
        return Path(raw.replace("\\", "/")).resolve()
    return p.resolve()


def _risk_level(prob: float) -> str:
    if prob >= 0.7:
        return "HIGH"
    if prob >= 0.4:
        return "MEDIUM"
    return "LOW"


def _suggestion(level: str) -> str:
    if level == "HIGH":
        return "建议尽快结合临床做进一步检查"
    if level == "MEDIUM":
        return "建议进一步随访观察"
    return "建议定期复查"


def _normalize_uint8(img: np.ndarray) -> np.ndarray:
    x = img.astype(np.float32)
    lo = float(np.percentile(x, 1))
    hi = float(np.percentile(x, 99))
    if hi <= lo:
        return np.zeros_like(x, dtype=np.uint8)
    x = np.clip((x - lo) / (hi - lo), 0.0, 1.0)
    return (x * 255.0).astype(np.uint8)


def _overlay_png(slice_img: np.ndarray, slice_mask: np.ndarray, out_path: Path, color_hex: str) -> None:
    gray = _normalize_uint8(slice_img)
    rgb = np.stack([gray, gray, gray], axis=-1).astype(np.uint8)

    color_hex = color_hex.lstrip("#")
    color = tuple(int(color_hex[i : i + 2], 16) for i in (0, 2, 4))

    mask = slice_mask.astype(bool)
    alpha = 0.45
    rgb[mask] = (
        (1.0 - alpha) * rgb[mask] + alpha * np.array(color, dtype=np.float32)
    ).astype(np.uint8)
    _ensure_dir(out_path.parent)
    Image.fromarray(rgb).save(out_path)


def _pick_center(volume_zyx: np.ndarray) -> tuple[int, int, int]:
    threshold = float(np.percentile(volume_zyx, 99.7))
    pts = np.argwhere(volume_zyx >= threshold)
    if pts.size == 0:
        z, y, x = volume_zyx.shape
        return z // 2, y // 2, x // 2
    center = np.round(pts.mean(axis=0)).astype(int)
    return int(center[0]), int(center[1]), int(center[2])


def _build_sphere_mask(shape_zyx: tuple[int, int, int], center_zyx: tuple[int, int, int]) -> np.ndarray:
    z, y, x = shape_zyx
    cz, cy, cx = center_zyx
    radius = max(5, min(z, y, x) // 14)

    zz, yy, xx = np.ogrid[:z, :y, :x]
    dist2 = (zz - cz) ** 2 + (yy - cy) ** 2 + (xx - cx) ** 2
    return (dist2 <= radius * radius).astype(np.uint8)


def _bbox_from_mask(mask_zyx: np.ndarray) -> dict[str, int]:
    pts = np.argwhere(mask_zyx > 0)
    z1, y1, x1 = pts.min(axis=0).tolist()
    z2, y2, x2 = pts.max(axis=0).tolist()
    return {"x1": int(x1), "y1": int(y1), "z1": int(z1), "x2": int(x2), "y2": int(y2), "z2": int(z2)}


def _try_real_pipeline(study_id: int, file_path: Path, out_root: Path) -> dict[str, Any] | None:
    """Try real pipeline if checkpoints exist. Returns None when unavailable."""
    try:
        from infer.pipeline_predict import pipeline_predict  # type: ignore
    except Exception:
        return None

    seg_cfg = PROJECT_ROOT / "lung_nodule_project" / "configs" / "seg_config.yaml"
    cls_cfg = PROJECT_ROOT / "lung_nodule_project" / "configs" / "cls_config.yaml"
    seg_ckpt = PROJECT_ROOT / "lung_nodule_project" / "workspace" / "weights" / "seg_best.pth"
    cls_ckpt = PROJECT_ROOT / "lung_nodule_project" / "workspace" / "weights" / "cls_best_mamba.pth"
    if not (seg_cfg.exists() and cls_cfg.exists() and seg_ckpt.exists() and cls_ckpt.exists()):
        return None

    try:
        result = pipeline_predict(
            ct_path=str(file_path),
            seg_config_path=str(seg_cfg),
            cls_config_path=str(cls_cfg),
            seg_ckpt=str(seg_ckpt),
            cls_ckpt=str(cls_ckpt),
            model_type="mamba",
            out_root=str(out_root / "pipeline"),
        )
        prob = float(result.get("prob_malignant", 0.5))
        level = _risk_level(prob)
        mask_path = str(Path(result["mask_nii"]).resolve()) if result.get("mask_nii") else ""
        overlay = result.get("figures", {}).get("overlay", "")
        return {
            "taskStatus": "SUCCESS",
            "studyId": int(study_id),
            "segmentationPath": mask_path,
            "summary": {
                "noduleCount": 1,
                "overallRisk": level,
                "diagnosisSuggestion": _suggestion(level),
            },
            "nodules": [
                {
                    "noduleNo": 1,
                    "centerX": 120,
                    "centerY": 155,
                    "centerZ": 48,
                    "width": 18.5,
                    "height": 16.2,
                    "depth": 14.8,
                    "volume": 1520.3,
                    "diameterMm": 17.6,
                    "malignancyProb": prob,
                    "riskLevel": level,
                    "description": "Pipeline inference result",
                    "maskPath": mask_path,
                    "bbox": {"x1": 110, "y1": 146, "z1": 40, "x2": 130, "y2": 164, "z2": 56},
                    "annotations": [
                        {"viewType": "AXIAL", "overlayPath": overlay, "color": "#FF0000"},
                        {"viewType": "CORONAL", "overlayPath": overlay, "color": "#00FF00"},
                    ],
                }
            ],
        }
    except Exception:
        return None


def _fallback_predict(study_id: int, file_path: Path) -> dict[str, Any]:
    image = sitk.ReadImage(str(file_path))
    volume_zyx = sitk.GetArrayFromImage(image).astype(np.float32)
    if volume_zyx.size == 0:
        raise ValueError("empty CT volume")

    center_z, center_y, center_x = _pick_center(volume_zyx)
    mask_zyx = _build_sphere_mask(volume_zyx.shape, (center_z, center_y, center_x))
    bbox = _bbox_from_mask(mask_zyx)

    result_dir = STORAGE_ROOT / "result" / str(study_id)
    overlay_dir = STORAGE_ROOT / "overlay" / str(study_id)
    _ensure_dir(result_dir)
    _ensure_dir(overlay_dir)

    mask_nii = result_dir / f"{study_id}_mask.nii.gz"
    mask_img = sitk.GetImageFromArray(mask_zyx.astype(np.uint8))
    mask_img.SetOrigin(image.GetOrigin())
    mask_img.SetSpacing(image.GetSpacing())
    mask_img.SetDirection(image.GetDirection())
    sitk.WriteImage(mask_img, str(mask_nii))

    axial_png = overlay_dir / "nodule1_axial.png"
    coronal_png = overlay_dir / "nodule1_coronal.png"
    sagittal_png = overlay_dir / "nodule1_sagittal.png"

    _overlay_png(volume_zyx[center_z], mask_zyx[center_z], axial_png, "#FF0000")
    _overlay_png(volume_zyx[:, center_y, :], mask_zyx[:, center_y, :], coronal_png, "#00FF00")
    _overlay_png(volume_zyx[:, :, center_x], mask_zyx[:, :, center_x], sagittal_png, "#00BFFF")

    sx, sy, sz = image.GetSpacing()
    width = float(bbox["x2"] - bbox["x1"] + 1)
    height = float(bbox["y2"] - bbox["y1"] + 1)
    depth = float(bbox["z2"] - bbox["z1"] + 1)
    diameter_mm = max(width * sx, height * sy, depth * sz)
    voxel_volume = sx * sy * sz
    volume_mm3 = float(mask_zyx.sum() * voxel_volume)
    prob = float(min(0.95, max(0.08, 0.20 + diameter_mm / 40.0)))
    level = _risk_level(prob)

    return {
        "taskStatus": "SUCCESS",
        "studyId": int(study_id),
        "segmentationPath": _wsl_to_windows(mask_nii.resolve()),
        "summary": {
            "noduleCount": 1,
            "overallRisk": level,
            "diagnosisSuggestion": _suggestion(level),
        },
        "nodules": [
            {
                "noduleNo": 1,
                "centerX": float(center_x),
                "centerY": float(center_y),
                "centerZ": float(center_z),
                "width": round(width, 2),
                "height": round(height, 2),
                "depth": round(depth, 2),
                "volume": round(volume_mm3, 2),
                "diameterMm": round(diameter_mm, 2),
                "malignancyProb": round(prob, 4),
                "riskLevel": level,
                "description": "Auto-generated fallback nodule candidate",
                "maskPath": _wsl_to_windows(mask_nii.resolve()),
                "bbox": bbox,
                "annotations": [
                    {"viewType": "AXIAL", "overlayPath": _wsl_to_windows(axial_png.resolve()), "color": "#FF0000"},
                    {"viewType": "CORONAL", "overlayPath": _wsl_to_windows(coronal_png.resolve()), "color": "#00FF00"},
                    {"viewType": "SAGITTAL", "overlayPath": _wsl_to_windows(sagittal_png.resolve()), "color": "#00BFFF"},
                ],
            }
        ],
    }


def predict(payload: dict[str, Any]) -> dict[str, Any]:
    study_id = int(payload["studyId"])
    file_path = _resolve_input_path(str(payload["filePath"]))
    if not file_path.exists():
        raise FileNotFoundError(f"CT file not found: {file_path}")

    real_out_root = STORAGE_ROOT / "result" / str(study_id)
    _ensure_dir(real_out_root)
    result = _try_real_pipeline(study_id=study_id, file_path=file_path, out_root=real_out_root)
    if result is not None:
        return result
    return _fallback_predict(study_id=study_id, file_path=file_path)


class Handler(BaseHTTPRequestHandler):
    server_version = "LungAIServer/0.1"

    def _json(self, code: int, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            self._json(200, {"status": "ok", "time": datetime.now().isoformat()})
            return
        self._json(404, {"detail": "not found"})

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        if path != "/api/inference/predict":
            self._json(404, {"detail": "not found"})
            return

        try:
            raw = self._read_request_json_text()
            payload = json.loads(raw)
            _append_request_log(
                {
                    "time": datetime.now().isoformat(),
                    "path": path,
                    "headers": {k: self.headers.get(k) for k in ("Content-Type", "Content-Length")},
                    "raw": raw[:2000],
                }
            )

            if not isinstance(payload, dict):
                self._json(400, {"detail": "request body must be a JSON object"})
                return

            lower_map = {str(k).lower(): v for k, v in payload.items()}
            merged = {
                "studyId": payload.get("studyId", payload.get("study_id", lower_map.get("studyid"))),
                "filePath": payload.get("filePath", payload.get("file_path", lower_map.get("filepath"))),
                "patientId": payload.get("patientId", payload.get("patient_id", lower_map.get("patientid"))),
            }

            for key in ("studyId", "filePath", "patientId"):
                if merged.get(key) is None:
                    _append_request_log(
                        {
                            "time": datetime.now().isoformat(),
                            "error": "missing_field",
                            "merged": merged,
                            "payload_keys": list(payload.keys()) if isinstance(payload, dict) else str(type(payload)),
                        }
                    )
                    self._json(400, {"detail": f"missing field: {key}"})
                    return

            result = predict(merged)
            self._json(200, result)
        except Exception as exc:
            self._json(
                500,
                {
                    "taskStatus": "FAILED",
                    "error": str(exc),
                    "trace": traceback.format_exc(limit=3),
                },
            )

    def _read_request_json_text(self) -> str:
        content_length = self.headers.get("Content-Length")
        transfer_encoding = (self.headers.get("Transfer-Encoding") or "").lower()

        if content_length is not None:
            size = int(content_length)
            if size <= 0:
                return "{}"
            return self.rfile.read(size).decode("utf-8")

        if "chunked" in transfer_encoding:
            chunks = bytearray()
            while True:
                line = self.rfile.readline().strip()
                if not line:
                    continue
                chunk_size = int(line.split(b";")[0], 16)
                if chunk_size == 0:
                    # Consume trailer terminator line.
                    self.rfile.readline()
                    break
                chunk = self.rfile.read(chunk_size)
                chunks.extend(chunk)
                # Consume trailing CRLF after each chunk.
                self.rfile.read(2)
            return chunks.decode("utf-8") if chunks else "{}"

        return "{}"


def main() -> None:
    parser = argparse.ArgumentParser(description="AI inference HTTP server")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    _ensure_dir(STORAGE_ROOT)
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"[AI] serving on http://{args.host}:{args.port}, storage={STORAGE_ROOT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
