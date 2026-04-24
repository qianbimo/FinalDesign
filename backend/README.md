# 肺结节系统后端（backend）

`backend` 是系统业务后端，负责用户权限、检查管理、文件上传、AI任务调用与报告流转。

## 技术栈

- Spring Boot `3.3.4`
- Java `17`
- Spring Security + JWT
- MyBatis-Plus `3.5.7`
- MySQL
- Knife4j / OpenAPI 3

## 目录结构

```text
backend/
├─ src/main/java/com/finaldesign/lungnodule
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ security
│  ├─ entity / dto / vo
│  └─ config / common / utils
├─ src/main/resources
│  ├─ application.yml
│  └─ sql/lung_nodule_schema.sql
├─ pom.xml
└─ README.md
```

## 启动前准备

1. 创建 MySQL 数据库：`lung_nodule`
2. 执行建表脚本：`src/main/resources/sql/lung_nodule_schema.sql`
3. 修改配置文件中的数据库账号密码：
   - `src/main/resources/application.yml`

## 启动方式

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

或在仓库根目录执行：

```bat
start-backend.bat
```

## 关键配置

- 服务端口：`8080`
- 数据库连接：`spring.datasource.*`
- 文件存储根目录：`storage.base-path`（默认 `E:/code/FinalDesign/storage`）
- AI 服务地址：
  - `ai.base-url: http://localhost:8000`
  - `ai.predict-path: /api/inference/predict`
- Mock 开关：`ai.mock-enabled`

## 常用接口

- 认证：`/api/auth/register`、`/api/auth/login`
- 检查记录：`/api/study/*`
- 文件上传：`/api/upload/ct`
- AI任务：`/api/ai-task/*`
- 报告管理：`/api/report/*`
- 管理员：`/api/admin/*`

完整接口示例见 [API_DOC.md](E:/code/FinalDesign/backend/API_DOC.md)。

## 文档地址

- Knife4j：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
