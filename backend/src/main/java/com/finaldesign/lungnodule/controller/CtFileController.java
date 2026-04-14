package com.finaldesign.lungnodule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.mapper.CtFileMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ct-file")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class CtFileController {

    private final CtFileMapper ctFileMapper;

    public CtFileController(CtFileMapper ctFileMapper) {
        this.ctFileMapper = ctFileMapper;
    }

    @GetMapping("/study/{studyId}")
    @Operation(summary = "查询检查CT文件列表")
    public Result<List<CtFile>> listByStudy(@PathVariable Long studyId) {
        List<CtFile> files = ctFileMapper.selectList(new LambdaQueryWrapper<CtFile>()
                .eq(CtFile::getStudyId, studyId)
                .orderByDesc(CtFile::getCreatedAt));
        return Result.success(files);
    }
}
