package com.newcam;

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
        if (mode == CameraMode.CAMERA) {
            return PhotoOrigin.STANDARD_CAMERA;
        } else if (mode == CameraMode.FASTCAM) {
            return PhotoOrigin.STANDARD_CAMERA_FASTCAM;
        } else if (mode == CameraMode.SCANNER) {
            return PhotoOrigin.STANDARD_CAMERA_DOCSCAN;
        } else {
            return PhotoOrigin.STANDARD_CAMERA;
        }
    }
}
