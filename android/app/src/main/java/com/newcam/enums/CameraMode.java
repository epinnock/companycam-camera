package com.newcam.enums;

/**
 * Created by dan on 12/18/17.
 */

public enum CameraMode {
    FASTCAM(0, "fastcam"),
    CAMERA(1, "camera"),
    SCANNER(2, "scanner");

    private final int intValue;
    private final String stringValue;

    CameraMode(int intValue, String stringValue) {
        this.intValue = intValue;
        this.stringValue = stringValue;
    }

    public int toInt() { return intValue; }

    @Override
    public String toString() { return stringValue; }

    public static CameraMode fromInt(int intValue) {
        for(CameraMode mode : CameraMode.values()) {
            if (mode.toInt() == intValue) { return mode; }
        }
        return CameraMode.CAMERA;
    }

    public static CameraMode fromString(String stringValue) {
        for(CameraMode mode : CameraMode.values()) {
            if (mode.toString().equals(stringValue)) { return mode; }
        }
        return CameraMode.CAMERA;
    }
}
