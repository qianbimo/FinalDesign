package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.service.DoctorService;
import org.springframework.stereotype.Service;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorProfileMapper doctorProfileMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final CtStudyMapper ctStudyMapper;

    public DoctorServiceImpl(DoctorProfileMapper doctorProfileMapper,
                             PatientProfileMapper patientProfileMapper,
                             CtStudyMapper ctStudyMapper) {
        this.doctorProfileMapper = doctorProfileMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
    }

    @Override
    public DoctorProfile getProfileByUserId(Long userId) {
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(404, "医生档案不存在");
        }
        return profile;
    }

    @Override
    public IPage<PatientProfile> pagePatients(Long current, Long size) {
        Page<PatientProfile> page = new Page<>(current, size);
        return patientProfileMapper.selectPage(page, new LambdaQueryWrapper<PatientProfile>()
                .orderByDesc(PatientProfile::getCreatedAt));
    }

    @Override
    public IPage<CtStudy> pageDoctorStudies(Long doctorUserId, Long current, Long size) {
        DoctorProfile doctorProfile = getProfileByUserId(doctorUserId);
        Page<CtStudy> page = new Page<>(current, size);
        return ctStudyMapper.selectPage(page, new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getDoctorId, doctorProfile.getId())
                .orderByDesc(CtStudy::getCreatedAt));
    }

    @Override
    public CtStudy getPatientStudyDetail(Long patientId, Long studyId) {
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null || !study.getPatientId().equals(patientId)) {
            throw new BusinessException(404, "检查记录不存在");
        }
        return study;
    }
}
