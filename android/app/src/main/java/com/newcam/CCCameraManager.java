package com.newcam;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.newcam.views.NewCameraView;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraManager extends SimpleViewManager<CCCameraView> {

    public static final String REACT_CLASS = "CompanyCamCamera";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected CCCameraView createViewInstance(ThemedReactContext context) {
        //TODO can conditionally return any of the views extending CCCameraView
        return new NewCameraView(context);
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
}
