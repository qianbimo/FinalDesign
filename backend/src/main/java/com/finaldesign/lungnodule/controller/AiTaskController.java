package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-task")
public class AiTaskController {

    private final AiTaskService aiTaskService;
    private final StudyAccessGuard studyAccessGuard;

    public AiTaskController(AiTaskService aiTaskService, StudyAccessGuard studyAccessGuard) {
        this.aiTaskService = aiTaskService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @PostMapping("/start/{studyId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "启动 AI 分析")
    public Result<Map<String, Long>> start(@PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanManageStudy(studyId);
        Long taskId = aiTaskService.startTask(studyId);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "查询 AI 任务状态")
    public Result<AiTask> task(@PathVariable Long taskId) {
        AiTask task = aiTaskService.getTask(taskId);
        studyAccessGuard.assertCurrentUserCanAccessStudy(task.getStudyId());
        return Result.success(task);
    }

    @GetMapping("/study/{studyId}/result")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "查询某次检查的 AI 结果")
    public Result<Map<String, Object>> studyResult(@PathVariable Long studyId) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        return Result.success(aiTaskService.getStudyResult(studyId));
    }
}
