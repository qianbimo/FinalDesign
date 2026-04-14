package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_profile")
public class DoctorProfile extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String department;
    private String title;
    private String specialty;
    private String licenseNo;
    private String introduction;
}
