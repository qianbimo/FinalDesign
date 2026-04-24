package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finaldesign.lungnodule.entity.Admin;
import com.finaldesign.lungnodule.dto.AdminUserCreateRequest;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.RegistrationRecord;
import com.finaldesign.lungnodule.entity.ReportRecord;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.AiTaskMapper;
import com.finaldesign.lungnodule.mapper.AdminMapper;
import com.finaldesign.lungnodule.mapper.CtStudyMapper;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.RegistrationRecordMapper;
import com.finaldesign.lungnodule.mapper.ReportRecordMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.service.AdminService;
import com.finaldesign.lungnodule.utils.NoGenerator;
import com.finaldesign.lungnodule.vo.AdminDashboardVO;
import com.finaldesign.lungnodule.vo.AdminReportVO;
import com.finaldesign.lungnodule.vo.AdminUserVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final AdminMapper adminMapper;
    private final RegistrationRecordMapper registrationRecordMapper;
    private final CtStudyMapper ctStudyMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ReportRecordMapper reportRecordMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(SysUserMapper sysUserMapper,
                            PatientProfileMapper patientProfileMapper,
                            DoctorProfileMapper doctorProfileMapper,
                            AdminMapper adminMapper,
                            RegistrationRecordMapper registrationRecordMapper,
                            CtStudyMapper ctStudyMapper,
                            AiTaskMapper aiTaskMapper,
                            ReportRecordMapper reportRecordMapper,
                            PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.adminMapper = adminMapper;
        this.registrationRecordMapper = registrationRecordMapper;
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
        Map<Long, Long> adminProfileMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<PatientProfile> patients = patientProfileMapper.selectList(new LambdaQueryWrapper<PatientProfile>()
                    .in(PatientProfile::getUserId, userIds));
            patientProfileMap.putAll(patients.stream()
                    .collect(Collectors.toMap(PatientProfile::getUserId, PatientProfile::getId)));

            List<DoctorProfile> doctors = doctorProfileMapper.selectList(new LambdaQueryWrapper<DoctorProfile>()
                    .in(DoctorProfile::getUserId, userIds));
            doctorProfileMap.putAll(doctors.stream()
                    .collect(Collectors.toMap(DoctorProfile::getUserId, DoctorProfile::getId)));

            List<Admin> admins = adminMapper.selectList(new LambdaQueryWrapper<Admin>()
                    .in(Admin::getUserId, userIds));
            adminProfileMap.putAll(admins.stream()
                    .collect(Collectors.toMap(Admin::getUserId, Admin::getId)));
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
            } else if ("ADMIN".equals(user.getRole())) {
                vo.setProfileId(adminProfileMap.get(user.getId()));
            }
            return vo;
        }).toList();

        Page<AdminUserVO> voPage = new Page<>(current, size, userPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public IPage<AdminReportVO> pageReports(Long current, Long size, String status, String keyword) {
        Page<ReportRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<ReportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReportRecord::getCreatedAt);

        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(ReportRecord::getStatus, status);
        }

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ReportRecord::getReportTitle, keyword)
                    .or().like(ReportRecord::getReportSummary, keyword));
        }

        IPage<ReportRecord> reportPage = reportRecordMapper.selectPage(page, wrapper);
        List<ReportRecord> records = reportPage.getRecords();

        List<Long> studyIds = records.stream()
                .map(ReportRecord::getStudyId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<Long> patientIds = records.stream()
                .map(ReportRecord::getPatientId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<Long> doctorIds = records.stream()
                .map(ReportRecord::getDoctorId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> studyNoMap = new HashMap<>();
        if (!studyIds.isEmpty()) {
            List<CtStudy> studies = ctStudyMapper.selectBatchIds(studyIds);
            studyNoMap.putAll(studies.stream().collect(Collectors.toMap(CtStudy::getId, CtStudy::getStudyNo)));
        }

        Map<Long, Long> patientUserIdMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<PatientProfile> patients = patientProfileMapper.selectBatchIds(patientIds);
            patientUserIdMap.putAll(patients.stream().collect(Collectors.toMap(PatientProfile::getId, PatientProfile::getUserId)));
        }

        Map<Long, Long> doctorUserIdMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            List<DoctorProfile> doctors = doctorProfileMapper.selectBatchIds(doctorIds);
            doctorUserIdMap.putAll(doctors.stream().collect(Collectors.toMap(DoctorProfile::getId, DoctorProfile::getUserId)));
        }

        List<Long> relatedUserIds = java.util.stream.Stream.concat(
                        patientUserIdMap.values().stream(),
                        doctorUserIdMap.values().stream())
                .distinct()
                .toList();

        Map<Long, String> userNameMap = new HashMap<>();
        if (!relatedUserIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(relatedUserIds);
            userNameMap.putAll(users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName)));
        }

        List<AdminReportVO> vos = records.stream().map(report -> {
            AdminReportVO vo = new AdminReportVO();
            vo.setId(report.getId());
            vo.setStudyId(report.getStudyId());
            vo.setStudyNo(studyNoMap.get(report.getStudyId()));
            vo.setPatientId(report.getPatientId());
            vo.setPatientName(userNameMap.get(patientUserIdMap.get(report.getPatientId())));
            vo.setDoctorId(report.getDoctorId());
            vo.setDoctorName(userNameMap.get(doctorUserIdMap.get(report.getDoctorId())));
            vo.setAiTaskId(report.getAiTaskId());
            vo.setReportTitle(report.getReportTitle());
            vo.setReportSummary(report.getReportSummary());
            vo.setStatus(report.getStatus());
            vo.setVersionNo(report.getVersionNo());
            vo.setGeneratedBy(report.getGeneratedBy());
            vo.setAuditTime(report.getAuditTime());
            vo.setCreatedAt(report.getCreatedAt());
            vo.setUpdatedAt(report.getUpdatedAt());
            return vo;
        }).toList();

        Page<AdminReportVO> voPage = new Page<>(current, size, reportPage.getTotal());
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
        } else if ("ADMIN".equals(request.getRole())) {
            Admin admin = new Admin();
            admin.setUserId(user.getId());
            adminMapper.insert(admin);
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
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long currentUserId, Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException(400, "Cannot delete current user");
        }

        String role = user.getRole();
        if ("PATIENT".equals(role)) {
            deletePatientUser(userId);
        } else if ("DOCTOR".equals(role)) {
            deleteDoctorUser(userId);
        } else if ("ADMIN".equals(role)) {
            deleteAdminUser(userId);
        } else {
            throw new BusinessException(400, "Invalid role");
        }

        sysUserMapper.deleteById(userId);
    }

    @Override
    public void deleteReport(Long reportId) {
        ReportRecord report = reportRecordMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "Report not found");
        }
        reportRecordMapper.deleteById(reportId);
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
        vo.setStudyStatusStats(queryStudyStatusStats());
        vo.setReportStatusStats(queryReportStatusStats());
        return vo;
    }

    private Map<String, Long> queryStudyStatusStats() {
        List<CtStudy> studies = ctStudyMapper.selectList(new LambdaQueryWrapper<CtStudy>()
                .select(CtStudy::getStatus));
        return studies.stream()
                .collect(Collectors.groupingBy(
                        item -> StringUtils.defaultIfBlank(item.getStatus(), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private Map<String, Long> queryReportStatusStats() {
        List<ReportRecord> reports = reportRecordMapper.selectList(new LambdaQueryWrapper<ReportRecord>()
                .select(ReportRecord::getStatus));
        return reports.stream()
                .collect(Collectors.groupingBy(
                        item -> StringUtils.defaultIfBlank(item.getStatus(), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private void deletePatientUser(Long userId) {
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId));
        if (profile == null) {
            return;
        }

        Long registrationCount = registrationRecordMapper.selectCount(new LambdaQueryWrapper<RegistrationRecord>()
                .eq(RegistrationRecord::getPatientId, profile.getId()));
        Long studyCount = ctStudyMapper.selectCount(new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getPatientId, profile.getId()));
        Long reportCount = reportRecordMapper.selectCount(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getPatientId, profile.getId()));
        if (registrationCount > 0 || studyCount > 0 || reportCount > 0) {
            throw new BusinessException(400, "User has related records and cannot be deleted");
        }

        patientProfileMapper.deleteById(profile.getId());
    }

    private void deleteDoctorUser(Long userId) {
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, userId));
        if (profile == null) {
            return;
        }

        Long registrationCount = registrationRecordMapper.selectCount(new LambdaQueryWrapper<RegistrationRecord>()
                .eq(RegistrationRecord::getDoctorId, profile.getId()));
        Long studyCount = ctStudyMapper.selectCount(new LambdaQueryWrapper<CtStudy>()
                .eq(CtStudy::getDoctorId, profile.getId()));
        Long reportCount = reportRecordMapper.selectCount(new LambdaQueryWrapper<ReportRecord>()
                .eq(ReportRecord::getDoctorId, profile.getId()));
        if (registrationCount > 0 || studyCount > 0 || reportCount > 0) {
            throw new BusinessException(400, "User has related records and cannot be deleted");
        }

        doctorProfileMapper.deleteById(profile.getId());
    }

    private void deleteAdminUser(Long userId) {
        Long adminCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ADMIN"));
        if (adminCount <= 1) {
            throw new BusinessException(400, "Cannot delete last admin");
        }

        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUserId, userId));
        if (admin != null) {
            adminMapper.deleteById(admin.getId());
        }
    }
}
