package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistrationCreateRequest {
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;

    private String description;
}
