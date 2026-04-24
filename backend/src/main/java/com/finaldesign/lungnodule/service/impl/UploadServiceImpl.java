package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> SINGLE_FILE_EXTENSIONS = Set.of("dcm", "nii", "nii.gz");
    private static final Set<String> MHD_RAW_EXTENSIONS = Set.of("mhd", "raw");

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
    public List<CtUploadResponseVO> uploadCtFiles(Long studyId, List<MultipartFile> files, Long uploadUserId) {
        List<MultipartFile> uploadFiles = normalizeFiles(files);
        if (uploadFiles.isEmpty()) {
            throw new BusinessException(400, "No upload files found");
        }

        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null) {
            throw new BusinessException(404, "Study record not found");
        }

        validateUploadSet(uploadFiles);

        List<CtFile> existingFiles = ctFileMapper.selectList(new LambdaQueryWrapper<CtFile>()
                .eq(CtFile::getStudyId, studyId));
        removeExistingFiles(existingFiles);
        ctFileMapper.delete(new LambdaQueryWrapper<CtFile>().eq(CtFile::getStudyId, studyId));

        String relativeDir = String.format("ct/%s/%s", study.getPatientId(), study.getStudyNo());
        Path dirPath = Paths.get(storageProperties.getBasePath(), relativeDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dirPath);
            clearDirectory(dirPath);
        } catch (IOException e) {
            throw new BusinessException("Failed to prepare upload directory: " + e.getMessage());
        }

        List<CtUploadResponseVO> uploaded = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (MultipartFile file : uploadFiles) {
            String originalFilename = safeFileName(file.getOriginalFilename());
            String fileType = FileTypeUtils.detectCtFileType(originalFilename);
            if (fileType == null) {
                throw new BusinessException(400, "Unsupported CT file type");
            }

            Path targetPath = resolvePathInDir(dirPath, originalFilename);
            try {
                file.transferTo(targetPath.toFile());
            } catch (IOException e) {
                throw new BusinessException("Failed to save file: " + e.getMessage());
            }

            String relativePath = relativeDir + "/" + originalFilename;
            String accessUrl = storageProperties.getAccessPrefix() + "/" + relativePath.replace("\\", "/");

            CtFile ctFile = new CtFile();
            ctFile.setStudyId(studyId);
            ctFile.setFileName(originalFilename);
            ctFile.setFileType(fileType);
            ctFile.setFilePath(targetPath.toString().replace("\\", "/"));
            ctFile.setFileSize(file.getSize());
            ctFile.setUploadUserId(uploadUserId);
            ctFile.setUploadTime(now);
            ctFile.setCheckStatus("VALID");
            ctFileMapper.insert(ctFile);

            uploaded.add(CtUploadResponseVO.builder()
                    .fileId(ctFile.getId())
                    .studyId(studyId)
                    .fileName(originalFilename)
                    .fileType(fileType)
                    .filePath(ctFile.getFilePath())
                    .fileSize(file.getSize())
                    .accessUrl(accessUrl.replace("//", "/"))
                    .build());
        }

        study.setStatus("UPLOADED");
        ctStudyMapper.updateById(study);
        return uploaded;
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(item -> item != null && !item.isEmpty())
                .collect(Collectors.toList());
    }

    private void validateUploadSet(List<MultipartFile> files) {
        if (files.size() > 2) {
            throw new BusinessException(400, "Only one file or one .mhd + .raw pair is allowed per study");
        }

        for (MultipartFile file : files) {
            validateSingleFile(file);
        }

        List<String> exts = files.stream()
                .map(this::getNormalizedExtension)
                .collect(Collectors.toList());

        if (files.size() == 1) {
            String ext = exts.get(0);
            if (MHD_RAW_EXTENSIONS.contains(ext)) {
                throw new BusinessException(400, "A single .mhd or .raw file is not allowed, upload both together");
            }
            if (!SINGLE_FILE_EXTENSIONS.contains(ext)) {
                throw new BusinessException(400, "Only .dcm/.nii/.nii.gz or matched .mhd+.raw is supported");
            }
            return;
        }

        Set<String> set = new HashSet<>(exts);
        if (!set.equals(MHD_RAW_EXTENSIONS)) {
            throw new BusinessException(400, "Two-file upload must be exactly one .mhd and one .raw");
        }

        String baseA = getBaseName(safeFileName(files.get(0).getOriginalFilename()));
        String baseB = getBaseName(safeFileName(files.get(1).getOriginalFilename()));
        if (!baseA.equalsIgnoreCase(baseB)) {
            throw new BusinessException(400, ".mhd and .raw must have the same base filename");
        }
    }

    private void validateSingleFile(MultipartFile file) {
        Integer maxSizeMb = storageProperties.getMaxSizeMb();
        if (maxSizeMb != null && file.getSize() > maxSizeMb * 1024L * 1024L) {
            throw new BusinessException(400, "File size exceeds limit");
        }
        String safeName = safeFileName(file.getOriginalFilename());
        String extension = getExtension(safeName);
        if (StringUtils.isBlank(extension)) {
            throw new BusinessException(400, "Invalid filename extension");
        }
        List<String> allowed = storageProperties.getAllowedExtensions();
        if (allowed == null || allowed.stream().noneMatch(item -> extension.equalsIgnoreCase(item))) {
            throw new BusinessException(400, "Unsupported file extension");
        }
    }

    private String getNormalizedExtension(MultipartFile file) {
        return getExtension(safeFileName(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
    }

    private String getExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".nii.gz")) {
            return "nii.gz";
        }
        int idx = lower.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return lower.substring(idx + 1);
    }

    private String getBaseName(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".nii.gz")) {
            return filename.substring(0, filename.length() - 7);
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0) {
            return filename;
        }
        return filename.substring(0, idx);
    }

    private String safeFileName(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new BusinessException(400, "Invalid file name");
        }
        String name = Paths.get(originalFilename).getFileName().toString();
        if (StringUtils.isBlank(name) || ".".equals(name) || "..".equals(name)) {
            throw new BusinessException(400, "Invalid file name");
        }
        return name;
    }

    private Path resolvePathInDir(Path dirPath, String fileName) {
        Path resolved = dirPath.resolve(fileName).normalize().toAbsolutePath();
        Path normalizedDir = dirPath.toAbsolutePath().normalize();
        if (!resolved.startsWith(normalizedDir)) {
            throw new BusinessException(400, "Illegal file path");
        }
        return resolved;
    }

    private void clearDirectory(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            return;
        }
        try (var stream = Files.walk(dirPath)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(dirPath))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private void removeExistingFiles(List<CtFile> existingFiles) {
        if (existingFiles == null || existingFiles.isEmpty()) {
            return;
        }
        Path storageRoot = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
        for (CtFile item : existingFiles) {
            String path = item.getFilePath();
            if (StringUtils.isBlank(path)) {
                continue;
            }
            try {
                Path candidate = Paths.get(path).toAbsolutePath().normalize();
                if (!candidate.startsWith(storageRoot)) {
                    continue;
                }
                Files.deleteIfExists(candidate);
            } catch (Exception ignored) {
            }
        }
    }
}
