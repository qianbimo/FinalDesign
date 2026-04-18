package com.finaldesign.lungnodule.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DoctorStudyVO {
    private Long id;
    private String studyNo;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private LocalDate studyDate;
    private String studyDesc;
    private String status;
}
