package com.finaldesign.lungnodule.dto;

import lombok.Data;

@Data
public class DoctorProfileUpdateRequest {
    private String department;
    private String title;
    private String licenseNo;
    private String introduction;
}
