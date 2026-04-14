package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.vo.AnnotationStudyVO;

public interface AnnotationService {
    AnnotationStudyVO getByStudyId(Long studyId);
}
