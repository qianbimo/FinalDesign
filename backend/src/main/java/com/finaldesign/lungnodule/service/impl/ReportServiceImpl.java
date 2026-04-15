package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.dto.ReportUpdateRequest;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.service.ReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRecordMapper reportRecordMapper;
    private final DoctorProfileMapper doctorProfileMapper;

    public ReportServiceImpl(ReportRecordMapper reportRecordMapper, DoctorProfileMapper doctorProfileMapper) {
        this.reportRecordMapper = reportRecordMapper;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    @Override
    public ReportRecord getByStudyId(Long studyId) {
        return reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, studyId)
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));
    }

    @Override
    public ReportRecord getById(Long reportId) {
        ReportRecord report = reportRecordMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "Report not found");
        }
        return report;
    }

    @Override
    public void updateReport(Long reportId, ReportUpdateRequest request, Long operatorUserId, String operatorRole) {
        ReportRecord report = getById(reportId);

        if (request.getReportTitle() != null) {
            report.setReportTitle(request.getReportTitle());
        }
        if (request.getReportContent() != null) {
            report.setReportContent(request.getReportContent());
        }
        if (request.getReportSummary() != null) {
            report.setReportSummary(request.getReportSummary());
        }

        if ("DOCTOR".equals(operatorRole)) {
            DoctorProfile doctorProfile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                    .eq(DoctorProfile::getUserId, operatorUserId));
            if (doctorProfile == null) {
                throw new BusinessException(403, "Only doctors can update report");
            }
            report.setGeneratedBy("DOCTOR");
            report.setDoctorId(doctorProfile.getId());
        }

        reportRecordMapper.updateById(report);
    }

    @Override
    public void auditReport(Long reportId, Long operatorUserId, String operatorRole) {
        ReportRecord report = getById(reportId);

        if ("DOCTOR".equals(operatorRole)) {
            DoctorProfile doctorProfile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                    .eq(DoctorProfile::getUserId, operatorUserId));
            if (doctorProfile == null) {
                throw new BusinessException(403, "Only doctors can audit report");
            }
            report.setDoctorId(doctorProfile.getId());
        }

        report.setStatus("REVIEWED");
        report.setAuditTime(LocalDateTime.now());
        reportRecordMapper.updateById(report);
    }
}
