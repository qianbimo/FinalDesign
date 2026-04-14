#!/usr/bin/env bash

# 若被 sh/zsh 调用，自动切回 bash 重新执行，避免 pipefail 报错
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi

set -euo pipefail

# 肺结节项目统一训练脚本
# 支持:
# 1) 分步训练(staged)
# 2) 完整训练(full)
# 3) 所有结果统一落盘到 workspace/runs/<内容_开始时间>/

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

MODE="full"                    # full | staged
PRESET="full"                  # full | smoke
STAGES="preprocess_luna,preprocess_lidc,train_seg,train_cls"
WITH_INFER=0
CT_PATH=""
MODEL_TYPE="mamba"
CONDA_ENV_NAME="${CONDA_ENV_NAME:-luna16_dl}"
SEG_EPOCHS_OVERRIDE=""
CLS_EPOCHS_OVERRIDE=""
SEG_BATCH_SIZE_OVERRIDE=""
CLS_BATCH_SIZE_OVERRIDE=""

usage() {
  cat <<'EOF'
用法:
  bash run_training.sh [选项]

核心模式:
  --mode full|staged
    full   : 一键全流程(预处理 + 分割训练 + 分类训练)
    staged : 分步执行，配合 --stages 指定阶段

配置预设:
  --preset full|smoke
    full  : 使用 configs/seg_config.yaml + configs/cls_config.yaml
    smoke : 使用 configs/seg_smoke.yaml + configs/cls_smoke.yaml

阶段列表(仅 staged 模式生效):
  --stages preprocess_luna,preprocess_lidc,train_seg,train_cls,infer

其它参数:
  --with-infer               full 模式下训练后追加一次串联推理
  --ct <path>               推理输入 CT 路径；不传则自动选择样例
  --model mamba|cnn|cnn_transformer
  --conda-env <name>        默认 luna16_dl
  --seg-epochs <int>        覆盖分割训练 epoch
  --cls-epochs <int>        覆盖分类训练 epoch
  -h, --help

示例:
  # 一键完整训练(正式配置)
  bash run_training.sh --mode full --preset full --model mamba

  # 分步训练(只跑预处理+分割)
  bash run_training.sh --mode staged --preset smoke --stages preprocess_luna,train_seg --seg-epochs 1

  # 完整训练后追加一次推理
  bash run_training.sh --mode full --preset full --with-infer --ct /mnt/f/dataset/LUNA16/subset0/xxx.mhd
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --preset)
      PRESET="${2:-}"
      shift 2
      ;;
    --stages)
      STAGES="${2:-}"
      shift 2
      ;;
    --with-infer)
      WITH_INFER=1
      shift
      ;;
    --ct)
      CT_PATH="${2:-}"
      shift 2
      ;;
    --model)
      MODEL_TYPE="${2:-}"
      shift 2
      ;;
    --conda-env)
      CONDA_ENV_NAME="${2:-}"
      shift 2
      ;;
    --seg-epochs)
      SEG_EPOCHS_OVERRIDE="${2:-}"
      shift 2
      ;;
    --cls-epochs)
      CLS_EPOCHS_OVERRIDE="${2:-}"
      shift 2
      ;;
    --seg-batch-size)
      SEG_BATCH_SIZE_OVERRIDE="${2:-}"
      shift 2
      ;;
    --cls-batch-size)
      CLS_BATCH_SIZE_OVERRIDE="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[ERROR] 未知参数: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ "$MODE" != "full" && "$MODE" != "staged" ]]; then
  echo "[ERROR] --mode 仅支持 full 或 staged"
  exit 1
fi
if [[ "$PRESET" != "full" && "$PRESET" != "smoke" ]]; then
  echo "[ERROR] --preset 仅支持 full 或 smoke"
  exit 1
fi
if [[ "$MODEL_TYPE" != "mamba" && "$MODEL_TYPE" != "cnn" && "$MODEL_TYPE" != "cnn_transformer" ]]; then
  echo "[ERROR] --model 仅支持 mamba/cnn/cnn_transformer"
  exit 1
fi

if [[ "$PRESET" == "smoke" ]]; then
  SEG_CONFIG="configs/seg_smoke.yaml"
  CLS_CONFIG="configs/cls_smoke.yaml"
else
  SEG_CONFIG="configs/seg_config.yaml"
  CLS_CONFIG="configs/cls_config.yaml"
fi

activate_conda() {
  if [[ -f "$HOME/anaconda3/etc/profile.d/conda.sh" ]]; then
    # shellcheck disable=SC1091
    source "$HOME/anaconda3/etc/profile.d/conda.sh"
  elif [[ -f "$HOME/miniconda3/etc/profile.d/conda.sh" ]]; then
    # shellcheck disable=SC1091
    source "$HOME/miniconda3/etc/profile.d/conda.sh"
  else
    echo "[ERROR] 未找到 conda.sh，请确认 Conda 已安装。"
    exit 1
  fi
  conda activate "$CONDA_ENV_NAME"
}

START_TS="$(date +%Y%m%d_%H%M%S)"
if [[ "$MODE" == "full" ]]; then
  RUN_TAG="${MODE}_${PRESET}_${MODEL_TYPE}"
else
  STAGE_TAG="$(echo "$STAGES" | tr ',' '-' | tr ' ' '_')"
  RUN_TAG="${MODE}_${PRESET}_${MODEL_TYPE}_${STAGE_TAG}"
fi
RUN_ROOT="workspace/runs/${RUN_TAG}_${START_TS}"
mkdir -p "$RUN_ROOT"

echo "Run root: $RUN_ROOT"
printf '%q ' "$0" "$@" > "${RUN_ROOT}/command.txt"
echo >> "${RUN_ROOT}/command.txt"
{
  echo "mode=$MODE"
  echo "preset=$PRESET"
  echo "model=$MODEL_TYPE"
  echo "stages=$STAGES"
  echo "start_ts=$START_TS"
  echo "seg_config=$SEG_CONFIG"
  echo "cls_config=$CLS_CONFIG"
} > "${RUN_ROOT}/run_meta.txt"

run_step() {
  local step="$1"
  case "$step" in
    preprocess_luna)
      echo "==== [STEP] preprocess_luna ===="
      LUNG_RUN_DIR="$RUN_ROOT" LUNG_RUN_STEP="preprocess_luna" \
        python datasets/preprocess_luna16.py --config "$SEG_CONFIG"
      ;;
    preprocess_lidc)
      echo "==== [STEP] preprocess_lidc ===="
      LUNG_RUN_DIR="$RUN_ROOT" LUNG_RUN_STEP="preprocess_lidc" \
        python datasets/preprocess_lidc.py --config "$CLS_CONFIG"
      ;;
    train_seg)
      echo "==== [STEP] train_seg ===="
      local seg_cmd=(python train/train_seg.py --config "$SEG_CONFIG")
      if [[ -n "$SEG_EPOCHS_OVERRIDE" ]]; then
        seg_cmd+=(--epochs "$SEG_EPOCHS_OVERRIDE")
      fi
      if [[ -n "$SEG_BATCH_SIZE_OVERRIDE" ]]; then
        seg_cmd+=(--batch-size "$SEG_BATCH_SIZE_OVERRIDE")
      fi
      LUNG_RUN_DIR="$RUN_ROOT" LUNG_RUN_STEP="train_seg" "${seg_cmd[@]}"
      ;;
    train_cls)
      echo "==== [STEP] train_cls (${MODEL_TYPE}) ===="
      local cls_cmd=(python train/train_cls.py --config "$CLS_CONFIG" --model "$MODEL_TYPE")
      if [[ -n "$CLS_EPOCHS_OVERRIDE" ]]; then
        cls_cmd+=(--epochs "$CLS_EPOCHS_OVERRIDE")
      fi
      if [[ -n "$CLS_BATCH_SIZE_OVERRIDE" ]]; then
        cls_cmd+=(--batch-size "$CLS_BATCH_SIZE_OVERRIDE")
      fi
      LUNG_RUN_DIR="$RUN_ROOT" LUNG_RUN_STEP="train_cls" "${cls_cmd[@]}"
      ;;
    infer)
      echo "==== [STEP] infer_pipeline ===="
      local ct_input="$CT_PATH"
      if [[ -z "$ct_input" ]]; then
        if ls /mnt/f/dataset/LUNA16/subset*/*.mhd >/dev/null 2>&1; then
          ct_input="$(ls /mnt/f/dataset/LUNA16/subset*/*.mhd | head -n 1)"
        elif ls /mnt/e/dataset/LUNA16/subset*/*.mhd >/dev/null 2>&1; then
          ct_input="$(ls /mnt/e/dataset/LUNA16/subset*/*.mhd | head -n 1)"
        elif ls /mnt/e/dataset/LUNA16/release/subset*/*.mhd >/dev/null 2>&1; then
          ct_input="$(ls /mnt/e/dataset/LUNA16/release/subset*/*.mhd | head -n 1)"
        else
          echo "[ERROR] 未提供 --ct，且未找到可用默认样例。"
          exit 1
        fi
      fi
      LUNG_RUN_DIR="$RUN_ROOT" LUNG_RUN_STEP="infer" \
        python infer/pipeline_predict.py \
          --ct "$ct_input" \
          --seg_config "$SEG_CONFIG" \
          --cls_config "$CLS_CONFIG" \
          --seg_ckpt "workspace/weights/seg_best.pth" \
          --cls_ckpt "workspace/weights/cls_best_${MODEL_TYPE}.pth" \
          --model "$MODEL_TYPE" \
          --out_root "$RUN_ROOT/infer"
      ;;
    *)
      echo "[ERROR] 未知阶段: $step"
      exit 1
      ;;
  esac
}

activate_conda
echo "==== Conda Env: $CONDA_ENV_NAME ===="
python -V

if [[ "$MODE" == "full" ]]; then
  run_step preprocess_luna
  run_step preprocess_lidc
  run_step train_seg
  run_step train_cls
  if [[ "$WITH_INFER" -eq 1 ]]; then
    run_step infer
  fi
else
  IFS=',' read -r -a arr <<< "$STAGES"
  for s in "${arr[@]}"; do
    s="$(echo "$s" | xargs)"
    [[ -z "$s" ]] && continue
    run_step "$s"
  done
fi

echo "==== ALL DONE ===="
echo "Run outputs saved to: $RUN_ROOT"
