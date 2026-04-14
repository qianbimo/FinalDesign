package com.finaldesign.lungnodule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserCreateRequest {
    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    @NotBlank(message = "Role cannot be empty")
    @Pattern(regexp = "PATIENT|DOCTOR|ADMIN", message = "Role must be PATIENT/DOCTOR/ADMIN")
    private String role;

    @NotBlank(message = "Real name cannot be empty")
    private String realName;

    private String phone;
    private String email;
    private Integer status;
}
