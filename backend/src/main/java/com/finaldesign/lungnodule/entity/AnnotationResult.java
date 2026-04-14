package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("annotation_result")
public class AnnotationResult extends BaseEntity {
    @TableId
    private Long id;
    private Long studyId;
    private Long aiTaskId;
    private Long noduleResultId;
    private String viewType;
    private String overlayPath;
    private String contourJson;
    private String color;
    private Integer visibleFlag;
}
