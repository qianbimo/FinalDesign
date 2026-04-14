package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.StudyCreateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;

public interface CtStudyService {
    Long create(StudyCreateRequest request);

    IPage<CtStudy> pageList(Long current, Long size, Long patientId, Long doctorId);

    CtStudy detail(Long id);
}
