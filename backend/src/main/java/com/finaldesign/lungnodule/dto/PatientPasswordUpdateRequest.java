package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientPasswordUpdateRequest {
    @NotBlank(message = "Old password cannot be empty")
    private String oldPassword;

    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
}
