package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.mapper.CtFileMapper;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ct-file")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class CtFileController {

    private final CtFileMapper ctFileMapper;
    private final StudyAccessGuard studyAccessGuard;

    public CtFileController(CtFileMapper ctFileMapper, StudyAccessGuard studyAccessGuard) {
        this.ctFileMapper = ctFileMapper;
        this.studyAccessGuard = studyAccessGuard;
    }

    @GetMapping("/study/{studyId}")
    @Operation(summary = "查询检查 CT 文件列表")
    public Result<List<CtFile>> listByStudy(@PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        List<CtFile> files = ctFileMapper.selectList(new LambdaQueryWrapper<CtFile>()
                .eq(CtFile::getStudyId, studyId)
                .orderByDesc(CtFile::getCreatedAt));
        return Result.success(files);
    }
}
