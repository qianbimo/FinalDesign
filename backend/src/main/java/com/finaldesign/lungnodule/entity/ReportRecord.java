package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_record")
public class ReportRecord extends BaseEntity {
    @TableId
    private Long id;
    private Long studyId;
    private Long patientId;
    private Long doctorId;
    private Long aiTaskId;
    private String reportTitle;
    private String reportContent;
    private String reportSummary;
    private String status;
    private Integer versionNo;
    private String generatedBy;
    private LocalDateTime auditTime;
}
