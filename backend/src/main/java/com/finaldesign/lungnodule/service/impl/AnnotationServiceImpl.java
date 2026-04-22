package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.dto.AiPredictResponse;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.AnnotationResult;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.entity.NoduleResult;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.AnnotationResultMapper;
import com.finaldesign.lungnodule.mapper.CtFileMapper;
import com.finaldesign.lungnodule.mapper.NoduleResultMapper;
import com.finaldesign.lungnodule.service.AnnotationService;
import com.finaldesign.lungnodule.utils.JsonUtils;
import com.finaldesign.lungnodule.vo.AnnotationStudyVO;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class AnnotationServiceImpl implements AnnotationService {

    private final AiTaskMapper aiTaskMapper;
    private final CtFileMapper ctFileMapper;
    private final NoduleResultMapper noduleResultMapper;
    private final AnnotationResultMapper annotationResultMapper;

    public AnnotationServiceImpl(AiTaskMapper aiTaskMapper,
                                 CtFileMapper ctFileMapper,
                                 NoduleResultMapper noduleResultMapper,
                                 AnnotationResultMapper annotationResultMapper) {
        this.aiTaskMapper = aiTaskMapper;
        this.ctFileMapper = ctFileMapper;
        this.noduleResultMapper = noduleResultMapper;
        this.annotationResultMapper = annotationResultMapper;
    }

    @Override
    public AnnotationStudyVO getByStudyId(Long studyId) {
        List<CtFile> ctFiles = ctFileMapper.selectList(new LambdaQueryWrapper<CtFile>()
                .eq(CtFile::getStudyId, studyId)
                .orderByDesc(CtFile::getCreatedAt));
        AiTask aiTask = aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStudyId, studyId)
                .orderByDesc(AiTask::getCreatedAt)
                .last("limit 1"));
        if (aiTask == null) {
            return AnnotationStudyVO.builder()
                    .studyId(studyId)
                    .originalPreviewPath(null)
                    .annotatedPreviewPath(null)
                    .overlayPreviewPath(null)
                    .ctFiles(ctFiles)
                    .nodules(List.of())
                    .annotations(List.of())
                    .build();
        }
        List<NoduleResult> nodules = noduleResultMapper.selectList(new LambdaQueryWrapper<NoduleResult>()
                .eq(NoduleResult::getAiTaskId, aiTask.getId())
                .orderByAsc(NoduleResult::getNoduleNo));
        List<AnnotationResult> annotations = annotationResultMapper.selectList(new LambdaQueryWrapper<AnnotationResult>()
                .eq(AnnotationResult::getAiTaskId, aiTask.getId()));
        String segmentationPath = null;
        String originalPreviewPath = null;
        String annotatedPreviewPath = null;
        String overlayPreviewPath = null;
        if (aiTask.getResponseJson() != null) {
            AiPredictResponse response = JsonUtils.fromJson(aiTask.getResponseJson(), AiPredictResponse.class);
            segmentationPath = response.getSegmentationPath();
            String overlayPath = firstOverlayPath(response);
            originalPreviewPath = findPeerFigurePath(overlayPath, "pipeline_ct_slice.png");
            annotatedPreviewPath = findPeerFigurePath(overlayPath, "pipeline_annotated.png");
            overlayPreviewPath = findPeerFigurePath(overlayPath, "pipeline_overlay.png");
        }
        return AnnotationStudyVO.builder()
                .studyId(studyId)
                .segmentationPath(segmentationPath)
                .originalPreviewPath(originalPreviewPath)
                .annotatedPreviewPath(annotatedPreviewPath)
                .overlayPreviewPath(overlayPreviewPath)
                .ctFiles(ctFiles)
                .nodules(nodules)
                .annotations(annotations)
                .build();
    }

    private String firstOverlayPath(AiPredictResponse response) {
        if (response == null || response.getNodules() == null) {
            return null;
        }
        for (AiPredictResponse.Nodule nodule : response.getNodules()) {
            if (nodule == null || nodule.getAnnotations() == null) {
                continue;
            }
            for (AiPredictResponse.Annotation annotation : nodule.getAnnotations()) {
                if (annotation != null && annotation.getOverlayPath() != null && !annotation.getOverlayPath().isBlank()) {
                    return annotation.getOverlayPath().trim();
                }
            }
        }
        return null;
    }

    private String findPeerFigurePath(String overlayPath, String targetFilename) {
        if (overlayPath == null || overlayPath.isBlank()) {
            return null;
        }
        try {
            Path overlay = Path.of(overlayPath);
            Path parent = overlay.getParent();
            if (parent == null) {
                return null;
            }
            Path target = parent.resolve(targetFilename);
            if (!Files.exists(target)) {
                return null;
            }
            return target.toString().replace("\\", "/");
        } catch (Exception ignored) {
            return null;
        }
    }
}
