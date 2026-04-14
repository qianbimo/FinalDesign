package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.entity.AiTask;

import java.util.Map;

public interface AiTaskService {
    Long startTask(Long studyId);

    AiTask getTask(Long taskId);

    Map<String, Object> getStudyResult(Long studyId);
}
