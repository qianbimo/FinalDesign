# 肺结节智能分析系统（FinalDesign）

本仓库包含完整的肺结节智能分析系统，分为三个核心板块：
- `backend`：Spring Boot 后端服务（用户、检查、报告、AI任务管理）
- `frontend`：Vue 3 前端系统（患者/医生/管理员多角色界面）
- `lung_nodule_project`：Python 深度学习模块（分割、分类、推理服务）

## 目录结构

```text
FinalDesign/
├─ backend/
├─ frontend/
├─ lung_nodule_project/
├─ storage/
├─ start-ai.bat
├─ start-backend.bat
├─ start-frontend.bat
└─ start-all.bat
```

## 环境要求

- Java 17+
- Maven 3.9+
- Node.js 18+（含 npm）
- Python 3.9+（建议 Conda 环境）
- MySQL 8.x

## 一键启动（Windows）

在仓库根目录执行：

```bat
start-all.bat
```

默认服务地址：
- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- AI 推理服务：`http://localhost:8000`

## 分模块启动

```bat
start-ai.bat
start-backend.bat
start-frontend.bat
```

## 联调顺序建议

1. 启动 MySQL，并导入后端建表脚本。
2. 启动 `lung_nodule_project` 推理服务（端口 `8000`）。
3. 启动后端（端口 `8080`）。
4. 启动前端（端口 `5173`）。
5. 前端登录后走上传 CT -> 发起 AI 任务 -> 查看报告流程。

## 说明

- 你提到的 `lung_noudle_project` 在仓库中实际目录名为 `lung_nodule_project`。
- 各模块详细说明见：
  - [backend/README.md](E:/code/FinalDesign/backend/README.md)
  - [frontend/README.md](E:/code/FinalDesign/frontend/README.md)
  - [lung_nodule_project/README.md](E:/code/FinalDesign/lung_nodule_project/README.md)
