package com.finaldesign.lungnodule.vo;

import com.finaldesign.lungnodule.entity.AnnotationResult;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.entity.NoduleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnnotationStudyVO {
    private Long studyId;
    private String segmentationPath;
    private String originalPreviewPath;
    private String annotatedPreviewPath;
    private List<CtFile> ctFiles;
    private List<NoduleResult> nodules;
    private List<AnnotationResult> annotations;
}
