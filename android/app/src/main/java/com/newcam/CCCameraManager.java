package com.newcam;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.newcam.enums.CameraMode;
import com.newcam.enums.FlashMode;
import com.newcam.enums.ResolutionMode;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraManager extends ViewGroupManager<CCCameraView> {

    public static final String REACT_CLASS = "CompanyCamCamera";

    private static CCCameraView latestView;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected CCCameraView createViewInstance(ThemedReactContext context) {
        latestView = new CCCameraView(context);
        return latestView;
    }

    public static CCCameraView getLatestView(){
        return latestView;
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
    public void setExifLocTimestamp(CCCameraView view, double val){
        view.setExifLocTimestamp(val);
    }

    @ReactProp(name = "hideNativeUI")
    public void setHideNativeUI(CCCameraView view, boolean val){
        view.setHideNativeUI(val);
    }

    @ReactProp(name = "flashMode")
    public void setFlashMode(CCCameraView view, int val){
        view.setFlashMode(FlashMode.fromInt(val));
    }

    @ReactProp(name = "cameraMode")
    public void setCameraMode(CCCameraView view, int val){
        view.setCameraMode(CameraMode.fromInt(val));
    }

    @ReactProp(name = "resolutionMode")
    public void setResolutionMode(CCCameraView view, int val){
        view.setResolutionMode(ResolutionMode.fromInt(val));
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
            "onClose",
            MapBuilder.of("registrationName", "onClose"),
            "onPhotoAccepted",
            MapBuilder.of("registrationName", "onPhotoAccepted"),
            "onPhotoTaken",
            MapBuilder.of("registrationName", "onPhotoTaken")
        );
    }
}
