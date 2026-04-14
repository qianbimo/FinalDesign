package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task")
public class AiTask extends BaseEntity {
    @TableId
    private Long id;
    private Long studyId;
    private String taskNo;
    private String modelVersion;
    private String taskStatus;
    private String requestJson;
    private String responseJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
}
