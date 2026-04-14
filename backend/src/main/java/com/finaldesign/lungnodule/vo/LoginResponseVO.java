package com.finaldesign.lungnodule.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseVO {
    private String token;
    private String role;
    private Long userId;
    private String realName;
}
