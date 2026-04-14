# 基于 3D Res-UNet + Mamba 的肺结节分类算法设计与应用

> 说明：已统一为单一工作目录 `workspace/`，所有 `processed / weights / outputs` 都会写入该目录。

本项目是一个“本地最小可运行版”医学影像深度学习工程，聚焦毕设前期验证，不包含前后端系统。

## 1. 项目目标

实现以下闭环流程：

1. 数据预处理
2. LUNA16 分割训练（3D Res-UNet）
3. LIDC-IDRI_1176 分类训练（ResNet3D + Mamba / 对比模型）
4. 单例串联推理（分割 -> ROI -> 分类）
5. 可视化与结果导出

## 2. 目录结构

```text
lung_nodule_project/
├── configs/
│   ├── seg_config.yaml
│   └── cls_config.yaml
├── datasets/
│   ├── preprocess_luna16.py
│   ├── preprocess_lidc.py
│   ├── dataset_seg.py
│   └── dataset_cls.py
├── models/
│   ├── resunet3d.py
│   ├── resnet3d_backbone.py
│   ├── mamba_classifier.py
│   └── cnn_transformer_baseline.py
├── train/
│   ├── train_seg.py
│   ├── train_cls.py
│   └── utils.py
├── infer/
│   ├── infer_seg.py
│   ├── infer_cls.py
│   ├── pipeline_predict.py
│   └── visualize.py
├── outputs/
│   ├── masks/
│   ├── figures/
│   ├── logs/
│   └── predictions/
├── weights/
├── requirements.txt
└── README.md
```

## 3. 环境安装

建议 Python 3.9 + PyTorch 2.1 + CUDA 11.8。

```bash
conda create -n luna_proj python=3.9 -y
conda activate luna_proj

# 按你的 CUDA 环境安装 PyTorch，这里以 cu118 为例
pip install torch==2.1.0 torchvision==0.16.0 torchaudio==2.1.0 --index-url https://download.pytorch.org/whl/cu118

pip install -r requirements.txt
```

## 4. 数据准备

### 4.1 LUNA16（分割）

- 原始目录需要包含 `subset*/*.mhd` 和 `annotations.csv`
- 配置在 `configs/seg_config.yaml`：
  - `data.luna_root`
  - `data.annotations_csv`

执行预处理：

```bash
python datasets/preprocess_luna16.py --config configs/seg_config.yaml
```

输出：
- `processed_luna16/images/*.npy`
- `processed_luna16/masks/*.npy`
- `processed_luna16/train_list.txt`
- `processed_luna16/val_list.txt`
- `processed_luna16/test_list.txt`（按本实验设定为空）

### 4.2 LIDC-IDRI_1176（分类）

需要：
- `LIDC-IDRI_1176.npy`
- `LIDC-IDRI_1176.zip`

配置在 `configs/cls_config.yaml`：
- `data.lidc_meta_npy`
- `data.lidc_zip`

执行预处理：

```bash
python datasets/preprocess_lidc.py --config configs/cls_config.yaml
```

标签规则：
- malignancy 中位数 `1~2 -> benign(0)`
- malignancy 中位数 `4~5 -> malignant(1)`
- `3 -> 丢弃`

输出：
- `processed_lidc/benign/*.npy`
- `processed_lidc/malignant/*.npy`
- `processed_lidc/train.txt`
- `processed_lidc/val.txt`
- `processed_lidc/test.txt`（按本实验设定为空）

## 5. 模型训练

### 5.1 分割训练

```bash
python train/train_seg.py --config configs/seg_config.yaml
```

可临时改 epoch：

```bash
python train/train_seg.py --config configs/seg_config.yaml --epochs 1
```

输出：
- 权重：`weights/seg_best.pth`
- 日志：`outputs/logs/train_seg.log`
- 指标：`outputs/logs/train_seg_metrics.csv`
- 曲线：`outputs/figures/seg_loss_curve.png`、`seg_dice_curve.png`、`seg_iou_curve.png`

### 5.2 分类训练

```bash
python train/train_cls.py --config configs/cls_config.yaml --model mamba
```

可选模型：
- `mamba`
- `cnn`
- `cnn_transformer`

可临时改 epoch：

```bash
python train/train_cls.py --config configs/cls_config.yaml --model mamba --epochs 1
```

输出：
- 权重：`weights/cls_best_<model>.pth`
- 日志：`outputs/logs/train_cls.log`
- 指标：`outputs/logs/train_cls_metrics.csv`
- 曲线：`outputs/figures/cls_loss_curve_<model>.png`、`cls_acc_curve_<model>.png`、`cls_auc_curve_<model>.png`

## 6. 推理与可视化

### 6.1 分割推理

```bash
python infer/infer_seg.py \
  --config configs/seg_config.yaml \
  --ct /path/to/case.mhd \
  --ckpt weights/seg_best.pth \
  --out_npy outputs/masks/pred_mask.npy \
  --out_nii outputs/masks/pred_mask.nii.gz
```

### 6.2 分类推理

```bash
python infer/infer_cls.py \
  --config configs/cls_config.yaml \
  --roi /path/to/roi.npy \
  --ckpt weights/cls_best_mamba.pth \
  --model mamba \
  --out_txt outputs/predictions/cls_result.txt \
  --out_json outputs/predictions/cls_result.json
```

### 6.3 串联推理（推荐答辩演示）

```bash
python infer/pipeline_predict.py \
  --ct /path/to/case.mhd \
  --seg_config configs/seg_config.yaml \
  --cls_config configs/cls_config.yaml \
  --seg_ckpt weights/seg_best.pth \
  --cls_ckpt weights/cls_best_mamba.pth \
  --model mamba
```

输出：
- 分割掩码：`outputs/masks/pipeline_mask.npy`、`pipeline_mask.nii.gz`
- ROI：`outputs/predictions/pipeline_roi.npy`
- 图像：`outputs/figures/pipeline_ct_slice.png`、`pipeline_overlay.png`、`pipeline_roi.png`、`pipeline_annotated.png`
- 文本与 JSON：`outputs/predictions/pipeline_result.txt`、`pipeline_result.json`

## 7. 注意事项

1. 本版本采用 `8:2:0` 划分，`test` 列表为空占位（用于前期验证）。
2. LIDC-IDRI_1176 为 patch 数据集，不是完整原始 XML 数据流程。
3. 分类脚本默认支持类别不平衡（class weight）。
4. 推理脚本先支持单样本，便于毕设演示与论文作图。
