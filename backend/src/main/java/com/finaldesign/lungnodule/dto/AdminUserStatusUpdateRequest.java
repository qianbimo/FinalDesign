package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {
    @NotNull(message = "Status cannot be null")
    @Pattern(regexp = "0|1", message = "Status must be 0 or 1")
    private String status;
}
