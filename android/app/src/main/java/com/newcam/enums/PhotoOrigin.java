package com.newcam.enums;

/**
 * Created by dan on 11/20/17.
 */

public enum PhotoOrigin {
    STANDARD_CAMERA("STANDARD_CAMERA"),
    STANDARD_CAMERA_FASTCAM("STANDARD_CAMERA_FASTCAM"),
    STANDARD_CAMERA_DOCSCAN("STANDARD_CAMERA_DOCSCAN");

    private final String stringValue;

    PhotoOrigin(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    public static PhotoOrigin fromCameraMode(CameraMode mode) {
        switch (mode) {
            case FASTCAM:
                return PhotoOrigin.STANDARD_CAMERA_FASTCAM;
            case SCANNER:
                return PhotoOrigin.STANDARD_CAMERA_DOCSCAN;
            case CAMERA:
            default:
                return PhotoOrigin.STANDARD_CAMERA;
        }

    }
}
