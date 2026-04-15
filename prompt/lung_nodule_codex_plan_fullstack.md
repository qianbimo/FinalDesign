# 肺结节智能分析系统总执行计划（Token 优化中文版）

## 项目目标
开发完整的肺结节智能分析系统（毕业设计）。

系统包含：

- 患者端
- 医生端
- CT影像上传
- AI检测模块（预留接口）
- 可视化标注
- 报告单系统
- 前后端分离架构

---

## 强制要求（节省 Token）

在正式编码前，Codex 必须先自动生成以下三个文档：

1. FRONTEND_ARCH.md（前端架构说明）
2. BACKEND_ARCH.md（后端架构说明）
3. TEST_PLAN.md（测试计划说明）

后续开发过程优先引用这些文件，不要反复在聊天中重复架构内容。

---

## 技术栈

### 前端

- Vue3
- Vite
- TypeScript
- Pinia
- Vue Router
- Element Plus
- Axios

### 后端

- Spring Boot 3
- MyBatis Plus
- MySQL
- JWT
- Swagger

### AI 模块

- HTTP接口预留
- 开发阶段允许 Mock 数据

---

## 开发阶段顺序

## 第一阶段：文档生成

- [ ] 生成 FRONTEND_ARCH.md
- [ ] 生成 BACKEND_ARCH.md
- [ ] 生成 TEST_PLAN.md

## 第二阶段：后端开发

- [ ] 登录注册
- [ ] 用户权限
- [ ] 患者模块
- [ ] 医生模块
- [ ] 挂号模块
- [ ] CT上传
- [ ] 检查记录
- [ ] AI任务模块
- [ ] 报告模块
- [ ] 标注模块

## 第三阶段：前端开发

- [ ] 登录页
- [ ] 注册页
- [ ] 患者端页面
- [ ] 医生端页面
- [ ] 上传页
- [ ] AI结果页
- [ ] 报告页
- [ ] 权限路由

## 第四阶段：测试

- [ ] 执行 TEST_PLAN.md
- [ ] 修复失败项
- [ ] 重新测试
- [ ] 勾选完成项

---

## 三个架构文档要求

## FRONTEND_ARCH.md

必须包含：

- 前端目录结构
- 页面路由
- 页面清单
- 公共组件
- 状态管理
- API模块
- 权限逻辑
- 患者流程
- 医生流程

## BACKEND_ARCH.md

必须包含：

- 后端目录结构
- 数据库表结构
- 模块拆分
- Controller列表
- Service列表
- JWT流程
- 上传流程
- AI调用流程
- 报告生成流程

## TEST_PLAN.md

必须包含：

- 接口测试
- 页面测试
- 患者完整流程测试
- 医生完整流程测试
- 权限测试
- 上传测试
- AI mock测试
- 报告测试
- 打勾清单

---

## 核心接口

POST /api/auth/register
POST /api/auth/login

GET /api/patient/profile
PUT /api/patient/profile
GET /api/patient/studies

GET /api/doctor/profile
GET /api/doctor/patients
GET /api/doctor/studies

POST /api/registration
GET /api/registration/list

POST /api/upload/ct
POST /api/study/create
GET /api/study/{id}

POST /api/ai-task/start/{studyId}
GET /api/ai-task/{taskId}
GET /api/ai-task/study/{studyId}/result

GET /api/annotation/study/{studyId}

GET /api/report/{id}
PUT /api/report/{id}
POST /api/report/{id}/audit

---

## 最终交付内容

- 可运行前端项目
- 可运行后端项目
- SQL建表文件
- 三个架构文档
- 测试清单
- AI Mock流程跑通

---

## 重要说明

不要反复输出架构内容。  
架构写入 md 文件后，后续直接引用文件继续开发。

---

## Codex执行指令

请先生成三个架构文档，再按阶段开发前后端。  
每完成一个模块就测试，并勾选完成项。  
