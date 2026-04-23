package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.PatientPasswordUpdateRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get patient profile")
    public Result<PatientProfile> profile() {
        return Result.success(patientService.getProfileByUserId(CurrentUserUtil.userId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update patient profile")
    public Result<Void> updateProfile(@Valid @RequestBody PatientProfileUpdateRequest request) {
        patientService.updateProfile(CurrentUserUtil.userId(), request);
        return Result.success("Profile updated successfully", null);
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update patient password")
    public Result<Void> updatePassword(@Valid @RequestBody PatientPasswordUpdateRequest request) {
        patientService.updatePassword(CurrentUserUtil.userId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("Password updated successfully", null);
    }

    @GetMapping("/studies")
    @Operation(summary = "Get patient studies")
    public Result<PageResult<CtStudy>> studies(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size,
                                                @RequestParam(defaultValue = "false") Boolean includeCancelled) {
        IPage<CtStudy> page = patientService.pageStudies(CurrentUserUtil.userId(), current, size, Boolean.TRUE.equals(includeCancelled));
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/studies/{studyId}")
    @Operation(summary = "Get patient study detail")
    public Result<CtStudy> studyDetail(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyDetail(CurrentUserUtil.userId(), studyId));
    }

    @GetMapping("/studies/{studyId}/ai-result")
    @Operation(summary = "Get patient AI result")
    public Result<AiTask> aiResult(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyAiResult(CurrentUserUtil.userId(), studyId));
    }

    @GetMapping("/studies/{studyId}/report")
    @Operation(summary = "Get patient report")
    public Result<ReportRecord> report(@PathVariable Long studyId) {
        return Result.success(patientService.getStudyReport(CurrentUserUtil.userId(), studyId));
    }
}
