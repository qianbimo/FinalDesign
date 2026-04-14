package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.RegistrationCreateRequest;
import com.finaldesign.lungnodule.entity.RegistrationRecord;

public interface RegistrationService {
    Long create(RegistrationCreateRequest request);

    IPage<RegistrationRecord> list(Long current, Long size, Long doctorId);

    void updateStatus(Long id, String status);
}
