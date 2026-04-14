package com.finaldesign.lungnodule.service;

import com.finaldesign.lungnodule.dto.AiPredictRequest;
import com.finaldesign.lungnodule.dto.AiPredictResponse;

public interface AiInferenceClient {
    AiPredictResponse predict(AiPredictRequest request);
}
