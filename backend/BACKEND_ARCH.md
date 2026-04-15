# BACKEND_ARCH

## 1. 后端目录结构

```text
backend/
├── src/main/java/com/finaldesign/lungnodule/
│   ├── common/          # Result、PageResult、状态码
│   ├── config/          # MyBatis、OpenAPI、存储、HTTP客户端等配置
│   ├── controller/      # REST 接口层
│   ├── dto/             # 请求/响应 DTO
│   ├── entity/          # 数据库实体
│   ├── enums/           # 业务状态枚举
│   ├── exception/       # 自定义异常、全局异常处理
│   ├── mapper/          # MyBatis-Plus Mapper
│   ├── security/        # JWT、安全过滤器、权限入口
│   ├── service/
│   │   └── impl/        # 业务实现
│   ├── utils/           # 编号生成、JSON处理、文件类型工具
│   └── vo/              # 视图对象
└── src/main/resources/
    ├── application.yml
    └── sql/lung_nodule_schema.sql
```

## 2. 数据库表结构

核心表（11 张）：

1. `sys_user`
2. `patient_profile`
3. `doctor_profile`
4. `registration_record`
5. `ct_study`
6. `ct_file`
7. `ai_task`
8. `nodule_result`
9. `annotation_result`
10. `report_record`
11. `operation_log`

主要关系：

- `sys_user` 1-1 `patient_profile`
- `sys_user` 1-1 `doctor_profile`
- `patient_profile` 1-N `ct_study`
- `doctor_profile` 1-N `ct_study`
- `ct_study` 1-N `ct_file`
- `ct_study` 1-N `ai_task`
- `ai_task` 1-N `nodule_result`
- `nodule_result` 1-N `annotation_result`
- `ct_study` 1-N `report_record`

## 3. 模块拆分

- 认证与权限：登录、注册、JWT、RBAC
- 患者模块：资料维护、历史检查、结果查看
- 医生模块：患者管理、病例查看、报告审核
- 挂号模块：申请、列表、状态更新
- 检查与文件模块：检查记录、CT上传、文件索引
- AI任务模块：任务创建、推理调用预留、结果入库
- 标注模块：三视图标注查询
- 报告模块：自动生成草稿、编辑、审核
- 管理员模块：用户管理、统计信息

## 4. Controller 列表

- `AuthController` -> `/api/auth`
- `PatientController` -> `/api/patient`
- `DoctorController` -> `/api/doctor`
- `RegistrationController` -> `/api/registration`
- `CtStudyController` -> `/api/study`
- `CtFileController` -> `/api/ct-file`
- `UploadController` -> `/api/upload`
- `AiTaskController` -> `/api/ai-task`
- `AnnotationController` -> `/api/annotation`
- `ReportController` -> `/api/report`
- `AdminController` -> `/api/admin`

## 5. Service 列表

- `AuthService`
- `PatientService`
- `DoctorService`
- `RegistrationService`
- `CtStudyService`
- `UploadService`
- `AiTaskService`
- `AiInferenceClient`（Python 推理服务客户端）
- `AnnotationService`
- `ReportService`
- `ReportGenerateService`
- `AdminService`

## 6. JWT 鉴权流程

1. 用户调用 `/api/auth/login`
2. 服务端校验账号密码
3. 生成 JWT（含 `userId`、`role`）
4. 前端在后续请求带上 `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` 解析 token，写入 `SecurityContext`
6. 控制器通过 `@PreAuthorize` + 角色进行访问控制

## 7. 上传流程

1. 创建检查记录：`POST /api/study/create`
2. 上传 CT 文件：`POST /api/upload/ct`
3. 校验扩展名与大小（`.dcm/.nii/.nii.gz`）
4. 按规则落盘：`storage/ct/{patientId}/{studyNo}/{yyyyMMdd}/`
5. 写入 `ct_file`，并返回可访问 URL

## 8. AI 调用流程（预留）

1. 医生调用 `POST /api/ai-task/start/{studyId}`
2. 校验检查记录和 CT 文件
3. 创建 `ai_task`（WAITING -> RUNNING）
4. 通过 `AiInferenceClient` 调用 Python 服务（或 Mock）
5. 保存 `ai_task.response_json`
6. 解析并写入 `nodule_result`、`annotation_result`
7. 生成报告草稿（`report_record`）
8. 更新任务状态与检查状态

## 9. 报告生成流程

1. 从 AI 返回结果计算：结节数量、最大直径、最高风险概率
2. 生成结构化报告正文与摘要
3. 保存为 `DRAFT`，`generated_by=SYSTEM`
4. 医生可通过接口编辑并审核
5. 审核后状态更新为 `REVIEWED`

## 10. 核心接口（当前实现）

- 认证：
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- 患者：
  - `GET/PUT /api/patient/profile`
  - `GET /api/patient/studies`
  - `GET /api/patient/studies/{studyId}`
  - `GET /api/patient/studies/{studyId}/ai-result`
  - `GET /api/patient/studies/{studyId}/report`
- 医生：
  - `GET /api/doctor/profile`
  - `GET /api/doctor/patients`
  - `GET /api/doctor/studies`
  - `GET /api/doctor/patient/{patientId}/studies/{studyId}`
- 挂号：
  - `POST /api/registration`
  - `GET /api/registration/list`
  - `PUT /api/registration/{id}/status`
- 检查/上传：
  - `POST /api/study/create`
  - `GET /api/study/list`
  - `GET /api/study/{id}`
  - `POST /api/upload/ct`
  - `GET /api/ct-file/study/{studyId}`
- AI/标注/报告：
  - `POST /api/ai-task/start/{studyId}`
  - `GET /api/ai-task/{taskId}`
  - `GET /api/ai-task/study/{studyId}/result`
  - `GET /api/annotation/study/{studyId}`
  - `GET /api/report/study/{studyId}`
  - `PUT /api/report/{reportId}`
  - `POST /api/report/{reportId}/audit`

## 11. 文档与调试

- OpenAPI：`/v3/api-docs`
- Knife4j：`/doc.html`
- Swagger UI：`/swagger-ui/index.html`
