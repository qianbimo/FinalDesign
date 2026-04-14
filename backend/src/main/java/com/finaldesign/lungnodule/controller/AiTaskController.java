package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-task")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/start/{studyId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "启动AI分析")
    public Result<Map<String, Long>> start(@PathVariable Long studyId) {
        Long taskId = aiTaskService.startTask(studyId);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "查询AI任务状态")
    public Result<AiTask> task(@PathVariable Long taskId) {
        return Result.success(aiTaskService.getTask(taskId));
    }

    @GetMapping("/study/{studyId}/result")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "查询某次检查的AI结果")
    public Result<Map<String, Object>> studyResult(@PathVariable Long studyId) {
        return Result.success(aiTaskService.getStudyResult(studyId));
    }
}
