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
        ReportRecord report = reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, studyId)
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));
        return localizeSystemReportIfNeeded(report);
    }

    @Override
    public ReportRecord getById(Long reportId) {
        ReportRecord report = reportRecordMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "Report not found");
        }
        return localizeSystemReportIfNeeded(report);
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

    private ReportRecord localizeSystemReportIfNeeded(ReportRecord report) {
        if (report == null || !"SYSTEM".equalsIgnoreCase(report.getGeneratedBy())) {
            return report;
        }
        boolean changed = false;

        String title = report.getReportTitle();
        if ("Chest CT Intelligence Analysis Report".equalsIgnoreCase(title)) {
            report.setReportTitle("胸部CT智能分析报告");
            changed = true;
        }

        String summary = report.getReportSummary();
        if (summary != null && !summary.isBlank()) {
            String localizedSummary = summary;
            localizedSummary = localizedSummary.replace("Detected ", "检出");
            localizedSummary = localizedSummary.replace(" nodules, overall risk ", "个结节，整体风险：");
            localizedSummary = replaceRiskToken(localizedSummary);
            if (!localizedSummary.equals(summary)) {
                report.setReportSummary(localizedSummary);
                changed = true;
            }
        }

        String content = report.getReportContent();
        if (content != null && !content.isBlank()) {
            String localizedContent = content;
            localizedContent = localizedContent.replace("Exam: Chest CT Intelligence Analysis", "检查项目：胸部CT智能分析");
            localizedContent = localizedContent.replace("Nodule count: ", "结节数量：");
            localizedContent = localizedContent.replace("Max nodule diameter: ", "最大结节直径：");
            localizedContent = localizedContent.replace("High-risk nodule count: ", "高风险结节数量：");
            localizedContent = localizedContent.replace("Highest malignancy probability: ", "最高恶性概率：");
            localizedContent = localizedContent.replace("AI risk assessment: ", "AI风险评估：");
            localizedContent = localizedContent.replace("Suggestion: ", "建议：");
            localizedContent = localizedContent.replace("Recommend clinical correlation and periodic follow-up.",
                    "建议结合临床进一步检查，并定期随访。");
            localizedContent = replaceRiskToken(localizedContent);
            if (!localizedContent.equals(content)) {
                report.setReportContent(localizedContent);
                changed = true;
            }
        }

        if (changed) {
            reportRecordMapper.updateById(report);
        }
        return report;
    }

    private String replaceRiskToken(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replace(" HIGH", " 高风险")
                .replace(" MEDIUM", " 中风险")
                .replace(" LOW", " 低风险")
                .replace(" UNKNOWN", " 未知");
    }
}
