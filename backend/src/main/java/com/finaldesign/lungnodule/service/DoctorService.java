package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.dto.DoctorProfileUpdateRequest;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.vo.DoctorPatientVO;
import com.finaldesign.lungnodule.vo.DoctorStudyVO;

public interface DoctorService {
    DoctorProfile getProfileByUserId(Long userId);

    void updateProfile(Long userId, DoctorProfileUpdateRequest request);

    IPage<DoctorPatientVO> pagePatients(Long current, Long size);

    IPage<DoctorStudyVO> pageDoctorStudies(Long doctorUserId, Long current, Long size);

    CtStudy getPatientStudyDetail(Long patientId, Long studyId);
}
