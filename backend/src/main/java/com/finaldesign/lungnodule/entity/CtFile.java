package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ct_file")
public class CtFile extends BaseEntity {
    @TableId
    private Long id;
    private Long studyId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Integer sliceCount;
    private Long uploadUserId;
    @TableField("upload_time")
    private LocalDateTime uploadTime;
    private String checkStatus;
    private String remark;
}
