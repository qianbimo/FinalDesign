package com.finaldesign.lungnodule.service.impl;

import com.finaldesign.lungnodule.config.StorageProperties;
import com.finaldesign.lungnodule.entity.CtFile;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.CtFileMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.service.UploadService;
import com.finaldesign.lungnodule.utils.FileTypeUtils;
import com.finaldesign.lungnodule.vo.CtUploadResponseVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StorageProperties storageProperties;
    private final CtStudyMapper ctStudyMapper;
    private final CtFileMapper ctFileMapper;

    public UploadServiceImpl(StorageProperties storageProperties,
                             CtStudyMapper ctStudyMapper,
                             CtFileMapper ctFileMapper) {
        this.storageProperties = storageProperties;
        this.ctStudyMapper = ctStudyMapper;
        this.ctFileMapper = ctFileMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CtUploadResponseVO uploadCtFile(Long studyId, MultipartFile file, Long uploadUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null) {
            throw new BusinessException(404, "检查记录不存在");
        }
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String filename = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;

        String relativeDir = String.format("ct/%s/%s/%s", study.getPatientId(), study.getStudyNo(),
                LocalDateTime.now().format(DATE_FORMATTER));
        Path dirPath = Paths.get(storageProperties.getBasePath(), relativeDir);
        Path targetPath = dirPath.resolve(filename);
        try {
            Files.createDirectories(dirPath);
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        String relativePath = relativeDir + "/" + filename;
        String fileType = FileTypeUtils.detectCtFileType(originalFilename);

        CtFile ctFile = new CtFile();
        ctFile.setStudyId(studyId);
        ctFile.setFileName(originalFilename);
        ctFile.setFileType(fileType);
        ctFile.setFilePath(targetPath.toString().replace("\\", "/"));
        ctFile.setFileSize(file.getSize());
        ctFile.setUploadUserId(uploadUserId);
        ctFile.setUploadTime(LocalDateTime.now());
        ctFile.setCheckStatus("VALID");
        ctFileMapper.insert(ctFile);

        if (!"UPLOADED".equalsIgnoreCase(study.getStatus())) {
            study.setStatus("UPLOADED");
            ctStudyMapper.updateById(study);
        }

        String accessUrl = storageProperties.getAccessPrefix() + "/" + relativePath.replace("\\", "/");
        return CtUploadResponseVO.builder()
                .fileId(ctFile.getId())
                .studyId(studyId)
                .fileName(originalFilename)
                .fileType(fileType)
                .filePath(ctFile.getFilePath())
                .fileSize(file.getSize())
                .accessUrl(accessUrl.replace("//", "/"))
                .build();
    }

    private void validateFile(MultipartFile file) {
        Integer maxSizeMb = storageProperties.getMaxSizeMb();
        if (maxSizeMb != null && file.getSize() > maxSizeMb * 1024L * 1024L) {
            throw new BusinessException(400, "文件大小超过限制: " + maxSizeMb + "MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new BusinessException(400, "文件名非法");
        }
        String extension = getExtension(originalFilename);
        List<String> allowed = storageProperties.getAllowedExtensions();
        if (allowed == null || !allowed.contains(extension.toLowerCase())) {
            throw new BusinessException(400, "不支持的文件类型，仅允许: .dcm/.nii/.nii.gz");
        }
    }

    private String getExtension(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".nii.gz")) {
            return "nii.gz";
        }
        int idx = lower.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return lower.substring(idx + 1);
    }
}
