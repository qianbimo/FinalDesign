package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.ReportUpdateRequest;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/study/{studyId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "查询某次检查报告")
    public Result<ReportRecord> byStudy(@PathVariable Long studyId) {
        return Result.success(reportService.getByStudyId(studyId));
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "修改报告单")
    public Result<Void> update(@PathVariable Long reportId, @Valid @RequestBody ReportUpdateRequest request) {
        reportService.updateReport(reportId, request, CurrentUserUtil.userId());
        return Result.success("修改成功", null);
    }

    @PostMapping("/{reportId}/audit")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "审核报告单")
    public Result<Void> audit(@PathVariable Long reportId) {
        reportService.auditReport(reportId, CurrentUserUtil.userId());
        return Result.success("审核成功", null);
    }
}
