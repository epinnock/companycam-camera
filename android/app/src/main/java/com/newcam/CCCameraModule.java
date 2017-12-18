package com.newcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.newcam.utils.AppPreferences;
import com.newcam.utils.CameraCheck;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by dan on 1/30/17.
 */

public class CCCameraModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    public static final String MODULE_NAME = "CCCameraModule";

    public static final int CC_CAMERA_FLASH_OFF = 0;
    public static final int CC_CAMERA_FLASH_ON = 1;
    public static final int CC_CAMERA_FLASH_AUTO = 2;
    public static final int CC_CAMERA_FLASH_TORCH = 3;

    public static final int CC_CAMERA_MODE_FASTCAM = 0;
    public static final int CC_CAMERA_MODE_CAMERA = 1;
    public static final int CC_CAMERA_MODE_SCANNER = 2;

    public static final int CC_RESOLUTION_MODE_NORMAL = 0;
    public static final int CC_RESOLUTION_MODE_HIGH = 1;
    public static final int CC_RESOLUTION_MODE_SUPER = 2;

    private ReactApplicationContext mContext;

    //TODO: only re-acquire the camera on onHostResume if onHostPause has occurred
    private boolean hostHasPausedWithoutResume = false;
    private boolean isActive = false;

    public CCCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("FlashMode", getFlashModeConstants());
                put("CameraMode", getCameraModeConstants());
                put("ResolutionMode", getResolutionModeConstants());
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", CC_CAMERA_FLASH_OFF);
                        put("on", CC_CAMERA_FLASH_ON);
                        put("auto", CC_CAMERA_FLASH_AUTO);
                        put("torch", CC_CAMERA_FLASH_TORCH);
                    }
                });
            }

            private Map<String, Object> getCameraModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("fastcam", CC_CAMERA_MODE_FASTCAM);
                        put("photo", CC_CAMERA_MODE_CAMERA);
                        put("scanner", CC_CAMERA_MODE_SCANNER);
                    }
                });
            }

            private Map<String, Object> getResolutionModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("normal", CC_RESOLUTION_MODE_NORMAL);
                        put("high", CC_RESOLUTION_MODE_HIGH);
                        put("super", CC_RESOLUTION_MODE_SUPER);
                    }
                });
            }
        });
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

        // Register to receive a broadcast when the volume button is pressed
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver, new IntentFilter("VolumeButtonBroadcast"));

        // Send a broadcast to let the MainActivity know that the CCCameraModule is active

        // Create an intent for the broadcast
        Intent intent = new Intent("CCCameraModuleBroadcast");

        // Add the "active" parameter to the intent
        intent.putExtra("active", true);

        // Send the broadcast locally
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
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

        // Unregister any broadcast receivers
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);

        // Send a broadcast to let the MainActivity know that the CCCameraModule is inactive

        // Create an intent for the broadcast
        Intent intent = new Intent("CCCameraModuleBroadcast");

        // Add the "active" parameter to the intent
        intent.putExtra("active", false);

        // Send the broadcast locally
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
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

    // This object responds to broadcasts
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // CCCameraModule activation or deactivation
            if (intent.getAction().equals("VolumeButtonBroadcast")) {

                // Take a photo when the volume button is pressed as long as this view is active.  The broadcast shouldn't be sent
                // if the view is inactive, but double check to avoid errors.
                if (isActive) {

                    // Get a reference to the current CCCameraView
                    CCCameraView camView = CCCameraManager.getLatestView();
                    if(camView == null) {
                        printDebug("No CCCameraView instance; failed");
                        return;
                    }

                    // Take a photo as long as the camera reference isn't null
                    if (camView.mCamera != null) {
                        camView.mCamera.takePicture();
                    }
                }
            }
        }
    };
}
