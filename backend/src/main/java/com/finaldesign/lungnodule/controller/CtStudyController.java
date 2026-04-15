package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.StudyCreateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.CtStudyService;
import com.finaldesign.lungnodule.service.DoctorService;
import com.finaldesign.lungnodule.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/study")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class CtStudyController {

    private final CtStudyService ctStudyService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final StudyAccessGuard studyAccessGuard;

    public CtStudyController(CtStudyService ctStudyService,
                             PatientService patientService,
                             DoctorService doctorService,
                             StudyAccessGuard studyAccessGuard) {
        this.ctStudyService = ctStudyService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @PostMapping("/create")
    @Operation(summary = "创建检查记录")
    public Result<Map<String, Long>> create(@Valid @RequestBody StudyCreateRequest request) {
        String role = CurrentUserUtil.role();
        if ("PATIENT".equals(role)) {
            PatientProfile patientProfile = patientService.getProfileByUserId(CurrentUserUtil.userId());
            request.setPatientId(patientProfile.getId());
        } else if (request.getPatientId() == null) {
            throw new BusinessException(400, "patientId is required");
        }
        if ("DOCTOR".equals(role)) {
            DoctorProfile doctorProfile = doctorService.getProfileByUserId(CurrentUserUtil.userId());
            request.setDoctorId(doctorProfile.getId());
        }
        Long id = ctStudyService.create(request);
        return Result.success(Map.of("studyId", id));
    }

    @GetMapping("/list")
    @Operation(summary = "查询检查记录列表")
    public Result<PageResult<CtStudy>> list(@RequestParam(defaultValue = "1") Long current,
                                            @RequestParam(defaultValue = "10") Long size,
                                            @RequestParam(required = false) Long patientId,
                                            @RequestParam(required = false) Long doctorId) {
        String role = CurrentUserUtil.role();
        if ("PATIENT".equals(role)) {
            patientId = studyAccessGuard.currentPatientProfileId();
            doctorId = null;
        } else if ("DOCTOR".equals(role)) {
            doctorId = studyAccessGuard.currentDoctorProfileId();
            patientId = null;
        }

        IPage<CtStudy> page = ctStudyService.pageList(current, size, patientId, doctorId);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询检查详情")
    public Result<CtStudy> detail(@PathVariable Long id) {
        CtStudy study = ctStudyService.detail(id);
        studyAccessGuard.assertCurrentUserCanAccessStudy(study);
        return Result.success(study);
    }
}
