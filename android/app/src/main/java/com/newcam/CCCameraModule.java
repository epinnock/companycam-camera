package com.newcam;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.newcam.utils.AppPreferences;
import com.newcam.utils.CameraCheck;

/**
 * Created by dan on 1/30/17.
 */

public class CCCameraModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    public static final String MODULE_NAME = "CCCameraModule";

    //TODO: only re-acquire the camera on onHostResume if onHostPause has occurred
    private boolean hostHasPausedWithoutResume = false;
    private boolean isActive = false;

    public CCCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    private void printDebug(String message){
        System.err.println("[CCCameraModule.java] " + message);
    }

    @Override
    public void onHostResume() {
        printDebug("onHostResume called");
        CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null) {
            printDebug("No CCCameraView instance; failed");
            return;
        }

        if(isActive && hostHasPausedWithoutResume){
            hostHasPausedWithoutResume = false;
            camView.startCamera(); //createPreview(); startPreview();
            printDebug("startCamera IS being called");
        }else{
            printDebug("startCamera is NOT being called");
        }
    }

    @Override
    public void onHostPause() {
        printDebug("onHostPause called");
        CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null) {
            printDebug("No CCCameraView instance; failed");
            return;
        }

        camView.releaseCamera();
        hostHasPausedWithoutResume = true;
    }

    @Override
    public void onHostDestroy() {
        printDebug("onHostDestroy called");
        CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null) {
            printDebug("No CCCameraView instance; failed");
            return;
        }

        camView.releaseCamera();
    }

    @ReactMethod
    public void setActive() {
        printDebug("setActive called");
        /*CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null){
            printDebug("No CCCameraView instance; failed");
            return;
        }
        camView.startCamera();*/

        isActive = true;
        printDebug("Active status: " + (isActive ? "Active" : "Inactive"));
    }

    @ReactMethod
    public void setInactive() {
        printDebug("setInactive called");
        /*CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null) {
            printDebug("No CCCameraView instance; failed");
            return;
        }
        camView.releaseCamera();*/

        isActive = false;
        printDebug("Active status: " + (isActive ? "Active" : "Inactive"));
    }

    @ReactMethod
    public void setForceCamera1(Boolean val) {
        AppPreferences.setForceCamera1(getReactApplicationContext(), val);
    }

    @ReactMethod
    public void getForceCamera1(Callback handleResult) {
        boolean val = AppPreferences.getForceCamera1(getReactApplicationContext());
        handleResult.invoke(val);
    }

    @ReactMethod
    public void getCamera2Available(Callback handleResult) {
        boolean val = CameraCheck.getCamera2Available(getReactApplicationContext());
        handleResult.invoke(val);
    }
}