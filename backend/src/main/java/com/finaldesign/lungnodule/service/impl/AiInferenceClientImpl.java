package com.finaldesign.lungnodule.service.impl;

import com.finaldesign.lungnodule.config.StorageProperties;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class AiInferenceClientImpl implements AiInferenceClient {

    private final AiProperties aiProperties;
    private final StorageProperties storageProperties;
    private final RestTemplate restTemplate;

    public AiInferenceClientImpl(AiProperties aiProperties,
                                 StorageProperties storageProperties,
                                 RestTemplate restTemplate) {
        this.aiProperties = aiProperties;
        this.storageProperties = storageProperties;
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
            AiPredictResponse response = restTemplate.postForObject(url, entity, AiPredictResponse.class);
            if (response == null) {
                throw new BusinessException("AI response is empty");
            }
            return response;
        } catch (Exception e) {
            throw new BusinessException("调用AI推理服务失败: " + e.getMessage());
        }
    }

    private AiPredictResponse mockResponse(AiPredictRequest request) {
        Long studyId = request.getStudyId();
        prepareMockPreviewFiles(studyId);

        AiPredictResponse response = new AiPredictResponse();
        response.setTaskStatus("SUCCESS");
        response.setStudyId(studyId);
        response.setSegmentationPath("result/" + studyId + "/seg_mask.nii.gz");

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
        nodule.setMaskPath("result/" + studyId + "/nodule_1_mask.nii.gz");

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
        axial.setOverlayPath("overlay/" + studyId + "/nodule1_axial.png");
        axial.setColor("#FF0000");

        AiPredictResponse.Annotation coronal = new AiPredictResponse.Annotation();
        coronal.setViewType("CORONAL");
        coronal.setOverlayPath("overlay/" + studyId + "/nodule1_coronal.png");
        coronal.setColor("#00FF00");

        nodule.setAnnotations(List.of(axial, coronal));
        response.setNodules(List.of(nodule));
        return response;
    }

    private void prepareMockPreviewFiles(Long studyId) {
        Path base = Paths.get(storageProperties.getBasePath());
        Path resultDir = base.resolve("result").resolve(String.valueOf(studyId));
        Path overlayDir = base.resolve("overlay").resolve(String.valueOf(studyId));
        try {
            Files.createDirectories(resultDir);
            Files.createDirectories(overlayDir);
            writePng(resultDir.resolve("original_preview.png"), "CT 原始预览", new Color(30, 64, 175));
            writePng(overlayDir.resolve("nodule1_axial.png"), "AXIAL 标注", new Color(220, 38, 38));
            writePng(overlayDir.resolve("nodule1_coronal.png"), "CORONAL 标注", new Color(22, 163, 74));
        } catch (IOException e) {
            throw new BusinessException("生成 mock 可视化文件失败: " + e.getMessage());
        }
    }

    private void writePng(Path path, String text, Color primaryColor) throws IOException {
        int width = 640;
        int height = 420;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(10, 15, 25));
            g.fillRect(0, 0, width, height);

            g.setColor(primaryColor);
            g.fillRoundRect(35, 35, width - 70, height - 70, 20, 20);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.drawString(text, 70, 210);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
    }
}

