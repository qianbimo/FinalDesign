package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.DoctorPasswordUpdateRequest;
import com.finaldesign.lungnodule.dto.DoctorProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.DoctorService;
import com.finaldesign.lungnodule.vo.DoctorPatientVO;
import com.finaldesign.lungnodule.vo.DoctorStudyVO;
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
@RequestMapping("/api/doctor")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class DoctorController {

    private final DoctorService doctorService;
    private final StudyAccessGuard studyAccessGuard;

    public DoctorController(DoctorService doctorService, StudyAccessGuard studyAccessGuard) {
        this.doctorService = doctorService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get doctor profile")
    public Result<DoctorProfile> profile() {
        return Result.success(doctorService.getProfileByUserId(CurrentUserUtil.userId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update doctor profile")
    public Result<Void> updateProfile(@Valid @RequestBody DoctorProfileUpdateRequest request) {
        doctorService.updateProfile(CurrentUserUtil.userId(), request);
        return Result.success("Profile updated successfully", null);
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update doctor password")
    public Result<Void> updatePassword(@Valid @RequestBody DoctorPasswordUpdateRequest request) {
        doctorService.updatePassword(CurrentUserUtil.userId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("Password updated successfully", null);
    }

    @GetMapping("/patients")
    @Operation(summary = "Get doctor patients")
    public Result<PageResult<DoctorPatientVO>> patients(@RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        Long doctorUserId = null;
        if ("DOCTOR".equals(CurrentUserUtil.role())) {
            doctorUserId = CurrentUserUtil.userId();
        }
        IPage<DoctorPatientVO> page = doctorService.pagePatients(doctorUserId, current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/studies")
    @Operation(summary = "Get doctor studies")
    public Result<PageResult<DoctorStudyVO>> studies(@RequestParam(defaultValue = "1") Long current,
                                                     @RequestParam(defaultValue = "10") Long size,
                                                     @RequestParam(required = false) String patientName,
                                                     @RequestParam(required = false) String status) {
        IPage<DoctorStudyVO> page = doctorService.pageDoctorStudies(
                CurrentUserUtil.userId(),
                current,
                size,
                patientName,
                status
        );
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/patient/{patientId}/studies/{studyId}")
    @Operation(summary = "Get doctor patient study detail")
    public Result<DoctorStudyVO> patientStudy(@PathVariable Long patientId, @PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        return Result.success(doctorService.getPatientStudyDetail(patientId, studyId));
    }
}
