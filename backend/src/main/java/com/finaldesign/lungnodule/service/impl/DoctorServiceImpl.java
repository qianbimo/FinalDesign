package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.DoctorProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.service.DoctorService;
import com.finaldesign.lungnodule.vo.DoctorPatientVO;
import com.finaldesign.lungnodule.vo.DoctorStudyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorProfileMapper doctorProfileMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final CtStudyMapper ctStudyMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public DoctorServiceImpl(DoctorProfileMapper doctorProfileMapper,
                             PatientProfileMapper patientProfileMapper,
                             CtStudyMapper ctStudyMapper,
                             SysUserMapper sysUserMapper,
                             PasswordEncoder passwordEncoder) {
        this.doctorProfileMapper = doctorProfileMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public DoctorProfile getProfileByUserId(Long userId) {
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(404, "Doctor profile not found");
        }
        return profile;
    }

    @Override
    public void updateProfile(Long userId, DoctorProfileUpdateRequest request) {
        DoctorProfile profile = getProfileByUserId(userId);
        BeanUtils.copyProperties(request, profile);
        doctorProfileMapper.updateById(profile);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "Old password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(400, "New password cannot be the same as old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    @Override
    public IPage<DoctorPatientVO> pagePatients(Long doctorUserId, Long current, Long size) {
        LambdaQueryWrapper<PatientProfile> queryWrapper = new LambdaQueryWrapper<PatientProfile>();
        if (doctorUserId != null) {
            DoctorProfile doctorProfile = getProfileByUserId(doctorUserId);
            queryWrapper.inSql(
                    PatientProfile::getId,
                    "select distinct patient_id from registration_record " +
                            "where doctor_id = " + doctorProfile.getId() + " and status <> 'CANCELLED'"
            );
        }
        queryWrapper.orderByDesc(PatientProfile::getCreatedAt);

        Page<PatientProfile> page = new Page<>(current, size);
        IPage<PatientProfile> profilePage = patientProfileMapper.selectPage(page, queryWrapper);

        Map<Long, String> patientNameMap = buildUserNameMap(profilePage.getRecords().stream()
                .map(PatientProfile::getUserId)
                .toList());

        List<DoctorPatientVO> records = profilePage.getRecords().stream()
                .map(profile -> {
                    DoctorPatientVO vo = new DoctorPatientVO();
                    Integer resolvedAge = resolveAgeByBirthday(profile.getBirthday(), profile.getAge());
                    if (resolvedAge != null && !resolvedAge.equals(profile.getAge())) {
                        profile.setAge(resolvedAge);
                        patientProfileMapper.updateById(profile);
                    }
                    vo.setId(profile.getId());
                    vo.setUserId(profile.getUserId());
                    vo.setPatientName(patientNameMap.get(profile.getUserId()));
                    vo.setGender(profile.getGender());
                    vo.setAge(resolvedAge);
                    vo.setBirthday(profile.getBirthday());
                    vo.setMedicalRecordNo(profile.getMedicalRecordNo());
                    vo.setAddress(profile.getAddress());
                    return vo;
                })
                .toList();

        Page<DoctorPatientVO> voPage = new Page<>(current, size, profilePage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public IPage<DoctorStudyVO> pageDoctorStudies(Long doctorUserId, Long current, Long size) {
        DoctorProfile doctorProfile = getProfileByUserId(doctorUserId);
        Page<CtStudy> page = new Page<>(current, size);
        IPage<CtStudy> studyPage = ctStudyMapper.selectPage(page, new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getDoctorId, doctorProfile.getId())
                .orderByDesc(CtStudy::getCreatedAt));

        List<Long> patientIds = studyPage.getRecords().stream().map(CtStudy::getPatientId).distinct().toList();
        Map<Long, Long> patientUserIdMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<PatientProfile> patientProfiles = patientProfileMapper.selectBatchIds(patientIds);
            for (PatientProfile patientProfile : patientProfiles) {
                patientUserIdMap.put(patientProfile.getId(), patientProfile.getUserId());
            }
        }

        Map<Long, String> userNameMap = buildUserNameMap(patientUserIdMap.values().stream().distinct().toList());

        List<DoctorStudyVO> records = studyPage.getRecords().stream()
                .map(study -> {
                    DoctorStudyVO vo = new DoctorStudyVO();
                    vo.setId(study.getId());
                    vo.setStudyNo(study.getStudyNo());
                    vo.setPatientId(study.getPatientId());
                    String patientName = userNameMap.get(patientUserIdMap.get(study.getPatientId()));
                    if (patientName == null || patientName.isBlank()) {
                        patientName = resolvePatientName(study.getPatientId());
                    }
                    vo.setPatientName(patientName);
                    vo.setDoctorId(study.getDoctorId());
                    vo.setStudyDate(study.getStudyDate());
                    vo.setStudyDesc(study.getStudyDesc());
                    vo.setStatus(study.getStatus());
                    return vo;
                })
                .toList();

        Page<DoctorStudyVO> voPage = new Page<>(current, size, studyPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public DoctorStudyVO getPatientStudyDetail(Long patientId, Long studyId) {
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null || !study.getPatientId().equals(patientId)) {
            throw new BusinessException(404, "妫€鏌ヨ褰曚笉瀛樺湪");
        }

        DoctorStudyVO vo = new DoctorStudyVO();
        vo.setId(study.getId());
        vo.setStudyNo(study.getStudyNo());
        vo.setPatientId(study.getPatientId());
        vo.setDoctorId(study.getDoctorId());
        vo.setStudyDate(study.getStudyDate());
        vo.setStudyDesc(study.getStudyDesc());
        vo.setStatus(study.getStatus());

        vo.setPatientName(resolvePatientName(study.getPatientId()));
        return vo;
    }

    private String resolvePatientName(Long patientId) {
        if (patientId == null) {
            return null;
        }

        PatientProfile patientProfile = patientProfileMapper.selectById(patientId);
        if (patientProfile != null && patientProfile.getUserId() != null) {
            SysUser patientUser = sysUserMapper.selectById(patientProfile.getUserId());
            String displayName = extractDisplayName(patientUser);
            if (displayName != null) {
                return displayName;
            }
        }

        // 兼容历史数据：部分数据可能直接保存了 sys_user.id 到 patient_id 字段
        SysUser fallbackUser = sysUserMapper.selectById(patientId);
        String fallbackName = extractDisplayName(fallbackUser);
        if (fallbackName != null) {
            return fallbackName;
        }
        return null;
    }

    private String extractDisplayName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return null;
    }

    private Map<Long, String> buildUserNameMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, String> userNameMap = new HashMap<>();
        for (SysUser user : users) {
            userNameMap.put(user.getId(), user.getRealName());
        }
        return userNameMap;
    }

    private Integer resolveAgeByBirthday(LocalDate birthday, Integer fallbackAge) {
        if (birthday == null) {
            return fallbackAge;
        }
        if (birthday.isAfter(LocalDate.now())) {
            return fallbackAge;
        }
        return Period.between(birthday, LocalDate.now()).getYears();
    }
}

