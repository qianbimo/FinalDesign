package com.finaldesign.lungnodule.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CtUploadResponseVO {
    private Long fileId;
    private Long studyId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private String accessUrl;
}
