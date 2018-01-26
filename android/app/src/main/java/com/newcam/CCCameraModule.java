package com.newcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.newcam.enums.CameraMode;
import com.newcam.enums.FlashMode;
import com.newcam.enums.ResolutionMode;
import com.newcam.jniexports.JNIExports;
import com.newcam.utils.AppPreferences;
import com.newcam.utils.CameraCheck;
import com.newcam.utils.ImageprocState;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by dan on 1/30/17.
 */

public class CCCameraModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    public static final String MODULE_NAME = "CCCameraModule";

    private ReactApplicationContext mContext;

    //TODO: only re-acquire the camera on onHostResume if onHostPause has occurred
    private boolean hostHasPausedWithoutResume = false;
    private boolean isActive = false;

    public CCCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        reactContext.addLifecycleEventListener(this);
    }

    // =============================================================================================
    // This is a utility function to use OpenCV to do some image processing.
    // It is used in the editor.  So why are they here in the camera project?
    //   1. The camera project already has OpenCV in it
    //   2. The functionality might be used directly in the scanner
    // =============================================================================================
    @ReactMethod
    void imageprocRender(String inputAbsolutePath, String outputAbsolutePath, ReadableMap optsMap, Promise promise) {

        // Read the ReadableMap
        ImageprocState opts = new ImageprocState();
        try {
            opts.loadReadableMap(optsMap);
        } catch (IllegalArgumentException e) {
            promise.reject("CCCameraModule", e.getMessage());
            return;
        }

        // Debug output
        System.out.println("[CCCamera] Filenames for imageprocRender:");
        System.out.println("[CCCamera] - inputAbsolutePath: '" + inputAbsolutePath + "'");
        System.out.println("[CCCamera] - outputAbsolutePath: '" + outputAbsolutePath + "'");
        opts.debugPrint("[CCCamera]");

        // Prepare input and current Bitmaps
        // Each step will apply an effect to bitmapCurrent and set bitmapCurrent to the new result.
        Bitmap bitmapInput = BitmapFactory.decodeFile(inputAbsolutePath);
        if (bitmapInput == null) {
            promise.reject("CCCameraModule", "BitmapFactory.decodeFile returned null");
            return;
        }
        Bitmap bitmapCurrent = bitmapInput;

        // Magic color (if applicable)
        if (opts.magicColor) {
            // Prepare input data
            int imgW = bitmapCurrent.getWidth();
            int imgH = bitmapCurrent.getHeight();
            int[] imageInputBGRA = new int[imgW * imgH];
            bitmapCurrent.getPixels(imageInputBGRA, 0, imgW, 0, 0, imgW, imgH);

            // Allocate output image
            int[] imageOutputBGRA = new int[imgW * imgH];

            // Perform magic color
            JNIExports.magicColor(imgW, imgH, imageInputBGRA, imageOutputBGRA);

            // Get Bitmap from output data
            Bitmap bitmapOutput = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888);
            bitmapOutput.setPixels(imageOutputBGRA, 0, imgW, 0, 0, imgW, imgH);

            // Update current Bitmap
            bitmapCurrent = bitmapOutput;
        }

        // Four point transformation (if applicable)
        if (opts.fourPointApplied) {
            // Prepare input data
            int imgInputW = bitmapCurrent.getWidth();
            int imgInputH = bitmapCurrent.getHeight();
            int[] imageInputBGRA = new int[imgInputW * imgInputH];
            bitmapCurrent.getPixels(imageInputBGRA, 0, imgInputW, 0, 0, imgInputW, imgInputH);

            // Allocate output container
            int[] dimsImageOutput = new int[2];
            int MAX_OUTPUT_DIM = 2880;
            int[] dataOutput = new int[MAX_OUTPUT_DIM*MAX_OUTPUT_DIM]; // TODO this might be too big...

            // Do the transformation
            JNIExports.fourPoint(imgInputW, imgInputH, imageInputBGRA, dimsImageOutput, MAX_OUTPUT_DIM, dataOutput, opts.pRect);

            // Get Bitmap from relevant region of output container
            int outputImageW = dimsImageOutput[0];
            int outputImageH = dimsImageOutput[1];
            Bitmap bitmapOutput = Bitmap.createBitmap(outputImageW, outputImageH, Bitmap.Config.ARGB_8888);
            bitmapOutput.setPixels(dataOutput, 0, outputImageW, 0, 0, outputImageW, outputImageH);

            // Update current Bitmap
            bitmapCurrent = bitmapOutput;
        }

        // Write bitmapOutput to the target file
        int jpgQuality = 80;
        try {
            File outFile = new File(outputAbsolutePath);
            FileOutputStream fos = new FileOutputStream(outFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            bitmapCurrent.compress(Bitmap.CompressFormat.JPEG, jpgQuality, bos);

            bos.flush();
            bos.close();
            fos.close();
        } catch (Exception e) {
            promise.reject("CCCameraModule", e.getMessage(), e);
            return;
        }

        // Resolve
        WritableArray waOutputDims = Arguments.createArray();
        waOutputDims.pushInt(bitmapCurrent.getWidth());
        waOutputDims.pushInt(bitmapCurrent.getHeight());

        WritableMap map = Arguments.createMap();
        map.putString("outputAbsolutePath", outputAbsolutePath);
        map.putArray("outputDims", waOutputDims);
        promise.resolve(map);
    }
    // =============================================================================================

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
                        put("off", FlashMode.OFF.toInt());
                        put("on", FlashMode.ON.toInt());
                        put("auto", FlashMode.AUTO.toInt());
                        put("torch", FlashMode.TORCH.toInt());
                    }
                });
            }

            private Map<String, Object> getCameraModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("fastcam", CameraMode.FASTCAM.toInt());
                        put("photo", CameraMode.CAMERA.toInt());
                        put("scanner", CameraMode.SCANNER.toInt());
                    }
                });
            }

            private Map<String, Object> getResolutionModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("normal", ResolutionMode.NORMAL.toInt());
                        put("high", ResolutionMode.HIGH.toInt());
                        put("super", ResolutionMode.SUPER.toInt());
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
    public void capture() {
        CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null){
            printDebug("No CCCameraView instance; failed");
            return;
        }
        camView.moduleCapture();
    }

    @ReactMethod
    public void flipCamera() {
        CCCameraView camView = CCCameraManager.getLatestView();
        if(camView == null){
            printDebug("No CCCameraView instance; failed");
            return;
        }
        camView.moduleFlipCamera();
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
