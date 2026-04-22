package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient_profile")
public class PatientProfile extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String gender;
    private Integer age;
    private LocalDate birthday;
    private String idCard;
    private String medicalRecordNo;
    private String address;
    private String remark;
}
