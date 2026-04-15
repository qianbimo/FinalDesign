# FRONTEND_ARCH

## 1. 前端目录结构

```text
frontend/
├── src/
│   ├── api/
│   │   ├── auth.js
│   │   ├── patient.js
│   │   ├── doctor.js
│   │   ├── registration.js
│   │   ├── study.js
│   │   ├── upload.js
│   │   ├── aiTask.js
│   │   ├── annotation.js
│   │   ├── report.js
│   │   └── admin.js
│   ├── layouts/
│   │   └── MainLayout.vue
│   ├── router/
│   │   └── index.js
│   ├── stores/
│   │   └── auth.js
│   ├── utils/
│   │   └── request.js
│   ├── views/
│   │   ├── LoginView.vue
│   │   ├── RegisterView.vue
│   │   ├── WorkspaceView.vue
│   │   ├── patient/*
│   │   ├── doctor/*
│   │   └── admin/*
│   ├── App.vue
│   └── main.js
└── vite.config.js
```

## 2. 页面路由

- 公共路由：`/login`、`/register`
- 业务入口：`/app/workspace`
- 患者路由：
  - `/app/patient/profile`
  - `/app/patient/studies`
  - `/app/patient/studies/:studyId`
  - `/app/patient/registration`
  - `/app/patient/upload`
- 医生路由：
  - `/app/doctor/profile`
  - `/app/doctor/patients`
  - `/app/doctor/studies`
  - `/app/doctor/studies/:patientId/:studyId`
  - `/app/doctor/reports`
  - `/app/doctor/annotations`
- 管理员路由：
  - `/app/admin/dashboard`
  - `/app/admin/users`

## 3. 页面清单

- 认证：登录、注册
- 通用：工作台、主布局
- 患者端：资料、检查记录、检查详情、挂号、CT上传
- 医生端：资料、患者列表、病例列表、病例详情、报告中心、标注查看
- 管理员端：用户管理、概览统计

## 4. 公共组件与基础设施

- 布局组件：`MainLayout.vue`
- 请求封装：`utils/request.js`（Axios 实例、鉴权头注入、统一错误处理）
- 全局样式：`styles/global.css`

## 5. 状态管理

- 使用 Pinia `auth` store 管理：
  - `token`
  - `role`
  - `userId`
  - `realName`
- 持久化到 `localStorage`，刷新后自动恢复登录态

## 6. API 模块划分

- `auth.js`：注册、登录
- `patient.js`：患者资料/检查/AI结果/报告
- `doctor.js`：医生资料/患者/病例
- `registration.js`：挂号申请、挂号列表、挂号状态更新
- `study.js`：检查创建、检查列表/详情、CT文件列表
- `upload.js`：CT文件上传
- `aiTask.js`：启动AI任务、任务状态、检查AI结果
- `annotation.js`：检查标注信息
- `report.js`：报告查询、编辑、审核
- `admin.js`：后台管理相关接口

## 7. 权限逻辑

- 登录前仅允许访问 `/login`、`/register`
- 所有 `/app/**` 路由要求登录
- 基于 `meta.roles` 做前端路由级 RBAC：
  - `PATIENT`
  - `DOCTOR`
  - `ADMIN`
- 无权限时回退到 `/app/workspace`

## 8. 患者流程

1. 注册与登录
2. 完善个人资料
3. 发起挂号申请
4. 创建检查记录
5. 上传 CT 文件
6. 查看检查记录、AI结果、报告

## 9. 医生流程

1. 登录
2. 查看挂号列表与患者列表
3. 查看病例详情
4. 启动 AI 分析任务
5. 查看标注结果和报告草稿
6. 编辑与审核报告

## 10. 与后端联调约定

- 统一响应结构：`{ code, message, data }`
- 统一分页结构：`{ total, current, size, records }`
- Token Header：`Authorization: Bearer <token>`
- CT 上传使用 `multipart/form-data`
