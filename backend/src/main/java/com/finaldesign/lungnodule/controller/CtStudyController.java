package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.StudyCreateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.CtStudyService;
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
    private final StudyAccessGuard studyAccessGuard;

    public CtStudyController(CtStudyService ctStudyService,
                             StudyAccessGuard studyAccessGuard) {
        this.ctStudyService = ctStudyService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Create CT study")
    public Result<Map<String, Long>> create(@Valid @RequestBody StudyCreateRequest request) {
        String role = CurrentUserUtil.role();
        Long operatorDoctorId = "DOCTOR".equals(role) ? studyAccessGuard.currentDoctorProfileId() : null;
        boolean adminMode = "ADMIN".equals(role);
        Long id = ctStudyService.create(request, operatorDoctorId, adminMode);
        return Result.success(Map.of("studyId", id));
    }

    @GetMapping("/list")
    @Operation(summary = "List CT studies")
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
    @Operation(summary = "Get CT study detail")
    public Result<CtStudy> detail(@PathVariable Long id) {
        CtStudy study = ctStudyService.detail(id);
        studyAccessGuard.assertCurrentUserCanAccessStudy(study);
        return Result.success(study);
    }
}

