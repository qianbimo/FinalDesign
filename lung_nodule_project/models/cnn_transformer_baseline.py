"""
分类对比模型：
1) 仅 CNN
2) CNN + Transformer
"""

from __future__ import annotations

import torch
import torch.nn as nn

from models.resnet3d_backbone import ResNet3DBackbone


class CNNOnlyClassifier(nn.Module):
    """仅使用 3D CNN(ResNet3D backbone) + FC。"""

    def __init__(
        self,
        in_channels: int = 1,
        num_classes: int = 2,
        backbone_depth: int = 18,
        feat_dim: int = 256,
        dropout: float = 0.2,
    ):
        super().__init__()
        self.backbone = ResNet3DBackbone(
            depth=backbone_depth,
            in_channels=in_channels,
            feat_dim=feat_dim,
            dropout=dropout,
        )
        self.head = nn.Sequential(
            nn.Dropout(float(dropout)),
            nn.Linear(int(feat_dim), num_classes),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        feat = self.backbone.forward_pooled(x)
        return self.head(feat)


class CNNTransformerClassifier(nn.Module):
    """3D CNN 提取 token + Transformer 编码 + FC。"""

    def __init__(
        self,
        in_channels: int = 1,
        num_classes: int = 2,
        backbone_depth: int = 18,
        hidden_dim: int = 256,
        num_layers: int = 2,
        dropout: float = 0.2,
    ):
        super().__init__()
        self.backbone = ResNet3DBackbone(
            depth=backbone_depth,
            in_channels=in_channels,
            feat_dim=hidden_dim,
            dropout=dropout,
        )
        self.token_proj = nn.Linear(self.backbone.out_channels, hidden_dim)
        enc_layer = nn.TransformerEncoderLayer(
            d_model=hidden_dim,
            nhead=8,
            dim_feedforward=hidden_dim * 4,
            dropout=dropout,
            batch_first=True,
        )
        self.encoder = nn.TransformerEncoder(enc_layer, num_layers=int(num_layers))
        self.head = nn.Sequential(
            nn.Dropout(float(dropout)),
            nn.Linear(hidden_dim, num_classes),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        fmap = self.backbone.forward_feature_map(x)  # [B,C,D,H,W]
        b, c, d, h, w = fmap.shape
        tokens = fmap.reshape(b, c, d * h * w).transpose(1, 2)  # [B,N,C]
        tokens = self.token_proj(tokens)  # [B,N,H]
        tokens = self.encoder(tokens)
        feat = tokens.mean(dim=1)
        return self.head(feat)
