package com.finaldesign.lungnodule.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileUpdateRequest {
    private String gender;
    private LocalDate birthday;
    private String idCard;
    private String address;
    private String remark;
}
