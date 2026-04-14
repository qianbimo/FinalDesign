package com.finaldesign.lungnodule.dto;

import lombok.Data;

@Data
public class AiPredictRequest {
    private Long studyId;
    private String filePath;
    private Long patientId;
}
