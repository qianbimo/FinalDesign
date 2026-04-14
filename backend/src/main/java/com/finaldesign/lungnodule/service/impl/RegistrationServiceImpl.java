package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.RegistrationCreateRequest;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.RegistrationRecordMapper;
import com.finaldesign.lungnodule.service.RegistrationService;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRecordMapper registrationRecordMapper;

    public RegistrationServiceImpl(RegistrationRecordMapper registrationRecordMapper) {
        this.registrationRecordMapper = registrationRecordMapper;
    }

    @Override
    public Long create(RegistrationCreateRequest request) {
        RegistrationRecord record = new RegistrationRecord();
        record.setPatientId(request.getPatientId());
        record.setDoctorId(request.getDoctorId());
        record.setAppointmentTime(request.getAppointmentTime());
        record.setStatus("PENDING");
        record.setDescription(request.getDescription());
        registrationRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public IPage<RegistrationRecord> list(Long current, Long size, Long doctorId) {
        Page<RegistrationRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<RegistrationRecord> wrapper = new LambdaQueryWrapper<RegistrationRecord>()
                .orderByDesc(RegistrationRecord::getCreatedAt);
        if (doctorId != null) {
            wrapper.eq(RegistrationRecord::getDoctorId, doctorId);
        }
        return registrationRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateStatus(Long id, String status) {
        RegistrationRecord record = registrationRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "挂号记录不存在");
        }
        record.setStatus(status);
        registrationRecordMapper.updateById(record);
    }
}
