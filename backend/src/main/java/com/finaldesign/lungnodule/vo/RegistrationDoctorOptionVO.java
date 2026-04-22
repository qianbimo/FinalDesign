package com.finaldesign.lungnodule.vo;

import lombok.Data;

@Data
public class RegistrationDoctorOptionVO {
    private Long doctorId;
    private Long userId;
    private String realName;
    private String department;
    private String title;
}
