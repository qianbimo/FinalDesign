package com.finaldesign.lungnodule.controller;

import com.finaldesign.lungnodule.common.Result;
import com.finaldesign.lungnodule.security.CurrentUserUtil;
import com.finaldesign.lungnodule.security.StudyAccessGuard;
import com.finaldesign.lungnodule.service.UploadService;
import com.finaldesign.lungnodule.vo.CtUploadResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class UploadController {

    private final UploadService uploadService;
    private final StudyAccessGuard studyAccessGuard;

    public UploadController(UploadService uploadService, StudyAccessGuard studyAccessGuard) {
        this.uploadService = uploadService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @PostMapping("/ct")
    @Operation(summary = "上传 CT 文件")
    public Result<CtUploadResponseVO> uploadCt(@RequestParam("studyId") Long studyId,
                                               @RequestPart("file") MultipartFile file) {
        studyAccessGuard.assertCurrentUserCanAccessStudy(studyId);
        return Result.success(uploadService.uploadCtFile(studyId, file, CurrentUserUtil.userId()));
    }
}
