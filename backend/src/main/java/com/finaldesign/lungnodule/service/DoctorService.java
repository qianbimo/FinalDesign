package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.DoctorProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.vo.DoctorPatientVO;
import com.finaldesign.lungnodule.vo.DoctorStudyVO;

public interface DoctorService {
    DoctorProfile getProfileByUserId(Long userId);

    void updateProfile(Long userId, DoctorProfileUpdateRequest request);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    IPage<DoctorPatientVO> pagePatients(Long doctorUserId, Long current, Long size);

    IPage<DoctorStudyVO> pageDoctorStudies(Long doctorUserId, Long current, Long size, String patientName, String status);

    DoctorStudyVO getPatientStudyDetail(Long patientId, Long studyId);
}
