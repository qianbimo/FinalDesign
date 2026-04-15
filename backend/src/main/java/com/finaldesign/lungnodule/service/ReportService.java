package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.dto.ReportUpdateRequest;
import com.finaldesign.lungnodule.entity.ReportRecord;

public interface ReportService {
    ReportRecord getByStudyId(Long studyId);

    ReportRecord getById(Long reportId);

    void updateReport(Long reportId, ReportUpdateRequest request, Long operatorUserId, String operatorRole);

    void auditReport(Long reportId, Long operatorUserId, String operatorRole);
}
