package com.finaldesign.lungnodule.security;

import com.finaldesign.lungnodule.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(401, "未登录或登录已失效");
        }
        return loginUser;
    }

    public static Long userId() {
        return getCurrentUser().getUserId();
    }

    public static String role() {
        return getCurrentUser().getRole();
    }
}
