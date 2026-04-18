USE lung_nodule;

ALTER TABLE ct_study
    ADD COLUMN IF NOT EXISTS registration_id BIGINT NULL COMMENT '挂号单ID(registration_record.id)' AFTER study_no;

ALTER TABLE ct_study
    ADD UNIQUE KEY IF NOT EXISTS uk_ct_study_registration_id (registration_id);

ALTER TABLE ct_study
    ADD INDEX IF NOT EXISTS idx_ct_study_registration_id (registration_id);

ALTER TABLE ct_study
    ADD CONSTRAINT fk_ct_study_registration
    FOREIGN KEY (registration_id) REFERENCES registration_record(id);

