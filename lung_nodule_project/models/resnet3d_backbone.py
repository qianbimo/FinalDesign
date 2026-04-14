"""
3D ResNet Backbone（分类特征提取）。
"""

from __future__ import annotations

from typing import List

import torch
import torch.nn as nn


class BasicBlock3D(nn.Module):
    expansion = 1

    def __init__(self, in_channels: int, out_channels: int, stride: int = 1):
        super().__init__()
        self.conv1 = nn.Conv3d(in_channels, out_channels, kernel_size=3, stride=stride, padding=1, bias=False)
        self.bn1 = nn.BatchNorm3d(out_channels)
        self.relu = nn.ReLU(inplace=True)
        self.conv2 = nn.Conv3d(out_channels, out_channels, kernel_size=3, stride=1, padding=1, bias=False)
        self.bn2 = nn.BatchNorm3d(out_channels)

        if stride != 1 or in_channels != out_channels:
            self.downsample = nn.Sequential(
                nn.Conv3d(in_channels, out_channels, kernel_size=1, stride=stride, bias=False),
                nn.BatchNorm3d(out_channels),
            )
        else:
            self.downsample = nn.Identity()

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        identity = self.downsample(x)
        out = self.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))
        out = self.relu(out + identity)
        return out


class ResNet3DBackbone(nn.Module):
    """
    仅输出特征，不直接输出分类。
    """

    def __init__(self, depth: int = 18, in_channels: int = 1, feat_dim: int = 256, dropout: float = 0.2):
        super().__init__()
        if depth == 18:
            layers = [2, 2, 2, 2]
        elif depth == 34:
            layers = [3, 4, 6, 3]
        else:
            raise ValueError("depth 仅支持 18 或 34")

        self.inplanes = 32
        self.stem = nn.Sequential(
            nn.Conv3d(in_channels, 32, kernel_size=7, stride=2, padding=3, bias=False),
            nn.BatchNorm3d(32),
            nn.ReLU(inplace=True),
            nn.MaxPool3d(kernel_size=3, stride=2, padding=1),
        )
        self.layer1 = self._make_layer(32, layers[0], stride=1)
        self.layer2 = self._make_layer(64, layers[1], stride=2)
        self.layer3 = self._make_layer(128, layers[2], stride=2)
        self.layer4 = self._make_layer(256, layers[3], stride=2)

        self.out_channels = 256
        self.global_pool = nn.AdaptiveAvgPool3d((1, 1, 1))
        self.proj = nn.Linear(self.out_channels, int(feat_dim))
        self.drop = nn.Dropout(float(dropout))

    def _make_layer(self, out_channels: int, blocks: int, stride: int) -> nn.Sequential:
        layers: List[nn.Module] = [BasicBlock3D(self.inplanes, out_channels, stride=stride)]
        self.inplanes = out_channels
        for _ in range(1, blocks):
            layers.append(BasicBlock3D(self.inplanes, out_channels, stride=1))
        return nn.Sequential(*layers)

    def forward_feature_map(self, x: torch.Tensor) -> torch.Tensor:
        x = self.stem(x)
        x = self.layer1(x)
        x = self.layer2(x)
        x = self.layer3(x)
        x = self.layer4(x)
        return x

    def forward_pooled(self, x: torch.Tensor) -> torch.Tensor:
        fmap = self.forward_feature_map(x)
        pooled = self.global_pool(fmap).flatten(1)
        pooled = self.drop(pooled)
        return self.proj(pooled)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.forward_pooled(x)
