package com.finaldesign.lungnodule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.entity.PatientProfile;

public interface DoctorService {
    DoctorProfile getProfileByUserId(Long userId);

    IPage<PatientProfile> pagePatients(Long current, Long size);

    IPage<CtStudy> pageDoctorStudies(Long doctorUserId, Long current, Long size);

    CtStudy getPatientStudyDetail(Long patientId, Long studyId);
}
