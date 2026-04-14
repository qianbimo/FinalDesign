package com.finaldesign.lungnodule.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String role;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private Long profileId;
}
