package com.finaldesign.lungnodule.dto;

import lombok.Data;

@Data
public class ReportUpdateRequest {
    private String reportTitle;
    private String reportContent;
    private String reportSummary;
}
