package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.dto.AiPredictResponse;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.service.ReportGenerateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportGenerateServiceImpl implements ReportGenerateService {

    private final ReportRecordMapper reportRecordMapper;

    public ReportGenerateServiceImpl(ReportRecordMapper reportRecordMapper) {
        this.reportRecordMapper = reportRecordMapper;
    }

    @Override
    public ReportRecord generateDraft(CtStudy study, AiTask aiTask, AiPredictResponse response) {
        List<AiPredictResponse.Nodule> nodules = response.getNodules() == null ? List.of() : response.getNodules();
        int noduleCount = nodules.size();
        double maxDiameter = nodules.stream().mapToDouble(n -> n.getDiameterMm() == null ? 0D : n.getDiameterMm()).max().orElse(0D);
        double maxProb = nodules.stream().mapToDouble(n -> n.getMalignancyProb() == null ? 0D : n.getMalignancyProb()).max().orElse(0D);
        long highRiskCount = nodules.stream().filter(n -> "HIGH".equalsIgnoreCase(n.getRiskLevel())).count();

        String aiRisk = response.getSummary() != null ? response.getSummary().getOverallRisk() : "UNKNOWN";
        String suggestion = response.getSummary() != null && response.getSummary().getDiagnosisSuggestion() != null
                ? response.getSummary().getDiagnosisSuggestion()
                : "Recommend clinical correlation and periodic follow-up.";

        String content = "Exam: Chest CT Intelligence Analysis\n"
                + "Nodule count: " + noduleCount + "\n"
                + "Max nodule diameter: " + String.format("%.1f", maxDiameter) + " mm\n"
                + "High-risk nodule count: " + highRiskCount + "\n"
                + "Highest malignancy probability: " + String.format("%.2f", maxProb) + "\n"
                + "AI risk assessment: " + aiRisk + "\n"
                + "Suggestion: " + suggestion;

        Integer latestVersion = 0;
        ReportRecord latest = reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, study.getId())
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));
        if (latest != null && latest.getVersionNo() != null) {
            latestVersion = latest.getVersionNo();
        }

        ReportRecord report = new ReportRecord();
        report.setStudyId(study.getId());
        report.setPatientId(study.getPatientId());
        report.setDoctorId(study.getDoctorId());
        report.setAiTaskId(aiTask.getId());
        report.setReportTitle("Chest CT Intelligence Analysis Report");
        report.setReportContent(content);
        report.setReportSummary("Detected " + noduleCount + " nodules, overall risk " + aiRisk);
        report.setStatus("DRAFT");
        report.setVersionNo(latestVersion + 1);
        report.setGeneratedBy("SYSTEM");
        reportRecordMapper.insert(report);
        return report;
    }
}