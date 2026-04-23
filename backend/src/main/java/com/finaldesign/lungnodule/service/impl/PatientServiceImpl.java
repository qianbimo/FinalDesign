package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.PatientProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.RegistrationRecordMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.service.PatientService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientProfileMapper patientProfileMapper;
    private final CtStudyMapper ctStudyMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ReportRecordMapper reportRecordMapper;
    private final RegistrationRecordMapper registrationRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public PatientServiceImpl(PatientProfileMapper patientProfileMapper,
                              CtStudyMapper ctStudyMapper,
                              AiTaskMapper aiTaskMapper,
                              ReportRecordMapper reportRecordMapper,
                              RegistrationRecordMapper registrationRecordMapper,
                              SysUserMapper sysUserMapper,
                              PasswordEncoder passwordEncoder) {
        this.patientProfileMapper = patientProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.reportRecordMapper = reportRecordMapper;
        this.registrationRecordMapper = registrationRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PatientProfile getProfileByUserId(Long userId) {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(404, "患者档案不存在");
        }

        Integer resolvedAge = resolveAgeByBirthday(profile.getBirthday(), profile.getAge());
        if (!Objects.equals(resolvedAge, profile.getAge())) {
            profile.setAge(resolvedAge);
            patientProfileMapper.updateById(profile);
        }
        return profile;
    }

    @Override
    public void updateProfile(Long userId, PatientProfileUpdateRequest request) {
        PatientProfile profile = getProfileByUserId(userId);
        profile.setGender(request.getGender());
        profile.setBirthday(request.getBirthday());
        profile.setAge(resolveAgeByBirthday(request.getBirthday()));
        profile.setIdCard(request.getIdCard());
        profile.setAddress(request.getAddress());
        profile.setRemark(request.getRemark());
        patientProfileMapper.updateById(profile);
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
    public IPage<CtStudy> pageStudies(Long userId, Long current, Long size, boolean includeCancelled) {
        PatientProfile profile = getProfileByUserId(userId);
        if (!includeCancelled) {
            Page<CtStudy> page = new Page<>(current, size);
            return ctStudyMapper.selectPage(page, new LambdaQueryWrapper<CtStudy>()
                    .eq(CtStudy::getPatientId, profile.getId())
                    .orderByDesc(CtStudy::getCreatedAt));
        }

        List<CtStudy> studies = ctStudyMapper.selectList(new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getPatientId, profile.getId())
                .orderByDesc(CtStudy::getCreatedAt));

        List<RegistrationRecord> cancelledRegistrations = registrationRecordMapper.selectList(
                new LambdaQueryWrapper<RegistrationRecord>()
                        .eq(RegistrationRecord::getPatientId, profile.getId())
                        .eq(RegistrationRecord::getStatus, "CANCELLED")
                        .orderByDesc(RegistrationRecord::getCreatedAt)
        );

        List<CtStudy> merged = new ArrayList<>(studies);
        for (RegistrationRecord registration : cancelledRegistrations) {
            CtStudy cancelledItem = new CtStudy();
            cancelledItem.setId(-registration.getId());
            cancelledItem.setStudyNo("REG-CANCELLED-" + registration.getId());
            cancelledItem.setPatientId(registration.getPatientId());
            cancelledItem.setDoctorId(registration.getDoctorId());
            if (registration.getAppointmentTime() != null) {
                cancelledItem.setStudyDate(registration.getAppointmentTime().toLocalDate());
            }
            cancelledItem.setStudyDesc(registration.getDescription());
            cancelledItem.setStatus("CANCELLED");
            cancelledItem.setCreatedAt(registration.getCreatedAt());
            cancelledItem.setUpdatedAt(registration.getUpdatedAt());
            merged.add(cancelledItem);
        }

        merged.sort(Comparator.comparing(CtStudy::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        long total = merged.size();
        int from = Math.max(0, (int) ((current - 1) * size));
        int to = Math.min((int) total, from + size.intValue());
        List<CtStudy> records = from >= to ? List.of() : merged.subList(from, to);

        Page<CtStudy> page = new Page<>(current, size);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    @Override
    public CtStudy getStudyDetail(Long userId, Long studyId) {
        PatientProfile profile = getProfileByUserId(userId);
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null || !study.getPatientId().equals(profile.getId())) {
            throw new BusinessException(404, "检查记录不存在");
        }
        return study;
    }

    @Override
    public AiTask getStudyAiResult(Long userId, Long studyId) {
        getStudyDetail(userId, studyId);
        return aiTaskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getStudyId, studyId)
                .orderByDesc(AiTask::getCreatedAt)
                .last("limit 1"));
    }

    @Override
    public ReportRecord getStudyReport(Long userId, Long studyId) {
        getStudyDetail(userId, studyId);
        return reportRecordMapper.selectOne(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getStudyId, studyId)
                .orderByDesc(ReportRecord::getVersionNo)
                .last("limit 1"));
    }

    private Integer resolveAgeByBirthday(LocalDate birthday) {
        if (birthday == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        if (birthday.isAfter(today)) {
            throw new BusinessException(400, "出生日期不能晚于当前日期");
        }

        return Period.between(birthday, today).getYears();
    }

    private Integer resolveAgeByBirthday(LocalDate birthday, Integer fallbackAge) {
        if (birthday == null) {
            return fallbackAge;
        }

        LocalDate today = LocalDate.now();
        if (birthday.isAfter(today)) {
            return fallbackAge;
        }

        return Period.between(birthday, today).getYears();
    }
}
