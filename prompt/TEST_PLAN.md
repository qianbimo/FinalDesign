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
- 回归时间：`2026-04-15`

## 3. 接口测试

### 3.1 认证

- [x] `POST /api/auth/register` 注册成功
- [x] `POST /api/auth/login` 登录成功并返回 token
- [x] 非法账号密码返回错误

### 3.2 患者

- [x] `GET /api/patient/profile`
- [x] `PUT /api/patient/profile`
- [x] `GET /api/patient/studies`
- [x] `GET /api/patient/studies/{studyId}`
- [x] `GET /api/patient/studies/{studyId}/ai-result`
- [x] `GET /api/patient/studies/{studyId}/report`

### 3.3 医生

- [x] `GET /api/doctor/profile`
- [x] `GET /api/doctor/patients`
- [x] `GET /api/doctor/studies`
- [x] `GET /api/doctor/patient/{patientId}/studies/{studyId}`

### 3.4 挂号

- [x] `POST /api/registration`
- [x] `GET /api/registration/list`
- [x] `PUT /api/registration/{id}/status`

### 3.5 检查与上传

- [x] `POST /api/study/create`
- [x] `GET /api/study/list`
- [x] `GET /api/study/{id}`
- [x] `POST /api/upload/ct`（multipart）
- [x] `GET /api/ct-file/study/{studyId}`

### 3.6 AI 与标注

- [x] `POST /api/ai-task/start/{studyId}`
- [x] `GET /api/ai-task/{taskId}`
- [x] `GET /api/ai-task/study/{studyId}/result`
- [x] `GET /api/annotation/study/{studyId}`

### 3.7 报告

- [x] `GET /api/report/study/{studyId}`
- [x] `GET /api/report/{reportId}`
- [x] `PUT /api/report/{reportId}`
- [x] `POST /api/report/{reportId}/audit`

## 4. 页面测试（联调层）

- [x] 登录页：表单校验、登录跳转
- [x] 注册页：注册提示与登录跳转
- [x] 患者端页面：资料、挂号、创建检查、上传、检查详情
- [x] 医生端页面：患者列表、病例详情、报告编辑
- [x] 管理员页面：用户管理与统计展示
- [x] 无权限路由访问跳转行为

## 5. 患者完整流程测试

- [x] 患者注册
- [x] 患者登录
- [x] 完善资料
- [x] 发起挂号
- [x] 创建检查记录
- [x] 上传 CT
- [x] 查看 AI 结果
- [x] 查看报告

## 6. 医生完整流程测试

- [x] 医生登录
- [x] 查看挂号列表
- [x] 查看病例
- [x] 启动 AI 任务
- [x] 查看标注数据
- [x] 编辑报告
- [x] 审核报告

## 7. 权限测试

- [x] 未登录访问 `/api/**` 受保护接口返回 401
- [x] 患者访问医生接口返回 403（`Result.code=403`）
- [x] 医生访问管理员接口返回 403（`Result.code=403`）
- [x] 管理员可访问管理接口
- [x] 其他患者访问当前患者检查详情返回 403（`Result.code=403`）

## 8. 上传测试

- [x] 允许 `.dcm`、`.nii`、`.nii.gz`
- [x] 非法扩展名被拒绝
- [x] 超过大小限制被拒绝
- [x] 文件实际落盘路径与返回路径一致

## 9. AI Mock 测试

- [x] `ai.mock-enabled=true`/AI 服务 mock 模式可完成全链路
- [x] `ai_task` 进入成功状态（`SUCCESS`）
- [x] 生成 `nodule_result` 与 `annotation_result`
- [x] 自动生成 `report_record` 草稿

## 10. 报告测试

- [x] 草稿生成包含结节统计信息（`Nodule count` 等字段）
- [x] 医生可更新报告内容
- [x] 审核后状态从 `DRAFT -> REVIEWED`
- [x] 版本号可持续增长（同一 study 连续两次 AI 生成后 `version_no=2`）

## 11. 打勾清单（当前仓库实测）

- [x] 后端编译成功（Mavenw `clean compile`）
- [x] 前端构建成功（Vite `build`）
- [x] 接口文档可访问（`/doc.html`）
- [x] 注册 -> 登录 -> 挂号 -> 上传 -> AI 结果全链路跑通
- [x] AI 结果可入库并生成报告
- [x] 角色权限回归通过（患者/医生/管理员）

## 12. 本次关键回归样本

- 回归用患者：`qa_patient`
- 回归用医生：`qa_doctor`
- 回归用管理员：`qa_admin`
- 本次完整链路样本：
  - `studyId=2044334104641851394`
  - `aiTaskId=2044334106403459073`
  - `reportId=2044334108232175618`
- 报告模板修复后验证样本：
  - `studyId=2044337159009476609`
  - `aiTaskId=2044337162855653378`
  - `reportId=2044337164625649666`

## 13. 说明

- 系统统一返回体为 `Result<T>`，部分权限拒绝走业务返回：HTTP 200 + `code=403`。
- 未登录鉴权拦截仍为 HTTP 401。

## 14. 报告总览专项测试（管理员）

### 14.1 启动可用性

- [x] `T14-01` `start-backend.bat` 可正常启动后端，`8080` 端口可监听
- [x] `T14-02` `start-frontend.bat` 可正常启动前端，`5173` 端口可监听

### 14.2 后端接口（`/api/admin/reports`）

- [x] `T14-03` 管理员账号登录成功并拿到 token
- [x] `T14-04` `GET /api/admin/reports` 默认分页返回 `records/total/current/size`
- [x] `T14-05` `status=DRAFT` 过滤生效
- [x] `T14-06` `keyword`（标题/摘要）过滤生效
- [x] `T14-07` 患者账号访问 `/api/admin/reports` 被拒绝（`code=403` 或 HTTP 403）
- [x] `T14-08` 列表项包含关键字段：`studyNo`、`patientName`、`doctorName`、`versionNo`、`status`

### 14.3 前端联调（管理员“报告总览”）

- [x] `T14-09` 管理员菜单显示“报告总览”
- [x] `T14-10` 路由 `/app/admin/reports` 可访问并成功拉取列表
- [x] `T14-11` 点击“查看”可拉取并展示报告详情

### 14.4 本轮实测记录（2026-04-15）

- 管理员登录：`qa_admin / 123456`
- 患者登录：`qa_patient / 123456`
- 报告总数：`11`（默认分页返回 `10` 条）
- 状态过滤：`status=DRAFT` 返回 `8` 条且状态全部为 `DRAFT`
- 关键字过滤：关键字 `Ch` 返回 `1` 条且命中标题/摘要
- 越权验证：患者访问管理员报告接口返回 `code=403`
- 详情验证样本：`reportId=2044337164625649666`，`GET /api/report/{reportId}` 返回成功
