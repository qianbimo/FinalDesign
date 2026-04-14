package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegistrationStatusUpdateRequest {
    @Pattern(regexp = "PENDING|CONFIRMED|CANCELLED|FINISHED", message = "状态非法")
    private String status;
}
