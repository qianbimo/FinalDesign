package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.AdminUserCreateRequest;
import com.finaldesign.lungnodule.vo.AdminDashboardVO;
import com.finaldesign.lungnodule.vo.AdminReportVO;
import com.finaldesign.lungnodule.vo.AdminUserVO;

public interface AdminService {
    IPage<AdminUserVO> pageUsers(Long current, Long size, String role, Integer status, String keyword);

    IPage<AdminReportVO> pageReports(Long current, Long size, String status, String keyword);

    Long createUser(AdminUserCreateRequest request);

    void updateUserStatus(Long userId, Integer status);

    void resetPassword(Long userId, String newPassword);

    AdminDashboardVO dashboard();
}
