package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.dto.AiPredictRequest;
import com.finaldesign.lungnodule.dto.AiPredictResponse;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.AnnotationResult;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.NoduleResult;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.AnnotationResultMapper;
import com.finaldesign.lungnodule.mapper.CtFileMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.NoduleResultMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.service.AiInferenceClient;
import com.finaldesign.lungnodule.service.AiTaskService;
import com.finaldesign.lungnodule.service.ReportGenerateService;
import com.finaldesign.lungnodule.utils.JsonUtils;
import com.finaldesign.lungnodule.utils.NoGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTaskServiceImpl implements AiTaskService {

    private final CtStudyMapper ctStudyMapper;
    private final CtFileMapper ctFileMapper;
    private final AiTaskMapper aiTaskMapper;
    private final NoduleResultMapper noduleResultMapper;
    private final AnnotationResultMapper annotationResultMapper;
    private final ReportGenerateService reportGenerateService;
    private final ReportRecordMapper reportRecordMapper;
    private final AiInferenceClient aiInferenceClient;

    public AiTaskServiceImpl(CtStudyMapper ctStudyMapper,
                             CtFileMapper ctFileMapper,
                             AiTaskMapper aiTaskMapper,
                             NoduleResultMapper noduleResultMapper,
                             AnnotationResultMapper annotationResultMapper,
                             ReportGenerateService reportGenerateService,
                             ReportRecordMapper reportRecordMapper,
                             AiInferenceClient aiInferenceClient) {
        this.ctStudyMapper = ctStudyMapper;
        this.ctFileMapper = ctFileMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.noduleResultMapper = noduleResultMapper;
        this.annotationResultMapper = annotationResultMapper;
        this.reportGenerateService = reportGenerateService;
        this.reportRecordMapper = reportRecordMapper;
        this.aiInferenceClient = aiInferenceClient;
    }

    @Override
    public Long startTask(Long studyId) {
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null) {
            throw new BusinessException(404, "Study not found");
        }
        List<CtFile> files = ctFileMapper.selectList(new LambdaQueryWrapper<CtFile>()
                .eq(CtFile::getStudyId, studyId)
                .orderByDesc(CtFile::getCreatedAt));
        if (files.isEmpty()) {
            throw new BusinessException(400, "Please upload CT file first");
        }
        CtFile sourceFile = selectSourceFile(files);

        AiPredictRequest predictRequest = new AiPredictRequest();
        predictRequest.setStudyId(studyId);
        predictRequest.setPatientId(study.getPatientId());
        predictRequest.setFilePath(sourceFile.getFilePath());

        AiTask aiTask = new AiTask();
        aiTask.setStudyId(studyId);
        aiTask.setTaskNo(NoGenerator.taskNo());
        aiTask.setModelVersion("3D-ResUNet+Mamba-reserved-v1");
        aiTask.setTaskStatus("WAITING");
        aiTask.setRequestJson(JsonUtils.toJson(predictRequest));
        aiTaskMapper.insert(aiTask);

        aiTask.setTaskStatus("RUNNING");
        aiTask.setStartedAt(LocalDateTime.now());
        aiTaskMapper.updateById(aiTask);

        study.setStatus("ANALYZING");
        ctStudyMapper.updateById(study);

        try {
            AiPredictResponse response = aiInferenceClient.predict(predictRequest);
            aiTask.setResponseJson(JsonUtils.toJson(response));

            if (!"SUCCESS".equalsIgnoreCase(response.getTaskStatus())) {
                aiTask.setTaskStatus("FAILED");
                aiTask.setErrorMessage("AI returned failed status: " + response.getTaskStatus());
                study.setStatus("FAILED");
            } else {
                aiTask.setTaskStatus("SUCCESS");
                study.setStatus("FINISHED");
                saveNoduleAndAnnotation(studyId, aiTask.getId(), response);
                reportGenerateService.generateDraft(study, aiTask, response);
            }
            aiTask.setFinishedAt(LocalDateTime.now());
            aiTaskMapper.updateById(aiTask);
            ctStudyMapper.updateById(study);
            return aiTask.getId();
        } catch (Exception e) {
            aiTask.setTaskStatus("FAILED");
            aiTask.setErrorMessage(e.getMessage());
            aiTask.setFinishedAt(LocalDateTime.now());
            aiTaskMapper.updateById(aiTask);
            study.setStatus("FAILED");
            ctStudyMapper.updateById(study);
            throw new BusinessException("AI task failed: " + e.getMessage());
        }
    }

    @Override
    public AiTask getTask(Long taskId) {
        AiTask aiTask = aiTaskMapper.selectById(taskId);
        if (aiTask == null) {
            throw new BusinessException(404, "AI task not found");
        }
        return aiTask;
    }

    @Override
    public Map<String, Object> getStudyResult(Long studyId) {
        AiTask task = aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStudyId, studyId)
                .orderByDesc(AiTask::getCreatedAt)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException(404, "No AI task found for this study");
        }
        List<NoduleResult> nodules = noduleResultMapper.selectList(new LambdaQueryWrapper<NoduleResult>()
                .eq(NoduleResult::getAiTaskId, task.getId())
                .orderByAsc(NoduleResult::getNoduleNo));
        List<AnnotationResult> annotations = annotationResultMapper.selectList(new LambdaQueryWrapper<AnnotationResult>()
                .eq(AnnotationResult::getAiTaskId, task.getId()));
        ReportRecord report = reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, studyId)
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));

        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("nodules", nodules);
        result.put("annotations", annotations);
        result.put("report", report);
        return result;
    }

    private void saveNoduleAndAnnotation(Long studyId, Long aiTaskId, AiPredictResponse response) {
        if (response.getNodules() == null || response.getNodules().isEmpty()) {
            return;
        }
        for (AiPredictResponse.Nodule nodule : response.getNodules()) {
            NoduleResult noduleResult = new NoduleResult();
            noduleResult.setStudyId(studyId);
            noduleResult.setAiTaskId(aiTaskId);
            noduleResult.setNoduleNo(nodule.getNoduleNo());
            noduleResult.setCenterX(nodule.getCenterX());
            noduleResult.setCenterY(nodule.getCenterY());
            noduleResult.setCenterZ(nodule.getCenterZ());
            noduleResult.setWidth(nodule.getWidth());
            noduleResult.setHeight(nodule.getHeight());
            noduleResult.setDepth(nodule.getDepth());
            noduleResult.setVolume(nodule.getVolume());
            noduleResult.setDiameterMm(nodule.getDiameterMm());
            noduleResult.setMalignancyProb(nodule.getMalignancyProb());
            noduleResult.setRiskLevel(nodule.getRiskLevel());
            noduleResult.setDescription(nodule.getDescription());
            noduleResult.setMaskPath(nodule.getMaskPath());
            noduleResult.setBboxJson(nodule.getBbox() == null ? null : JsonUtils.toJson(nodule.getBbox()));
            noduleResultMapper.insert(noduleResult);

            if (nodule.getAnnotations() == null || nodule.getAnnotations().isEmpty()) {
                continue;
            }
            for (AiPredictResponse.Annotation ann : nodule.getAnnotations()) {
                AnnotationResult annotationResult = new AnnotationResult();
                annotationResult.setStudyId(studyId);
                annotationResult.setAiTaskId(aiTaskId);
                annotationResult.setNoduleResultId(noduleResult.getId());
                annotationResult.setViewType(ann.getViewType());
                annotationResult.setOverlayPath(ann.getOverlayPath());
                annotationResult.setColor(ann.getColor());
                annotationResult.setVisibleFlag(1);
                annotationResultMapper.insert(annotationResult);
            }
        }
    }

    private CtFile selectSourceFile(List<CtFile> files) {
        for (CtFile file : files) {
            if ("MHD".equalsIgnoreCase(file.getFileType())) {
                return file;
            }
        }
        for (CtFile file : files) {
            if ("NII_GZ".equalsIgnoreCase(file.getFileType())
                    || "NII".equalsIgnoreCase(file.getFileType())
                    || "DCM".equalsIgnoreCase(file.getFileType())) {
                return file;
            }
        }
        return files.get(0);
    }
}
