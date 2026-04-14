package com.finaldesign.lungnodule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String moduleName;
    private String operationType;
    private String operationDesc;
    private String requestPath;
    private String requestMethod;
    private String ip;
}
