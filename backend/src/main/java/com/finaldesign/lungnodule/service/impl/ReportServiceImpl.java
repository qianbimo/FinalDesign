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
            throw new BusinessException(404, "报告不存在");
        }
        return report;
    }

    @Override
    public void updateReport(Long reportId, ReportUpdateRequest request, Long doctorUserId) {
        ReportRecord report = getById(reportId);
        DoctorProfile doctorProfile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, doctorUserId));
        if (doctorProfile == null) {
            throw new BusinessException(403, "仅医生可修改报告");
        }

        if (request.getReportTitle() != null) {
            report.setReportTitle(request.getReportTitle());
        }
        if (request.getReportContent() != null) {
            report.setReportContent(request.getReportContent());
        }
        if (request.getReportSummary() != null) {
            report.setReportSummary(request.getReportSummary());
        }
        report.setGeneratedBy("DOCTOR");
        report.setDoctorId(doctorProfile.getId());
        reportRecordMapper.updateById(report);
    }

    @Override
    public void auditReport(Long reportId, Long doctorUserId) {
        ReportRecord report = getById(reportId);
        DoctorProfile doctorProfile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, doctorUserId));
        if (doctorProfile == null) {
            throw new BusinessException(403, "仅医生可审核报告");
        }
        report.setStatus("REVIEWED");
        report.setDoctorId(doctorProfile.getId());
        report.setAuditTime(LocalDateTime.now());
        reportRecordMapper.updateById(report);
    }
}
