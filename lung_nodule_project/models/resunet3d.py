"""
轻量 3D Res-UNet。
适配单卡 11GB 显存，输出单通道分割 logits。
"""

from __future__ import annotations

import torch
import torch.nn as nn
import torch.nn.functional as F


class ResidualBlock3D(nn.Module):
    """基础残差块：Conv3d-BN-ReLU x2 + shortcut。"""

    def __init__(self, in_channels: int, out_channels: int):
        super().__init__()
        self.conv1 = nn.Conv3d(in_channels, out_channels, kernel_size=3, padding=1, bias=False)
        self.bn1 = nn.BatchNorm3d(out_channels)
        self.conv2 = nn.Conv3d(out_channels, out_channels, kernel_size=3, padding=1, bias=False)
        self.bn2 = nn.BatchNorm3d(out_channels)
        self.relu = nn.ReLU(inplace=True)
        if in_channels != out_channels:
            self.short = nn.Sequential(
                nn.Conv3d(in_channels, out_channels, kernel_size=1, bias=False),
                nn.BatchNorm3d(out_channels),
            )
        else:
            self.short = nn.Identity()

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        identity = self.short(x)
        out = self.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))
        out = self.relu(out + identity)
        return out


class DownBlock(nn.Module):
    """下采样块：MaxPool + ResidualBlock。"""

    def __init__(self, in_channels: int, out_channels: int):
        super().__init__()
        self.pool = nn.MaxPool3d(kernel_size=2, stride=2)
        self.block = ResidualBlock3D(in_channels, out_channels)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.block(self.pool(x))


class UpBlock(nn.Module):
    """上采样块：转置卷积 + skip concat + ResidualBlock。"""

    def __init__(self, in_channels: int, out_channels: int):
        super().__init__()
        self.up = nn.ConvTranspose3d(in_channels, in_channels // 2, kernel_size=2, stride=2)
        self.block = ResidualBlock3D(in_channels, out_channels)

    def forward(self, x: torch.Tensor, skip: torch.Tensor) -> torch.Tensor:
        x = self.up(x)
        dz = skip.size(2) - x.size(2)
        dy = skip.size(3) - x.size(3)
        dx = skip.size(4) - x.size(4)
        x = F.pad(x, [dx // 2, dx - dx // 2, dy // 2, dy - dy // 2, dz // 2, dz - dz // 2])
        x = torch.cat([skip, x], dim=1)
        return self.block(x)


class ResUNet3D(nn.Module):
    """
    3D Res-UNet 主体。
    输入:  [B,1,D,H,W]
    输出:  [B,1,D,H,W] (logits)
    """

    def __init__(self, in_channels: int = 1, out_channels: int = 1, base_channels: int = 16):
        super().__init__()
        c = int(base_channels)
        self.enc1 = ResidualBlock3D(in_channels, c)
        self.enc2 = DownBlock(c, c * 2)  #此处添加了一个残差块，增加了模型的表达能力，同时保持了较低的参数量和计算复杂度。
        self.enc3 = DownBlock(c * 2, c * 4)
        self.enc4 = DownBlock(c * 4, c * 8)
        self.bottom = DownBlock(c * 8, c * 16)

        self.dec4 = UpBlock(c * 16, c * 8)
        self.dec3 = UpBlock(c * 8, c * 4)
        self.dec2 = UpBlock(c * 4, c * 2)
        self.dec1 = UpBlock(c * 2, c)
        self.out_conv = nn.Conv3d(c, out_channels, kernel_size=1)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        s1 = self.enc1(x)
        s2 = self.enc2(s1)
        s3 = self.enc3(s2)
        s4 = self.enc4(s3)
        b = self.bottom(s4)

        x = self.dec4(b, s4)
        x = self.dec3(x, s3)
        x = self.dec2(x, s2)
        x = self.dec1(x, s1)
        return self.out_conv(x)
