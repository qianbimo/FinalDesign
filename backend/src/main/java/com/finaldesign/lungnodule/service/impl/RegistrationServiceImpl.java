package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.RegistrationCreateRequest;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.RegistrationRecordMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.service.RegistrationService;
import com.finaldesign.lungnodule.vo.RegistrationDoctorOptionVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRecordMapper registrationRecordMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final SysUserMapper sysUserMapper;

    public RegistrationServiceImpl(RegistrationRecordMapper registrationRecordMapper,
                                   DoctorProfileMapper doctorProfileMapper,
                                   SysUserMapper sysUserMapper) {
        this.registrationRecordMapper = registrationRecordMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<RegistrationDoctorOptionVO> listAvailableDoctors() {
        List<DoctorProfile> doctorProfiles = doctorProfileMapper.selectList(new LambdaQueryWrapper<DoctorProfile>()
                .orderByDesc(DoctorProfile::getCreatedAt));
        if (doctorProfiles.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = doctorProfiles.stream().map(DoctorProfile::getUserId).distinct().toList();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getRole, "DOCTOR")
                .eq(SysUser::getStatus, 1));
        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser user : users) {
            userMap.put(user.getId(), user);
        }

        return doctorProfiles.stream()
                .filter(profile -> userMap.containsKey(profile.getUserId()))
                .map(profile -> {
                    SysUser user = userMap.get(profile.getUserId());
                    RegistrationDoctorOptionVO vo = new RegistrationDoctorOptionVO();
                    vo.setDoctorId(profile.getId());
                    vo.setUserId(user.getId());
                    vo.setRealName(user.getRealName());
                    vo.setDepartment(profile.getDepartment());
                    vo.setTitle(profile.getTitle());
                    return vo;
                })
                .toList();
    }

    @Override
    public Long create(RegistrationCreateRequest request) {
        DoctorProfile doctorProfile = doctorProfileMapper.selectById(request.getDoctorId());
        if (doctorProfile == null) {
            throw new BusinessException(400, "Doctor profile not found");
        }
        SysUser doctorUser = sysUserMapper.selectById(doctorProfile.getUserId());
        if (doctorUser == null || doctorUser.getStatus() == null || doctorUser.getStatus() != 1) {
            throw new BusinessException(400, "Doctor profile not found");
        }

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
    public void updateStatus(Long id, String status, Long operatorDoctorId) {
        RegistrationRecord record = registrationRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "Registration record not found");
        }
        if (operatorDoctorId != null && !Objects.equals(record.getDoctorId(), operatorDoctorId)) {
            throw new BusinessException(403, "Access denied");
        }
        record.setStatus(status);
        registrationRecordMapper.updateById(record);
    }
}
