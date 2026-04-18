CREATE DATABASE IF NOT EXISTS lung_nodule DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lung_nodule;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS report_record;
DROP TABLE IF EXISTS annotation_result;
DROP TABLE IF EXISTS nodule_result;
DROP TABLE IF EXISTS ai_task;
DROP TABLE IF EXISTS ct_file;
DROP TABLE IF EXISTS ct_study;
DROP TABLE IF EXISTS registration_record;
DROP TABLE IF EXISTS doctor_profile;
DROP TABLE IF EXISTS patient_profile;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    role VARCHAR(20) NOT NULL COMMENT '角色: PATIENT/DOCTOR/ADMIN',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE patient_profile (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    gender VARCHAR(10) NULL COMMENT '性别',
    age INT NULL COMMENT '年龄',
    birthday DATE NULL COMMENT '生日',
    id_card VARCHAR(32) NULL COMMENT '身份证号',
    medical_record_no VARCHAR(64) NULL COMMENT '病历号',
    address VARCHAR(255) NULL COMMENT '地址',
    allergy_history TEXT NULL COMMENT '过敏史',
    past_history TEXT NULL COMMENT '既往史',
    family_history TEXT NULL COMMENT '家族史',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_user_id (user_id),
    UNIQUE KEY uk_patient_medical_record_no (medical_record_no),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';

CREATE TABLE doctor_profile (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    department VARCHAR(100) NULL COMMENT '科室',
    title VARCHAR(100) NULL COMMENT '职称',
    specialty VARCHAR(255) NULL COMMENT '擅长领域',
    license_no VARCHAR(100) NULL COMMENT '执业证号',
    introduction TEXT NULL COMMENT '个人简介',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_user_id (user_id),
    UNIQUE KEY uk_doctor_license_no (license_no),
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

CREATE TABLE registration_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者ID(patient_profile.id)',
    doctor_id BIGINT NOT NULL COMMENT '医生ID(doctor_profile.id)',
    appointment_time DATETIME NOT NULL COMMENT '预约时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/CANCELLED/FINISHED',
    description VARCHAR(500) NULL COMMENT '挂号描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_registration_patient_id (patient_id),
    KEY idx_registration_doctor_id (doctor_id),
    KEY idx_registration_status (status),
    CONSTRAINT fk_registration_patient FOREIGN KEY (patient_id) REFERENCES patient_profile(id),
    CONSTRAINT fk_registration_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号记录表';

CREATE TABLE ct_study (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_no VARCHAR(64) NOT NULL COMMENT '检查编号',
    registration_id BIGINT NULL COMMENT '挂号单ID(registration_record.id)',
    patient_id BIGINT NOT NULL COMMENT '患者ID(patient_profile.id)',
    doctor_id BIGINT NULL COMMENT '医生ID(doctor_profile.id)',
    study_date DATE NULL COMMENT '检查日期',
    study_desc VARCHAR(500) NULL COMMENT '检查描述',
    device_info VARCHAR(255) NULL COMMENT '设备信息',
    status VARCHAR(20) NOT NULL DEFAULT 'WAIT_UPLOAD' COMMENT '状态: WAIT_UPLOAD/UPLOADED/PREPROCESSING/ANALYZING/FINISHED/FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ct_study_study_no (study_no),
    UNIQUE KEY uk_ct_study_registration_id (registration_id),
    KEY idx_ct_study_registration_id (registration_id),
    KEY idx_ct_study_patient_id (patient_id),
    KEY idx_ct_study_doctor_id (doctor_id),
    KEY idx_ct_study_status (status),
    CONSTRAINT fk_ct_study_registration FOREIGN KEY (registration_id) REFERENCES registration_record(id),
    CONSTRAINT fk_ct_study_patient FOREIGN KEY (patient_id) REFERENCES patient_profile(id),
    CONSTRAINT fk_ct_study_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CT检查记录表';

CREATE TABLE ct_file (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_id BIGINT NOT NULL COMMENT '检查ID(ct_study.id)',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型: DCM/NII/NII_GZ/MHD/RAW/PNG/JPG',
    file_path VARCHAR(500) NOT NULL COMMENT '本地文件路径',
    file_size BIGINT NULL COMMENT '文件大小(byte)',
    slice_count INT NULL COMMENT '切片数量',
    upload_user_id BIGINT NOT NULL COMMENT '上传用户ID(sys_user.id)',
    upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    check_status VARCHAR(20) NULL COMMENT '校验状态',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ct_file_study_id (study_id),
    KEY idx_ct_file_upload_user_id (upload_user_id),
    CONSTRAINT fk_ct_file_study FOREIGN KEY (study_id) REFERENCES ct_study(id),
    CONSTRAINT fk_ct_file_upload_user FOREIGN KEY (upload_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CT文件表';

CREATE TABLE ai_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_id BIGINT NOT NULL COMMENT '检查ID(ct_study.id)',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    model_version VARCHAR(100) NULL COMMENT '模型版本',
    task_status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '任务状态: WAITING/RUNNING/SUCCESS/FAILED',
    request_json JSON NULL COMMENT '请求参数JSON',
    response_json JSON NULL COMMENT '响应结果JSON',
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_task_task_no (task_no),
    KEY idx_ai_task_study_id (study_id),
    KEY idx_ai_task_task_status (task_status),
    CONSTRAINT fk_ai_task_study FOREIGN KEY (study_id) REFERENCES ct_study(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析任务表';

CREATE TABLE nodule_result (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_id BIGINT NOT NULL COMMENT '检查ID(ct_study.id)',
    ai_task_id BIGINT NOT NULL COMMENT 'AI任务ID(ai_task.id)',
    nodule_no INT NOT NULL COMMENT '结节编号',
    center_x DOUBLE NULL COMMENT '中心点X',
    center_y DOUBLE NULL COMMENT '中心点Y',
    center_z DOUBLE NULL COMMENT '中心点Z',
    width DOUBLE NULL COMMENT '宽度',
    height DOUBLE NULL COMMENT '高度',
    depth DOUBLE NULL COMMENT '深度',
    volume DOUBLE NULL COMMENT '体积',
    diameter_mm DOUBLE NULL COMMENT '直径(mm)',
    malignancy_prob DOUBLE NULL COMMENT '恶性概率',
    risk_level VARCHAR(20) NULL COMMENT '风险等级: LOW/MEDIUM/HIGH',
    description VARCHAR(500) NULL COMMENT '结节描述',
    mask_path VARCHAR(500) NULL COMMENT 'mask路径',
    bbox_json JSON NULL COMMENT '包围盒JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_nodule_result_study_id (study_id),
    KEY idx_nodule_result_ai_task_id (ai_task_id),
    CONSTRAINT fk_nodule_result_study FOREIGN KEY (study_id) REFERENCES ct_study(id),
    CONSTRAINT fk_nodule_result_ai_task FOREIGN KEY (ai_task_id) REFERENCES ai_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='肺结节分析结果表';

CREATE TABLE annotation_result (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_id BIGINT NOT NULL COMMENT '检查ID(ct_study.id)',
    ai_task_id BIGINT NOT NULL COMMENT 'AI任务ID(ai_task.id)',
    nodule_result_id BIGINT NOT NULL COMMENT '结节结果ID(nodule_result.id)',
    view_type VARCHAR(20) NOT NULL COMMENT '视图类型: AXIAL/CORONAL/SAGITTAL/THREE_D',
    overlay_path VARCHAR(500) NULL COMMENT '叠加图路径',
    contour_json JSON NULL COMMENT '轮廓JSON',
    color VARCHAR(20) NULL COMMENT '颜色',
    visible_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_annotation_study_id (study_id),
    KEY idx_annotation_ai_task_id (ai_task_id),
    KEY idx_annotation_nodule_result_id (nodule_result_id),
    CONSTRAINT fk_annotation_study FOREIGN KEY (study_id) REFERENCES ct_study(id),
    CONSTRAINT fk_annotation_ai_task FOREIGN KEY (ai_task_id) REFERENCES ai_task(id),
    CONSTRAINT fk_annotation_nodule FOREIGN KEY (nodule_result_id) REFERENCES nodule_result(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可视化标注结果表';

CREATE TABLE report_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    study_id BIGINT NOT NULL COMMENT '检查ID(ct_study.id)',
    patient_id BIGINT NOT NULL COMMENT '患者ID(patient_profile.id)',
    doctor_id BIGINT NULL COMMENT '医生ID(doctor_profile.id)',
    ai_task_id BIGINT NULL COMMENT 'AI任务ID(ai_task.id)',
    report_title VARCHAR(200) NULL COMMENT '报告标题',
    report_content TEXT NULL COMMENT '报告内容',
    report_summary VARCHAR(1000) NULL COMMENT '报告摘要',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/REVIEWED/FINAL',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    generated_by VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '生成者: SYSTEM/DOCTOR',
    audit_time DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_study_id (study_id),
    KEY idx_report_patient_id (patient_id),
    KEY idx_report_doctor_id (doctor_id),
    KEY idx_report_ai_task_id (ai_task_id),
    CONSTRAINT fk_report_study FOREIGN KEY (study_id) REFERENCES ct_study(id),
    CONSTRAINT fk_report_patient FOREIGN KEY (patient_id) REFERENCES patient_profile(id),
    CONSTRAINT fk_report_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profile(id),
    CONSTRAINT fk_report_ai_task FOREIGN KEY (ai_task_id) REFERENCES ai_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告单表';

CREATE TABLE operation_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户ID(sys_user.id)',
    module_name VARCHAR(50) NULL COMMENT '模块名',
    operation_type VARCHAR(50) NULL COMMENT '操作类型',
    operation_desc VARCHAR(500) NULL COMMENT '操作描述',
    request_path VARCHAR(255) NULL COMMENT '请求路径',
    request_method VARCHAR(20) NULL COMMENT '请求方法',
    ip VARCHAR(64) NULL COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_operation_user_id (user_id),
    CONSTRAINT fk_operation_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
