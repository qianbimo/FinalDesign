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
        String aiRiskZh = toRiskZh(aiRisk);
        String suggestion = response.getSummary() != null && response.getSummary().getDiagnosisSuggestion() != null
                ? response.getSummary().getDiagnosisSuggestion()
                : "建议结合临床进一步检查，并定期随访。";

        String content = "检查项目：胸部CT智能分析\n"
                + "结节数量：" + noduleCount + "\n"
                + "最大结节直径：" + String.format("%.1f", maxDiameter) + " mm\n"
                + "高风险结节数量：" + highRiskCount + "\n"
                + "最高恶性概率：" + String.format("%.2f", maxProb) + "\n"
                + "AI风险评估：" + aiRiskZh + "\n"
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
        report.setReportSummary("检出结节" + noduleCount + "个，整体风险：" + aiRiskZh);
        report.setStatus("DRAFT");
        report.setVersionNo(latestVersion + 1);
        report.setGeneratedBy("SYSTEM");
        reportRecordMapper.insert(report);
        return report;
    }

    private String toRiskZh(String risk) {
        if (risk == null) {
            return "未知";
        }
        if ("HIGH".equalsIgnoreCase(risk)) {
            return "高风险";
        }
        if ("MEDIUM".equalsIgnoreCase(risk)) {
            return "中风险";
        }
        if ("LOW".equalsIgnoreCase(risk)) {
            return "低风险";
        }
        if ("UNKNOWN".equalsIgnoreCase(risk)) {
            return "未知";
        }
        return risk;
    }
}
