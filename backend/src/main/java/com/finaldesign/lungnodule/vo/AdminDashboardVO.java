package com.finaldesign.lungnodule.vo;

import lombok.Data;

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
}
