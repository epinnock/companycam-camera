package com.newcam.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

/**
 * Created by dan on 3/2/17.
 */


public class CameraCheck {

    private static boolean FORCE_CAMERA_1 = true;

    public static boolean getCamera2Available(Context context) {
        boolean trueValue = android.os.Build.VERSION.SDK_INT >= 21 && CameraCheck.hasNonLegacyCamera(context);
        return FORCE_CAMERA_1 ? false : trueValue;
    }

    // This method checks if there's at least one non-LEGACY rear-facing camera available on this device
    @TargetApi(21)
    protected static boolean hasNonLegacyCamera(Context context) {

        boolean foundNonLegacyCamera = false;

        // At least SDK 21 is required to support the camera2 API
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        //CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);

                // Check if this is a rear-facing camera and it's hardware support level is greater than LEGACY
                if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    int deviceLevel = cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (deviceLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        foundNonLegacyCamera = true;
                    }
                }
            }
        }
        catch (CameraAccessException cae) {
            System.out.println("caught a CameraAccessException");
        }

        return foundNonLegacyCamera;
    }

}
