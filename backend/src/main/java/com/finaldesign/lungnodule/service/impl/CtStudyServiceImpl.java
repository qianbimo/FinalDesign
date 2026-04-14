package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.StudyCreateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.service.CtStudyService;
import com.finaldesign.lungnodule.utils.NoGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CtStudyServiceImpl implements CtStudyService {

    private final CtStudyMapper ctStudyMapper;

    public CtStudyServiceImpl(CtStudyMapper ctStudyMapper) {
        this.ctStudyMapper = ctStudyMapper;
    }

    @Override
    public Long create(StudyCreateRequest request) {
        CtStudy study = new CtStudy();
        study.setStudyNo(NoGenerator.studyNo());
        study.setPatientId(request.getPatientId());
        study.setDoctorId(request.getDoctorId());
        study.setStudyDate(request.getStudyDate() == null ? LocalDate.now() : request.getStudyDate());
        study.setStudyDesc(request.getStudyDesc());
        study.setDeviceInfo(request.getDeviceInfo());
        study.setStatus("UPLOADED");
        ctStudyMapper.insert(study);
        return study.getId();
    }

    @Override
    public IPage<CtStudy> pageList(Long current, Long size, Long patientId, Long doctorId) {
        Page<CtStudy> page = new Page<>(current, size);
        LambdaQueryWrapper<CtStudy> wrapper = new LambdaQueryWrapper<CtStudy>()
                .orderByDesc(CtStudy::getCreatedAt);
        if (patientId != null) {
            wrapper.eq(CtStudy::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(CtStudy::getDoctorId, doctorId);
        }
        return ctStudyMapper.selectPage(page, wrapper);
    }

    @Override
    public CtStudy detail(Long id) {
        CtStudy study = ctStudyMapper.selectById(id);
        if (study == null) {
            throw new BusinessException(404, "检查记录不存在");
        }
        return study;
    }
}
