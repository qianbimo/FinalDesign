package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.RegistrationCreateRequest;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.vo.RegistrationDoctorOptionVO;

import java.util.List;

public interface RegistrationService {
    List<RegistrationDoctorOptionVO> listAvailableDoctors();

    Long create(RegistrationCreateRequest request);

    IPage<RegistrationRecord> list(Long current, Long size, Long doctorId);

    void updateStatus(Long id, String status, Long operatorDoctorId);
}
