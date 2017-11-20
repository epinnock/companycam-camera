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

    public static PhotoOrigin fromCameraMode(String cameraMode) {
        if (cameraMode.equals("camera")) {
            return PhotoOrigin.STANDARD_CAMERA;
        } else if (cameraMode.equals("fastcam")) {
            return PhotoOrigin.STANDARD_CAMERA_FASTCAM;
        } else if (cameraMode.equals("scanner")) {
            return PhotoOrigin.STANDARD_CAMERA_DOCSCAN;
        } else {
            return PhotoOrigin.STANDARD_CAMERA;
        }
    }
}
