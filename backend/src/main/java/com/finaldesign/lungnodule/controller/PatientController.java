package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.PatientProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/profile")
    @Operation(summary = "查询患者个人资料")
    public Result<PatientProfile> profile() {
        return Result.success(patientService.getProfileByUserId(CurrentUserUtil.userId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新患者个人资料")
    public Result<Void> updateProfile(@Valid @RequestBody PatientProfileUpdateRequest request) {
        patientService.updateProfile(CurrentUserUtil.userId(), request);
        return Result.success("更新成功", null);
    }

    @GetMapping("/studies")
    @Operation(summary = "查询历史检查记录")
    public Result<PageResult<CtStudy>> studies(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size) {
        IPage<CtStudy> page = patientService.pageStudies(CurrentUserUtil.userId(), current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/studies/{studyId}")
    @Operation(summary = "查询某次检查详情")
    public Result<CtStudy> studyDetail(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyDetail(CurrentUserUtil.userId(), studyId));
    }

    @GetMapping("/studies/{studyId}/ai-result")
    @Operation(summary = "查询某次检查AI结果")
    public Result<AiTask> aiResult(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyAiResult(CurrentUserUtil.userId(), studyId));
    }

    @GetMapping("/studies/{studyId}/report")
    @Operation(summary = "查询某次检查报告单")
    public Result<ReportRecord> report(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyReport(CurrentUserUtil.userId(), studyId));
    }
}
