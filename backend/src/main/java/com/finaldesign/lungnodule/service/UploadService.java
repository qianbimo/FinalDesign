package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.vo.CtUploadResponseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {
    List<CtUploadResponseVO> uploadCtFiles(Long studyId, List<MultipartFile> files, Long uploadUserId);
}
