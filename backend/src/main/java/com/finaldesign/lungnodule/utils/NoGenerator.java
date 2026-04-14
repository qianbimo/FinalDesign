package com.finaldesign.lungnodule.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class NoGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGenerator() {
    }

    public static String studyNo() {
        return "ST" + LocalDateTime.now().format(FORMATTER) + shortUuid();
    }

    public static String taskNo() {
        return "AI" + LocalDateTime.now().format(FORMATTER) + shortUuid();
    }

    public static String medicalRecordNo() {
        return "MR" + LocalDateTime.now().format(FORMATTER) + shortUuid();
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
