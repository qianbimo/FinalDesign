package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.config.StorageProperties;
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
import java.nio.file.Paths;
import java.util.List;

@Service
public class AnnotationServiceImpl implements AnnotationService {

    private final AiTaskMapper aiTaskMapper;
    private final CtFileMapper ctFileMapper;
    private final NoduleResultMapper noduleResultMapper;
    private final AnnotationResultMapper annotationResultMapper;
    private final StorageProperties storageProperties;

    public AnnotationServiceImpl(AiTaskMapper aiTaskMapper,
                                 CtFileMapper ctFileMapper,
                                 NoduleResultMapper noduleResultMapper,
                                 AnnotationResultMapper annotationResultMapper,
                                 StorageProperties storageProperties) {
        this.aiTaskMapper = aiTaskMapper;
        this.ctFileMapper = ctFileMapper;
        this.noduleResultMapper = noduleResultMapper;
        this.annotationResultMapper = annotationResultMapper;
        this.storageProperties = storageProperties;
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
            segmentationPath = normalizeToStorageRelative(response.getSegmentationPath());
            String overlayPath = normalizeToStorageRelative(firstOverlayPath(response));
            originalPreviewPath = resolveOriginalPreviewPath(overlayPath);
            annotatedPreviewPath = resolveAnnotatedPreviewPath(overlayPath);
            overlayPreviewPath = resolveOverlayPreviewPath(overlayPath);
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

    private String resolveOriginalPreviewPath(String overlayPath) {
        String pipelineCt = buildSiblingPath(overlayPath, "pipeline_ct_slice.png");
        String legacy = buildSiblingPath(overlayPath, "original_preview.png");
        return firstExistingPath(pipelineCt, legacy, overlayPath);
    }

    private String resolveAnnotatedPreviewPath(String overlayPath) {
        String pipelineAnnotated = buildSiblingPath(overlayPath, "pipeline_annotated.png");
        return firstExistingPath(pipelineAnnotated, overlayPath);
    }

    private String resolveOverlayPreviewPath(String overlayPath) {
        String pipelineOverlay = buildSiblingPath(overlayPath, "pipeline_overlay.png");
        return firstExistingPath(pipelineOverlay, overlayPath);
    }

    private String buildSiblingPath(String basePath, String targetFilename) {
        if (basePath == null || basePath.isBlank()) {
            return null;
        }
        String normalized = basePath.replace("\\", "/");
        int idx = normalized.lastIndexOf('/');
        if (idx < 0) {
            return null;
        }
        return normalized.substring(0, idx + 1) + targetFilename;
    }

    private String firstExistingPath(String... candidates) {
        for (String candidate : candidates) {
            if (existsUnderStorage(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean existsUnderStorage(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return true;
        }
        try {
            String normalized = normalizeToStorageRelative(path);
            if (normalized == null || normalized.isBlank()) {
                return false;
            }
            Path absolute = Paths.get(storageProperties.getBasePath(), normalized).normalize();
            return Files.exists(absolute);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeToStorageRelative(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String raw = path.trim().replace("\\", "/");

        if (raw.startsWith("/files/")) {
            raw = raw.substring("/files/".length());
        }
        if (raw.startsWith("ct/") || raw.startsWith("overlay/") || raw.startsWith("result/")) {
            return raw;
        }

        String[] anchors = {"/ct/", "/overlay/", "/result/"};
        for (String anchor : anchors) {
            int idx = raw.indexOf(anchor);
            if (idx >= 0) {
                return raw.substring(idx + 1);
            }
        }
        return raw;
    }
}
