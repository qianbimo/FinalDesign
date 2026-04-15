package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.ReportUpdateRequest;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;
    private final StudyAccessGuard studyAccessGuard;

    public ReportController(ReportService reportService, StudyAccessGuard studyAccessGuard) {
        this.reportService = reportService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @GetMapping("/study/{studyId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "按检查 ID 查询报告")
    public Result<ReportRecord> byStudy(@PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        return Result.success(reportService.getByStudyId(studyId));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "按报告 ID 查询报告")
    public Result<ReportRecord> byId(@PathVariable Long reportId) {
        ReportRecord report = reportService.getById(reportId);
        studyAccessGuard.assertCurrentUserCanAccessStudy(report.getStudyId());
        return Result.success(report);
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "修改报告单")
    public Result<Void> update(@PathVariable Long reportId, @Valid @RequestBody ReportUpdateRequest request) {
        ReportRecord report = reportService.getById(reportId);
        studyAccessGuard.assertCurrentUserCanManageStudy(report.getStudyId());
        reportService.updateReport(reportId, request, CurrentUserUtil.userId(), CurrentUserUtil.role());
        return Result.success("修改成功", null);
    }

    @PostMapping("/{reportId}/audit")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "审核报告单")
    public Result<Void> audit(@PathVariable Long reportId) {
        ReportRecord report = reportService.getById(reportId);
        studyAccessGuard.assertCurrentUserCanManageStudy(report.getStudyId());
        reportService.auditReport(reportId, CurrentUserUtil.userId(), CurrentUserUtil.role());
        return Result.success("审核成功", null);
    }
}
