package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistrationCreateRequest {
    private Long patientId;

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "appointmentTime is required")
    private LocalDateTime appointmentTime;

    private String description;
}