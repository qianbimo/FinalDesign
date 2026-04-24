package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.AdminResetPasswordRequest;
import com.finaldesign.lungnodule.dto.AdminUserCreateRequest;
import com.finaldesign.lungnodule.dto.AdminUserStatusUpdateRequest;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.service.AdminService;
import com.finaldesign.lungnodule.vo.AdminDashboardVO;
import com.finaldesign.lungnodule.vo.AdminReportVO;
import com.finaldesign.lungnodule.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "Admin list users")
    public Result<PageResult<AdminUserVO>> users(@RequestParam(defaultValue = "1") Long current,
                                                  @RequestParam(defaultValue = "10") Long size,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword) {
        IPage<AdminUserVO> page = adminService.pageUsers(current, size, role, status, keyword);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/reports")
    @Operation(summary = "Admin list all reports")
    public Result<PageResult<AdminReportVO>> reports(@RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "10") Long size,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String keyword) {
        IPage<AdminReportVO> page = adminService.pageReports(current, size, status, keyword);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping("/users")
    @Operation(summary = "Admin create user")
    public Result<Map<String, Long>> create(@Valid @RequestBody AdminUserCreateRequest request) {
        Long userId = adminService.createUser(request);
        return Result.success(Map.of("userId", userId));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Admin update user status")
    public Result<Void> updateStatus(@PathVariable("id") Long userId,
                                     @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        adminService.updateUserStatus(userId, Integer.valueOf(request.getStatus()));
        return Result.success("Updated", null);
    }

    @PutMapping("/users/{id}/reset-password")
    @Operation(summary = "Admin reset password")
    public Result<Void> resetPassword(@PathVariable("id") Long userId,
                                      @RequestBody(required = false) AdminResetPasswordRequest request) {
        adminService.resetPassword(userId, request == null ? null : request.getNewPassword());
        return Result.success("Password reset", null);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Admin delete user")
    public Result<Void> deleteUser(@PathVariable("id") Long userId) {
        adminService.deleteUser(CurrentUserUtil.userId(), userId);
        return Result.success("Deleted", null);
    }

    @DeleteMapping("/reports/{id}")
    @Operation(summary = "Admin delete report")
    public Result<Void> deleteReport(@PathVariable("id") Long reportId) {
        adminService.deleteReport(reportId);
        return Result.success("Deleted", null);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard")
    public Result<AdminDashboardVO> dashboard() {
        return Result.success(adminService.dashboard());
    }
}
