package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.dto.LoginRequest;
import com.finaldesign.lungnodule.dto.RegisterRequest;
import com.finaldesign.lungnodule.vo.LoginResponseVO;

public interface AuthService {
    Long register(RegisterRequest request);

    LoginResponseVO login(LoginRequest request);
}
