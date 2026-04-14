package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.dto.AdminUserCreateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.service.AdminService;
import com.finaldesign.lungnodule.utils.NoGenerator;
import com.finaldesign.lungnodule.vo.AdminDashboardVO;
import com.finaldesign.lungnodule.vo.AdminUserVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final CtStudyMapper ctStudyMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ReportRecordMapper reportRecordMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(SysUserMapper sysUserMapper,
                            PatientProfileMapper patientProfileMapper,
                            DoctorProfileMapper doctorProfileMapper,
                            CtStudyMapper ctStudyMapper,
                            AiTaskMapper aiTaskMapper,
                            ReportRecordMapper reportRecordMapper,
                            PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.reportRecordMapper = reportRecordMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public IPage<AdminUserVO> pageUsers(Long current, Long size, String role, Integer status, String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysUser::getCreatedAt);

        if (StringUtils.isNotBlank(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }

        IPage<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);
        List<Long> userIds = userPage.getRecords().stream().map(SysUser::getId).toList();

        Map<Long, Long> patientProfileMap = new HashMap<>();
        Map<Long, Long> doctorProfileMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<PatientProfile> patients = patientProfileMapper.selectList(new LambdaQueryWrapper<PatientProfile>()
                    .in(PatientProfile::getUserId, userIds));
            patientProfileMap.putAll(patients.stream()
                    .collect(Collectors.toMap(PatientProfile::getUserId, PatientProfile::getId)));

            List<DoctorProfile> doctors = doctorProfileMapper.selectList(new LambdaQueryWrapper<DoctorProfile>()
                    .in(DoctorProfile::getUserId, userIds));
            doctorProfileMap.putAll(doctors.stream()
                    .collect(Collectors.toMap(DoctorProfile::getUserId, DoctorProfile::getId)));
        }

        List<AdminUserVO> vos = userPage.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRole(user.getRole());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setStatus(user.getStatus());
            vo.setCreatedAt(user.getCreatedAt());
            if ("PATIENT".equals(user.getRole())) {
                vo.setProfileId(patientProfileMap.get(user.getId()));
            } else if ("DOCTOR".equals(user.getRole())) {
                vo.setProfileId(doctorProfileMap.get(user.getId()));
            }
            return vo;
        }).toList();

        Page<AdminUserVO> voPage = new Page<>(current, size, userPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(AdminUserCreateRequest request) {
        SysUser existed = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (existed != null) {
            throw new BusinessException(400, "Username already exists");
        }
        if (!"PATIENT".equals(request.getRole())
                && !"DOCTOR".equals(request.getRole())
                && !"ADMIN".equals(request.getRole())) {
            throw new BusinessException(400, "Invalid role");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sysUserMapper.insert(user);

        if ("PATIENT".equals(request.getRole())) {
            PatientProfile profile = new PatientProfile();
            profile.setUserId(user.getId());
            profile.setMedicalRecordNo(NoGenerator.medicalRecordNo());
            patientProfileMapper.insert(profile);
        } else if ("DOCTOR".equals(request.getRole())) {
            DoctorProfile profile = new DoctorProfile();
            profile.setUserId(user.getId());
            doctorProfileMapper.insert(profile);
        }
        return user.getId();
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "Status must be 0 or 1");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        String finalPassword = StringUtils.isBlank(newPassword) ? "123456" : newPassword;
        user.setPassword(passwordEncoder.encode(finalPassword));
        sysUserMapper.updateById(user);
    }

    @Override
    public AdminDashboardVO dashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setTotalUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()));
        vo.setPatientUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "PATIENT")));
        vo.setDoctorUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "DOCTOR")));
        vo.setAdminUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")));
        vo.setActiveUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1)));
        vo.setTotalStudies(ctStudyMapper.selectCount(new LambdaQueryWrapper<CtStudy>()));
        vo.setTotalAiTasks(aiTaskMapper.selectCount(new LambdaQueryWrapper<AiTask>()));
        vo.setTotalReports(reportRecordMapper.selectCount(new LambdaQueryWrapper<ReportRecord>()));
        return vo;
    }
}
