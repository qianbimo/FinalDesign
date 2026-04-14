"""
ResNet3D + Mamba 分类器。
"""

from __future__ import annotations

from typing import Optional

import torch
import torch.nn as nn

from models.resnet3d_backbone import ResNet3DBackbone

try:
    # mamba-ssm 1.x 常见导入方式
    from mamba_ssm.modules.mamba_simple import Mamba  # type: ignore
except Exception:  # pragma: no cover
    try:
        from mamba_ssm import Mamba  # type: ignore
    except Exception:
        Mamba = None


class _MambaBlock(nn.Module):
    """Mamba block，若环境缺失则自动退化到 TransformerEncoderLayer。"""

    def __init__(self, dim: int):
        super().__init__()
        if Mamba is None:
            self.is_fallback = True
            self.block = nn.TransformerEncoderLayer(
                d_model=dim,
                nhead=8,
                dim_feedforward=dim * 4,
                dropout=0.1,
                batch_first=True,
            )
        else:
            self.is_fallback = False
            self.block = Mamba(d_model=dim, d_state=16, d_conv=4, expand=2)
        self.norm = nn.LayerNorm(dim)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        if self.is_fallback:
            out = self.block(x)
            return self.norm(out)
        out = x + self.block(x)
        return self.norm(out)


class MambaClassifier(nn.Module):
    """
    输入: [B,1,64,64,64]
    输出: [B,2] logits
    """

    def __init__(
        self,
        in_channels: int = 1,
        num_classes: int = 2,
        backbone_depth: int = 18,
        mamba_dim: int = 256,
        mamba_layers: int = 2,
        dropout: float = 0.2,
    ):
        super().__init__()
        self.backbone = ResNet3DBackbone(
            depth=backbone_depth,
            in_channels=in_channels,
            feat_dim=mamba_dim,
            dropout=dropout,
        )
        self.token_proj = nn.Linear(self.backbone.out_channels, mamba_dim)
        self.blocks = nn.ModuleList([_MambaBlock(mamba_dim) for _ in range(int(mamba_layers))])
        self.head = nn.Sequential(
            nn.Dropout(float(dropout)),
            nn.Linear(mamba_dim, num_classes),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # 先提取 3D 特征图
        fmap = self.backbone.forward_feature_map(x)  # [B,C,D,H,W]
        b, c, d, h, w = fmap.shape
        tokens = fmap.reshape(b, c, d * h * w).transpose(1, 2)  # [B,N,C]
        tokens = self.token_proj(tokens)  # [B,N,mamba_dim]

        for blk in self.blocks:
            tokens = blk(tokens)

        # 使用 token 平均池化作为全局表示
        feat = tokens.mean(dim=1)  # [B,mamba_dim]
        logits = self.head(feat)
        return logits
