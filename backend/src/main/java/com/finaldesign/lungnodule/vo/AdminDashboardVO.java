package com.finaldesign.lungnodule.vo;

import lombok.Data;

import java.util.Map;

@Data
public class AdminDashboardVO {
    private Long totalUsers;
    private Long patientUsers;
    private Long doctorUsers;
    private Long adminUsers;
    private Long activeUsers;
    private Long totalStudies;
    private Long totalAiTasks;
    private Long totalReports;
    private Map<String, Long> studyStatusStats;
    private Map<String, Long> reportStatusStats;
}
