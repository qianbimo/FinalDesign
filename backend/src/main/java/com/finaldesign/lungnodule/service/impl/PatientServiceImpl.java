package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.PatientProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.service.PatientService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientProfileMapper patientProfileMapper;
    private final CtStudyMapper ctStudyMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ReportRecordMapper reportRecordMapper;

    public PatientServiceImpl(PatientProfileMapper patientProfileMapper,
                              CtStudyMapper ctStudyMapper,
                              AiTaskMapper aiTaskMapper,
                              ReportRecordMapper reportRecordMapper) {
        this.patientProfileMapper = patientProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.reportRecordMapper = reportRecordMapper;
    }

    @Override
    public PatientProfile getProfileByUserId(Long userId) {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(404, "患者档案不存在");
        }
        return profile;
    }

    @Override
    public void updateProfile(Long userId, PatientProfileUpdateRequest request) {
        PatientProfile profile = getProfileByUserId(userId);
        BeanUtils.copyProperties(request, profile);
        patientProfileMapper.updateById(profile);
    }

    @Override
    public IPage<CtStudy> pageStudies(Long userId, Long current, Long size) {
        PatientProfile profile = getProfileByUserId(userId);
        Page<CtStudy> page = new Page<>(current, size);
        return ctStudyMapper.selectPage(page, new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getPatientId, profile.getId())
                .orderByDesc(CtStudy::getCreatedAt));
    }

    @Override
    public CtStudy getStudyDetail(Long userId, Long studyId) {
        PatientProfile profile = getProfileByUserId(userId);
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null || !study.getPatientId().equals(profile.getId())) {
            throw new BusinessException(404, "检查记录不存在");
        }
        return study;
    }

    @Override
    public AiTask getStudyAiResult(Long userId, Long studyId) {
        getStudyDetail(userId, studyId);
        return aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStudyId, studyId)
                .orderByDesc(AiTask::getCreatedAt)
                .last("limit 1"));
    }

    @Override
    public ReportRecord getStudyReport(Long userId, Long studyId) {
        getStudyDetail(userId, studyId);
        return reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, studyId)
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));
    }
}
