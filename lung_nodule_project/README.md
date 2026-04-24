# 肺结节算法模块（lung_nodule_project）

该模块用于肺结节任务的训练与推理，包含：
- 分割：3D Res-UNet
- 分类：ResNet3D + Mamba（以及对比模型）
- 推理：提供后端可调用的 AI 推理服务

## 目录结构

```text
lung_nodule_project/
├─ configs/
├─ datasets/
├─ models/
├─ train/
├─ infer/
├─ utils/
├─ workspace/         # 训练与实验产物
├─ requirements.txt
└─ run_training.sh
```

## 环境安装

建议 Python `3.9`，并使用独立 Conda 环境：

```bash
conda create -n luna16_dl python=3.9 -y
conda activate luna16_dl
pip install -r requirements.txt
```

> 若需 GPU，请根据本机 CUDA 版本安装对应 PyTorch。

## 数据与配置

- 分割配置：`configs/seg_config.yaml`
- 分类配置：`configs/cls_config.yaml`
- 快速冒烟配置：`configs/seg_smoke.yaml`、`configs/cls_smoke.yaml`

请先在配置文件中检查数据路径、输出路径和训练参数。

## 训练命令

### 分割训练

```bash
python train/train_seg.py --config configs/seg_config.yaml --epochs 100
```

### 分类训练（Mamba）

```bash
python train/train_cls.py --config configs/cls_config.yaml --model mamba --epochs 100
```

可选模型：
- `mamba`
- `cnn`
- `cnn_transformer`

### 一体化脚本

```bash
bash run_training.sh --mode full --preset full --model mamba
```

## 推理服务（给后端调用）

在 `lung_nodule_project` 目录启动：

```bash
python -m infer.ai_inference_server --host 127.0.0.1 --port 8000
```

默认接口：
- `POST /api/inference/predict`

## 结果输出

训练/实验结果默认写入：

```text
workspace/runs/<run_name_timestamp>/
```

典型内容：
- `stdout.log`
- `metrics.csv`
- `weights/*.pth`
- 曲线图与可视化结果
