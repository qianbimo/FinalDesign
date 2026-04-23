package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.PatientProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.ReportRecord;

public interface PatientService {
    PatientProfile getProfileByUserId(Long userId);

    void updateProfile(Long userId, PatientProfileUpdateRequest request);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    IPage<CtStudy> pageStudies(Long userId, Long current, Long size, boolean includeCancelled);

    CtStudy getStudyDetail(Long userId, Long studyId);

    AiTask getStudyAiResult(Long userId, Long studyId);

    ReportRecord getStudyReport(Long userId, Long studyId);
}
