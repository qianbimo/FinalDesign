package com.finaldesign.lungnodule.service.impl;

import com.finaldesign.lungnodule.config.AiProperties;
import com.finaldesign.lungnodule.dto.AiPredictRequest;
import com.finaldesign.lungnodule.dto.AiPredictResponse;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.service.AiInferenceClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AiInferenceClientImpl implements AiInferenceClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;

    public AiInferenceClientImpl(AiProperties aiProperties, RestTemplate restTemplate) {
        this.aiProperties = aiProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public AiPredictResponse predict(AiPredictRequest request) {
        if (Boolean.TRUE.equals(aiProperties.getMockEnabled())) {
            return mockResponse(request);
        }
        String url = aiProperties.getBaseUrl() + aiProperties.getPredictPath();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AiPredictRequest> entity = new HttpEntity<>(request, headers);
            return restTemplate.postForObject(url, entity, AiPredictResponse.class);
        } catch (Exception e) {
            throw new BusinessException("调用AI推理服务失败: " + e.getMessage());
        }
    }

    private AiPredictResponse mockResponse(AiPredictRequest request) {
        AiPredictResponse response = new AiPredictResponse();
        response.setTaskStatus("SUCCESS");
        response.setStudyId(request.getStudyId());
        response.setSegmentationPath("result/" + request.getStudyId() + "/seg_mask.nii.gz");

        AiPredictResponse.Summary summary = new AiPredictResponse.Summary();
        summary.setNoduleCount(1);
        summary.setOverallRisk("MEDIUM");
        summary.setDiagnosisSuggestion("建议进一步随访观察");
        response.setSummary(summary);

        AiPredictResponse.Nodule nodule = new AiPredictResponse.Nodule();
        nodule.setNoduleNo(1);
        nodule.setCenterX(120.0);
        nodule.setCenterY(155.0);
        nodule.setCenterZ(48.0);
        nodule.setWidth(18.5);
        nodule.setHeight(16.2);
        nodule.setDepth(14.8);
        nodule.setVolume(1520.3);
        nodule.setDiameterMm(17.6);
        nodule.setMalignancyProb(0.73);
        nodule.setRiskLevel("HIGH");
        nodule.setDescription("右上肺实性结节");
        nodule.setMaskPath("result/" + request.getStudyId() + "/nodule_1_mask.nii.gz");

        AiPredictResponse.Bbox bbox = new AiPredictResponse.Bbox();
        bbox.setX1(110.0);
        bbox.setY1(146.0);
        bbox.setZ1(40.0);
        bbox.setX2(130.0);
        bbox.setY2(164.0);
        bbox.setZ2(56.0);
        nodule.setBbox(bbox);

        AiPredictResponse.Annotation axial = new AiPredictResponse.Annotation();
        axial.setViewType("AXIAL");
        axial.setOverlayPath("overlay/" + request.getStudyId() + "/nodule1_axial.png");
        axial.setColor("#FF0000");
        AiPredictResponse.Annotation coronal = new AiPredictResponse.Annotation();
        coronal.setViewType("CORONAL");
        coronal.setOverlayPath("overlay/" + request.getStudyId() + "/nodule1_coronal.png");
        coronal.setColor("#00FF00");
        nodule.setAnnotations(List.of(axial, coronal));

        response.setNodules(List.of(nodule));
        return response;
    }
}
