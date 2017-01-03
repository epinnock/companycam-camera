package com.newcam;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.newcam.views.Camera2View;
import com.newcam.views.NewCameraView;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraManager extends SimpleViewManager<CCCameraView> {

    public ThemedReactContext mContext;
    public static final String REACT_CLASS = "CompanyCamCamera";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected CCCameraView createViewInstance(ThemedReactContext context) {
        mContext = context;

        // Return the appropriate view class according to the device's version and available cameras
        if (android.os.Build.VERSION.SDK_INT >= 21 && hasNonLegacyCamera()) {
            return new Camera2View(context);
        }
        else {
            return new NewCameraView(context);
        }
    }

    @ReactProp(name = "storagePath")
    public void setStoragePath(CCCameraView view, String str){
        view.setStoragePath(str);
    }

    @ReactProp(name = "projectName")
    public void setProjectName(CCCameraView view, String str){
        view.setProjectName(str);
    }

    @ReactProp(name = "projectAddress")
    public void setProjectAddress(CCCameraView view, String str){
        view.setProjectAddress(str);
    }

    @ReactProp(name = "exifLat")
    public void setExifLat(CCCameraView view, double val){
        view.setExifLat(val);
    }

    @ReactProp(name = "exifLon")
    public void setExifLon(CCCameraView view, double val){
        view.setExifLon(val);
    }

    @ReactProp(name = "exifLocTimestamp")
    public void setExifLocTimestamp(CCCameraView view, long val){
        view.setExifLocTimestamp(val);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
            "onClose",
            MapBuilder.of("registrationName", "onClose"),
            "photoAccepted",
            MapBuilder.of("registrationName", "photoAccepted"),
            "photoTaken",
            MapBuilder.of("registrationName", "photoTaken")
        );
    }

    // This method checks if there's at least one non-LEGACY rear-facing camera available on this device
    @TargetApi(21)
    public boolean hasNonLegacyCamera() {

        boolean foundNonLegacyCamera = false;

        // At least SDK 21 is required to support the camera2 API
        CameraManager manager = (CameraManager) mContext.getCurrentActivity().getSystemService(Context.CAMERA_SERVICE);
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
