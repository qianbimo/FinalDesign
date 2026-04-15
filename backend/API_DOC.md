# API 文档（第一阶段）

## 统一返回结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 1）认证接口

### POST /api/auth/register

说明：
- 公开注册只允许 `PATIENT`。
- 传入 `DOCTOR/ADMIN` 会返回 `400`。

请求：

```json
{
  "username": "patient001",
  "password": "123456",
  "role": "PATIENT",
  "realName": "张三",
  "phone": "13800000000"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1
  }
}
```

### POST /api/auth/login

请求：

```json
{
  "username": "patient001",
  "password": "123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "role": "PATIENT",
    "userId": 1,
    "realName": "张三"
  }
}
```

## 2）CT 检查与上传

### POST /api/study/create

Header：`Authorization: Bearer {token}`

说明：
- `PATIENT` 角色可不传 `patientId`，后端会自动绑定当前患者档案。
- `DOCTOR/ADMIN` 角色需要传 `patientId`。

```json
{
  "patientId": 1001,
  "doctorId": 2001,
  "studyDate": "2026-04-14",
  "studyDesc": "胸部CT平扫",
  "deviceInfo": "Siemens SOMATOM"
}
```

### GET /api/study/list?current=1&size=10

### GET /api/study/{id}

### POST /api/upload/ct

- Content-Type: `multipart/form-data`
- 参数：
  - `studyId`: Long
  - `file`: `.dcm | .nii | .nii.gz`

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileId": 1,
    "studyId": 1,
    "fileName": "1001.nii.gz",
    "fileType": "NII_GZ",
    "filePath": "E:/code/FinalDesign/storage/ct/1001/ST.../20260414/xxx_1001.nii.gz",
    "fileSize": 1024,
    "accessUrl": "/files/ct/1001/ST.../20260414/xxx_1001.nii.gz"
  }
}
```

## 3）AI任务预留接口

### POST /api/ai-task/start/{studyId}

- 检查记录存在性校验
- 校验CT文件已上传
- 创建 ai_task
- 调用 AI 推理客户端 `AiInferenceClient`
- 保存 nodule_result 与 annotation_result
- 自动生成 report_record 草稿

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 3001
  }
}
```

### GET /api/ai-task/{taskId}

### GET /api/ai-task/study/{studyId}/result

返回：任务信息 + 结节列表 + 标注列表 + 最新报告。

## 4）患者接口

- GET `/api/patient/profile`
- PUT `/api/patient/profile`
- GET `/api/patient/studies`
- GET `/api/patient/studies/{studyId}`
- GET `/api/patient/studies/{studyId}/ai-result`
- GET `/api/patient/studies/{studyId}/report`

## 5）医生接口

- GET `/api/doctor/profile`
- GET `/api/doctor/patients`
- GET `/api/doctor/studies`
- GET `/api/doctor/patient/{patientId}/studies/{studyId}`

## 6）挂号接口

- POST `/api/registration`
- GET `/api/registration/list`
- PUT `/api/registration/{id}/status`

## 7）报告接口

- GET `/api/report/{id}`
- GET `/api/report/study/{studyId}`
- PUT `/api/report/{reportId}`
- POST `/api/report/{reportId}/audit`

## 8）标注接口

- GET `/api/annotation/study/{studyId}`

## 9）管理员接口

- 权限：`ADMIN`
- GET `/api/admin/users?current=1&size=10&role=&status=&keyword=`
- POST `/api/admin/users`
- PUT `/api/admin/users/{id}/status`
- PUT `/api/admin/users/{id}/reset-password`
- GET `/api/admin/dashboard`

