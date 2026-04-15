package com.finaldesign.lungnodule.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class StudyAccessGuard {

    private final CtStudyMapper ctStudyMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorProfileMapper doctorProfileMapper;

    public StudyAccessGuard(CtStudyMapper ctStudyMapper,
                            PatientProfileMapper patientProfileMapper,
                            DoctorProfileMapper doctorProfileMapper) {
        this.ctStudyMapper = ctStudyMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    public CtStudy getStudyOrThrow(Long studyId) {
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null) {
            throw new BusinessException(404, "Study not found");
        }
        return study;
    }

    public void assertCurrentUserCanAccessStudy(Long studyId) {
        assertCurrentUserCanAccessStudy(getStudyOrThrow(studyId));
    }

    public void assertCurrentUserCanAccessStudy(CtStudy study) {
        String role = CurrentUserUtil.role();
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("PATIENT".equals(role)) {
            PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                    .eq(PatientProfile::getUserId, CurrentUserUtil.userId()));
            if (profile == null || !Objects.equals(study.getPatientId(), profile.getId())) {
                throw new BusinessException(403, "Access denied");
            }
            return;
        }
        if ("DOCTOR".equals(role)) {
            DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                    .eq(DoctorProfile::getUserId, CurrentUserUtil.userId()));
            if (profile == null || !Objects.equals(study.getDoctorId(), profile.getId())) {
                throw new BusinessException(403, "Access denied");
            }
            return;
        }
        throw new BusinessException(403, "Access denied");
    }

    public void assertCurrentUserCanManageStudy(Long studyId) {
        assertCurrentUserCanManageStudy(getStudyOrThrow(studyId));
    }

    public void assertCurrentUserCanManageStudy(CtStudy study) {
        String role = CurrentUserUtil.role();
        if ("ADMIN".equals(role)) {
            return;
        }
        if (!"DOCTOR".equals(role)) {
            throw new BusinessException(403, "Access denied");
        }
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, CurrentUserUtil.userId()));
        if (profile == null || !Objects.equals(study.getDoctorId(), profile.getId())) {
            throw new BusinessException(403, "Access denied");
        }
    }

    public Long currentPatientProfileId() {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, CurrentUserUtil.userId()));
        if (profile == null) {
            throw new BusinessException(404, "Patient profile not found");
        }
        return profile.getId();
    }

    public Long currentDoctorProfileId() {
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, CurrentUserUtil.userId()));
        if (profile == null) {
            throw new BusinessException(404, "Doctor profile not found");
        }
        return profile.getId();
    }
}
