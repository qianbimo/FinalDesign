package com.finaldesign.lungnodule.utils;

public class FileTypeUtils {

    private FileTypeUtils() {
    }

    public static String detectCtFileType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".nii.gz")) {
            return "NII_GZ";
        }
        if (lower.endsWith(".nii")) {
            return "NII";
        }
        if (lower.endsWith(".dcm")) {
            return "DCM";
        }
        if (lower.endsWith(".mhd")) {
            return "MHD";
        }
        if (lower.endsWith(".raw")) {
            return "RAW";
        }
        if (lower.endsWith(".png")) {
            return "PNG";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "JPG";
        }
        return null;
    }
}
