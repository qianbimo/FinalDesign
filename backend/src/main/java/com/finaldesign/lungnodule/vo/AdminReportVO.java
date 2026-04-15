package com.finaldesign.lungnodule.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminReportVO {
    private Long id;
    private Long studyId;
    private String studyNo;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long aiTaskId;
    private String reportTitle;
    private String reportSummary;
    private String status;
    private Integer versionNo;
    private String generatedBy;
    private LocalDateTime auditTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
