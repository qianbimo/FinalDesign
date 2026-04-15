package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.dto.RegistrationCreateRequest;
import com.finaldesign.lungnodule.dto.RegistrationStatusUpdateRequest;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.service.DoctorService;
import com.finaldesign.lungnodule.service.PatientService;
import com.finaldesign.lungnodule.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    public RegistrationController(RegistrationService registrationService,
                                  PatientService patientService,
                                  DoctorService doctorService) {
        this.registrationService = registrationService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "患者发起挂号")
    public Result<Map<String, Long>> create(@Valid @RequestBody RegistrationCreateRequest request) {
        PatientProfile profile = patientService.getProfileByUserId(CurrentUserUtil.userId());
        request.setPatientId(profile.getId());
        Long id = registrationService.create(request);
        return Result.success(Map.of("id", id));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "医生查看挂号列表")
    public Result<PageResult<RegistrationRecord>> list(@RequestParam(defaultValue = "1") Long current,
                                                       @RequestParam(defaultValue = "10") Long size) {
        Long doctorId = null;
        if ("DOCTOR".equals(CurrentUserUtil.role())) {
            DoctorProfile profile = doctorService.getProfileByUserId(CurrentUserUtil.userId());
            doctorId = profile.getId();
        }
        IPage<RegistrationRecord> page = registrationService.list(current, size, doctorId);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "修改挂号状态")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody RegistrationStatusUpdateRequest request) {
        Long doctorId = null;
        if ("DOCTOR".equals(CurrentUserUtil.role())) {
            doctorId = doctorService.getProfileByUserId(CurrentUserUtil.userId()).getId();
        }
        registrationService.updateStatus(id, request.getStatus(), doctorId);
        return Result.success("更新成功", null);
    }
}
