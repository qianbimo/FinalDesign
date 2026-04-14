# 肺结节智能分析系统后端（基础版）

## 1. 项目目录结构

```text
backend
├── pom.xml
├── src/main/java/com/finaldesign/lungnodule
│   ├── LungNoduleBackendApplication.java
│   ├── common
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── mapper
│   ├── security
│   ├── service
│   │   └── impl
│   ├── utils
│   └── vo
└── src/main/resources
    ├── application.yml
    └── sql/lung_nodule_schema.sql
```

## 2. 技术栈

- Spring Boot 3
- Spring Security + JWT
- MyBatis-Plus
- MySQL
- Knife4j / OpenAPI3

## 3. 快速启动

1. 创建数据库并执行脚本：
   - `src/main/resources/sql/lung_nodule_schema.sql`
2. 修改 `application.yml` 数据库账号密码。
3. 启动服务：
   - `mvn spring-boot:run`
4. 文档地址：
   - `http://localhost:8080/doc.html`

## 4. 存储目录约定

- `storage/ct/{patientId}/{studyNo}/{yyyyMMdd}/...`
- `storage/result/{studyNo}/...`（AI返回路径预留）
- `storage/overlay/{studyNo}/...`（AI返回路径预留）

## 5. 已完成核心能力

- 登录注册 + JWT鉴权 + RBAC（PATIENT/DOCTOR/ADMIN）
- 患者/医生基础资料查询
- 挂号管理
- CT检查记录创建与分页查询
- CT文件上传（格式校验 + 本地存储 + 路径落库）
- AI任务预留接口（HTTP调用Python服务，支持mock开关）
- AI结果落库（ai_task / nodule_result / annotation_result）
- 报告自动生成草稿（report_record）
- 报告编辑与审核
- 标注数据查询接口

## 6. AI 接口说明

- 默认通过 `ai.mock-enabled: true` 返回模拟结果（仅联调流程）
- 关闭 mock 后将调用：
  - `POST {ai.base-url}{ai.predict-path}`

## 7. 管理员账号初始化（联调必读）

- 公开注册接口 `/api/auth/register` 只允许创建 `PATIENT`。
- `DOCTOR` / `ADMIN` 账号需由管理员后台创建，或通过数据库脚本初始化。
- 若当前库没有管理员，可先注册一个患者，再在数据库执行：

```sql
UPDATE sys_user SET role = 'ADMIN' WHERE username = '你的用户名';
```

