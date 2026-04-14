package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.dto.AiPredictResponse;
import com.finaldesign.lungnodule.entity.AiTask;
import com.finaldesign.lungnodule.entity.CtStudy;
import com.finaldesign.lungnodule.entity.ReportRecord;

public interface ReportGenerateService {
    ReportRecord generateDraft(CtStudy study, AiTask aiTask, AiPredictResponse response);
}
