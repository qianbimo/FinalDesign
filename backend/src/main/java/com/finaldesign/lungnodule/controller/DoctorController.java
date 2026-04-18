package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.common.PageResult;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.DoctorService;
import com.finaldesign.lungnodule.vo.DoctorPatientVO;
import com.finaldesign.lungnodule.vo.DoctorStudyVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "查询医生资料")
    public Result<DoctorProfile> profile() {
        return Result.success(doctorService.getProfileByUserId(CurrentUserUtil.userId()));
    }

    @GetMapping("/patients")
    @Operation(summary = "查询患者列表")
    public Result<PageResult<DoctorPatientVO>> patients(@RequestParam(defaultValue = "1") Long current,
                                                       @RequestParam(defaultValue = "10") Long size) {
        IPage<DoctorPatientVO> page = doctorService.pagePatients(current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/studies")
    @Operation(summary = "查询医生分析过的病例")
    public Result<PageResult<DoctorStudyVO>> studies(@RequestParam(defaultValue = "1") Long current,
                                               @RequestParam(defaultValue = "10") Long size) {
        IPage<DoctorStudyVO> page = doctorService.pageDoctorStudies(CurrentUserUtil.userId(), current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/patient/{patientId}/studies/{studyId}")
    @Operation(summary = "查看某患者检查详情")
    public Result<CtStudy> patientStudy(@PathVariable Long patientId, @PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        return Result.success(doctorService.getPatientStudyDetail(patientId, studyId));
    }
}
