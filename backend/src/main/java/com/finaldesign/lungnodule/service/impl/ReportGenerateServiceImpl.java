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
        String suggestion = response.getSummary() != null ? response.getSummary().getDiagnosisSuggestion() : "建议结合临床进一步评估";

        String content = "检查项目：胸部 CT 智能分析\n"
                + "结节数量：" + noduleCount + "\n"
                + "最大结节直径：" + String.format("%.1f", maxDiameter) + " mm\n"
                + "高风险结节数：" + highRiskCount + "\n"
                + "最高恶性风险概率：" + String.format("%.2f", maxProb) + "\n"
                + "AI 风险评估：" + aiRisk + "\n"
                + "建议：" + suggestion;

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
        report.setReportTitle("胸部CT智能分析报告");
        report.setReportContent(content);
        report.setReportSummary("发现结节" + noduleCount + "个，整体风险" + aiRisk);
        report.setStatus("DRAFT");
        report.setVersionNo(latestVersion + 1);
        report.setGeneratedBy("SYSTEM");
        reportRecordMapper.insert(report);
        return report;
    }
}
