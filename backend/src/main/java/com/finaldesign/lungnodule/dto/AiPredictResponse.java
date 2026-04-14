package com.finaldesign.lungnodule.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiPredictResponse {
    private String taskStatus;
    private Long studyId;
    private String segmentationPath;
    private Summary summary;
    private List<Nodule> nodules;

    @Data
    public static class Summary {
        private Integer noduleCount;
        private String overallRisk;
        private String diagnosisSuggestion;
    }

    @Data
    public static class Nodule {
        private Integer noduleNo;
        private Double centerX;
        private Double centerY;
        private Double centerZ;
        private Double width;
        private Double height;
        private Double depth;
        private Double volume;
        private Double diameterMm;
        private Double malignancyProb;
        private String riskLevel;
        private String description;
        private String maskPath;
        private Bbox bbox;
        private List<Annotation> annotations;
    }

    @Data
    public static class Bbox {
        private Double x1;
        private Double y1;
        private Double z1;
        private Double x2;
        private Double y2;
        private Double z2;
    }

    @Data
    public static class Annotation {
        private String viewType;
        private String overlayPath;
        private String color;
    }
}
