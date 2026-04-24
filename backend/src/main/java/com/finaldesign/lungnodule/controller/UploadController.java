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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class UploadController {

    private final UploadService uploadService;
    private final StudyAccessGuard studyAccessGuard;

    public UploadController(UploadService uploadService, StudyAccessGuard studyAccessGuard) {
        this.uploadService = uploadService;
        this.studyAccessGuard = studyAccessGuard;
    }

    @PostMapping("/ct")
    @Operation(summary = "Upload CT files (single file or matched .mhd + .raw pair)")
    public Result<List<CtUploadResponseVO>> uploadCt(@RequestParam("studyId") Long studyId,
                                                     @RequestPart(value = "files", required = false) MultipartFile[] files,
                                                     @RequestPart(value = "file", required = false) MultipartFile file) {
        studyAccessGuard.assertCurrentUserCanManageStudy(studyId);

        List<MultipartFile> payload = new ArrayList<>();
        if (files != null) {
            payload.addAll(Arrays.asList(files));
        }
        if (file != null) {
            payload.add(file);
        }

        return Result.success(uploadService.uploadCtFiles(studyId, payload, CurrentUserUtil.userId()));
    }
}
