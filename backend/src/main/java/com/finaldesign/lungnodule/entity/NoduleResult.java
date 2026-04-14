package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nodule_result")
public class NoduleResult extends BaseEntity {
    @TableId
    private Long id;
    private Long studyId;
    private Long aiTaskId;
    private Integer noduleNo;
    private Double centerX;
    private Double centerY;
    private Double centerZ;
    private Double width;
    private Double height;
    private Double depth;
    private Double volume;
    private Double diameterMm;
    private Double malignancyProb;
    private String riskLevel;
    private String description;
    private String maskPath;
    private String bboxJson;
}
