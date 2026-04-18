package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorProfileMapper doctorProfileMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final CtStudyMapper ctStudyMapper;
    private final SysUserMapper sysUserMapper;

    public DoctorServiceImpl(DoctorProfileMapper doctorProfileMapper,
                             PatientProfileMapper patientProfileMapper,
                             CtStudyMapper ctStudyMapper,
                             SysUserMapper sysUserMapper) {
        this.doctorProfileMapper = doctorProfileMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.ctStudyMapper = ctStudyMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public DoctorProfile getProfileByUserId(Long userId) {
        DoctorProfile profile = doctorProfileMapper.selectOne(new LambdaQueryWrapper<DoctorProfile>()
                .eq(DoctorProfile::getUserId, userId));
        if (profile == null) {
            throw new BusinessException(404, "医生档案不存在");
        }
        return profile;
    }

    @Override
    public IPage<DoctorPatientVO> pagePatients(Long current, Long size) {
        Page<PatientProfile> page = new Page<>(current, size);
        IPage<PatientProfile> profilePage = patientProfileMapper.selectPage(page, new LambdaQueryWrapper<PatientProfile>()
                .orderByDesc(PatientProfile::getCreatedAt));

        Map<Long, String> patientNameMap = buildUserNameMap(profilePage.getRecords().stream()
                .map(PatientProfile::getUserId)
                .toList());

        List<DoctorPatientVO> records = profilePage.getRecords().stream()
                .map(profile -> {
                    DoctorPatientVO vo = new DoctorPatientVO();
                    vo.setId(profile.getId());
                    vo.setUserId(profile.getUserId());
                    vo.setPatientName(patientNameMap.get(profile.getUserId()));
                    vo.setGender(profile.getGender());
                    vo.setAge(profile.getAge());
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
                    vo.setPatientName(userNameMap.get(patientUserIdMap.get(study.getPatientId())));
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
    public CtStudy getPatientStudyDetail(Long patientId, Long studyId) {
        CtStudy study = ctStudyMapper.selectById(studyId);
        if (study == null || !study.getPatientId().equals(patientId)) {
            throw new BusinessException(404, "检查记录不存在");
        }
        return study;
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
}
