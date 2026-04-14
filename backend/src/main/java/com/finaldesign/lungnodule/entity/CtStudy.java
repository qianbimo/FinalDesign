package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ct_study")
public class CtStudy extends BaseEntity {
    @TableId
    private Long id;
    private String studyNo;
    private Long patientId;
    private Long doctorId;
    private LocalDate studyDate;
    private String studyDesc;
    private String deviceInfo;
    private String status;
}
