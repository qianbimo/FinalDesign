package com.finaldesign.lungnodule.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DoctorPatientVO {
    private Long id;
    private Long userId;
    private String patientName;
    private String gender;
    private Integer age;
    private LocalDate birthday;
    private String medicalRecordNo;
    private String address;
}
