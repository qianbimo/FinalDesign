package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.service.AnnotationService;
import com.finaldesign.lungnodule.vo.AnnotationStudyVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/annotation")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class AnnotationController {

    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @GetMapping("/study/{studyId}")
    @Operation(summary = "查询某次检查的标注数据")
    public Result<AnnotationStudyVO> getByStudy(@PathVariable Long studyId) {
        return Result.success(annotationService.getByStudyId(studyId));
    }
}
