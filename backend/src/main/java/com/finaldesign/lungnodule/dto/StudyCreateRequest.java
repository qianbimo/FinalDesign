package com.finaldesign.lungnodule.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudyCreateRequest {
    private Long patientId;

    private Long doctorId;
    private LocalDate studyDate;
    private String studyDesc;
    private String deviceInfo;
}
