package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.vo.CtUploadResponseVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    CtUploadResponseVO uploadCtFile(Long studyId, MultipartFile file, Long uploadUserId);
}
