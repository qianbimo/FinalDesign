# 肺结节系统前端（frontend）

`frontend` 是基于 Vue 3 的多角色前端系统，提供患者、医生、管理员的业务界面。

## 技术栈

- Vue `3.5.x`
- Vite `6.x`
- Vue Router `4.x`
- Pinia `2.x`
- Element Plus `2.x`
- Axios
- ECharts

## 目录结构

```text
frontend/
├─ src/
│  ├─ api
│  ├─ router
│  ├─ stores
│  ├─ layouts
│  ├─ views
│  └─ styles
├─ .env.development
├─ vite.config.js
├─ package.json
└─ README.md
```

## 安装与启动

在 `frontend` 目录执行：

```bash
npm install
npm run dev
```

默认访问：
- 前端地址：`http://localhost:5173`

## 构建命令

```bash
npm run build
npm run preview
```

## 后端代理

开发模式通过 Vite 代理：
- `/api` -> `http://localhost:8080`
- `/files` -> `http://localhost:8080`

可在 `.env.development` 中修改：

```env
VITE_API_TARGET=http://localhost:8080
```

## 路由与角色

- 公共页面：`/login`、`/register`
- 工作台：`/app/workspace`
- 患者：资料、检查记录、挂号申请
- 医生：患者列表、报告中心、挂号处理、病例详情
- 管理员：系统概览、用户管理、报告总览

路由守卫基于登录态和角色（`PATIENT` / `DOCTOR` / `ADMIN`）做权限控制。
