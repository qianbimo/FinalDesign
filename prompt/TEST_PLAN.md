# TEST_PLAN

## 1. 测试范围

- 前端页面与路由
- 后端 REST API
- 鉴权与权限
- CT 上传
- AI Mock 联调
- 报告生成与审核

## 2. 测试环境

- 前端：`frontend`（Vue3 + Vite）
- 后端：`backend`（Spring Boot 3）
- 数据库：MySQL 8.x，库名 `lung_nodule`
- 文件存储：`E:/code/FinalDesign/storage`
- 文档：`http://localhost:8080/doc.html`

## 3. 接口测试

### 3.1 认证

- [ ] `POST /api/auth/register` 注册成功
- [ ] `POST /api/auth/login` 登录成功并返回 token
- [ ] 非法账号密码返回错误

### 3.2 患者

- [ ] `GET /api/patient/profile`
- [ ] `PUT /api/patient/profile`
- [ ] `GET /api/patient/studies`
- [ ] `GET /api/patient/studies/{studyId}`
- [ ] `GET /api/patient/studies/{studyId}/ai-result`
- [ ] `GET /api/patient/studies/{studyId}/report`

### 3.3 医生

- [ ] `GET /api/doctor/profile`
- [ ] `GET /api/doctor/patients`
- [ ] `GET /api/doctor/studies`
- [ ] `GET /api/doctor/patient/{patientId}/studies/{studyId}`

### 3.4 挂号

- [ ] `POST /api/registration`
- [ ] `GET /api/registration/list`
- [ ] `PUT /api/registration/{id}/status`

### 3.5 检查与上传

- [ ] `POST /api/study/create`
- [ ] `GET /api/study/list`
- [ ] `GET /api/study/{id}`
- [ ] `POST /api/upload/ct`（multipart）
- [ ] `GET /api/ct-file/study/{studyId}`

### 3.6 AI 与标注

- [ ] `POST /api/ai-task/start/{studyId}`
- [ ] `GET /api/ai-task/{taskId}`
- [ ] `GET /api/ai-task/study/{studyId}/result`
- [ ] `GET /api/annotation/study/{studyId}`

### 3.7 报告

- [ ] `GET /api/report/study/{studyId}`
- [ ] `PUT /api/report/{reportId}`
- [ ] `POST /api/report/{reportId}/audit`

## 4. 页面测试

- [ ] 登录页：表单校验、登录跳转
- [ ] 注册页：角色选择与注册结果提示
- [ ] 患者端页面：资料、挂号、上传、检查详情
- [ ] 医生端页面：患者列表、病例详情、报告编辑
- [ ] 管理员页面：用户管理与统计展示
- [ ] 无权限路由访问跳转行为

## 5. 患者完整流程测试

- [ ] 患者注册
- [ ] 患者登录
- [ ] 完善资料
- [ ] 发起挂号
- [ ] 创建检查记录
- [ ] 上传 CT
- [ ] 查看 AI 结果
- [ ] 查看报告

## 6. 医生完整流程测试

- [ ] 医生注册/登录
- [ ] 查看挂号列表
- [ ] 查看病例
- [ ] 启动 AI 任务
- [ ] 查看标注数据
- [ ] 编辑报告
- [ ] 审核报告

## 7. 权限测试

- [ ] 未登录访问 `/api/**` 受保护接口返回 401
- [ ] 患者访问医生接口返回 403
- [ ] 医生访问管理员接口返回 403
- [ ] 管理员可访问管理接口

## 8. 上传测试

- [ ] 允许 `.dcm`、`.nii`、`.nii.gz`
- [ ] 非法扩展名被拒绝
- [ ] 超过大小限制被拒绝
- [ ] 文件实际落盘路径与返回路径一致

## 9. AI Mock 测试

- [ ] `ai.mock-enabled=true` 时可完成全链路
- [ ] `ai_task` 状态从 WAITING -> RUNNING -> SUCCESS
- [ ] 生成 `nodule_result` 与 `annotation_result`
- [ ] 自动生成 `report_record` 草稿

## 10. 报告测试

- [ ] 草稿生成包含结节统计信息
- [ ] 医生可更新报告内容
- [ ] 审核后状态从 DRAFT -> REVIEWED
- [ ] 版本号可持续增长

## 11. 打勾清单（当前仓库实测）

- [x] 后端编译成功
- [x] 接口文档可访问（`/doc.html`）
- [x] 注册 -> 登录 -> 挂号 -> 上传 -> AI 结果全链路跑通
- [x] AI Mock 返回结果可入库并生成报告

## 12. 回归建议

- 每次后端改动后至少执行：
  1. 认证接口回归
  2. 上传接口回归
  3. AI 任务回归
  4. 报告读写回归
- 每次前端改动后至少执行：
  1. 登录态恢复
  2. 路由权限校验
  3. 患者端关键流程
  4. 医生端关键流程
