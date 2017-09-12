package com.newcam;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.newcam.cameras.CCCamera;
import com.newcam.cameras.CCCamera1;
import com.newcam.cameras.CCCamera2;
import com.newcam.utils.AppPreferences;
import com.newcam.utils.CameraCheck;
import com.newcam.views.CCCameraLayout;

import java.io.File;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraView extends RelativeLayout {

    // The mCamera object implements the camera-related behavior
    public CCCamera mCamera;

    // The mCameraLayout object contains all the UI elements for the camera interface
    public CCCameraLayout mCameraLayout;

    // The mPreviewLayout contains the camera preview
    public RelativeLayout mPreviewLayout;

    // Component props: values
    public String placeName;
    public String placeAddress;
    protected File appPhotoDirectory;
    protected double propExifLocationLatitude;
    protected double propExifLocationLongitude;
    protected long propExifLocationTimestamp;
    public String propAuxModeCaption;
    public boolean showCameraUI;

    // Permissions required to take a picture
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    // Common layout features
    protected TextView mPlaceName;
    protected TextView mPlaceAddress;

    // The useTabletLayout flag describes whether the tablet layout is being used for this device
    public boolean useTabletLayout = false;

    public CCCameraView(Context context) {
        super(context);

        // Request/verify permissions before initializing
        if(checkCameraPermissions()){
            inflate(context, R.layout.view_cccamera, this);

            // Get references to the subviews
            //mCameraLayout = (CCCameraLayout) findViewById(R.id.layout_cccamera);
            mPreviewLayout = (RelativeLayout) findViewById(R.id.camera_preview);

            mCameraLayout = new CCCameraLayout(context);
            mCameraLayout.setAuxModeListener(new CCCameraLayout.AuxModeListener(){
                @Override
                public void onAuxModeClicked() {
                    propOnAuxModeClicked();
                }
            });
            RelativeLayout.LayoutParams newParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mCameraLayout, newParams);

            init(context);
        }else{
            System.err.println("No camera permissions");

            // TODO: the props don't seem to be set at this point (no @ReactProp method for callbacks...)
            // Inflate a little error message/close button layout in this case...
            inflate(context, R.layout.view_nocamera, this);

            Button exitButton = (Button) findViewById(R.id.exit_button);
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    propOnClose("No camera permissions", "error");
                }
            });
        }
    }

    public void init(Context context) {
        boolean forceCamera1 = AppPreferences.getForceCamera1(context);
        boolean camera2Available = CameraCheck.getCamera2Available(context);

        // Create the appropriate CCCamera class according to the device's version and available cameras
        if (!forceCamera1 && camera2Available) {
            mCamera = new CCCamera2(context, this);
        }
        else {
            mCamera = new CCCamera1(context, this);
        }

        // Set the layout object's reference to the camera
        mCameraLayout.setCamera(this.mCamera);

        // Add a touch listener to the mPreviewLayout
        mPreviewLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {

                // Dismiss the resolution layout if it's showing
                if (mCameraLayout.mResolutionLayoutVisible) {
                    mCameraLayout.hideResolutionLayout();
                }

                // Pass the touch event to the mCamera object and let it handle it
                return mCamera.handleTouchEvent(event);
            }
        });
    }

    public Activity getActivity() {
        ThemedReactContext context = (ThemedReactContext)this.getContext();
        return context.getCurrentActivity();
    }

    // This method returns a boolean that describes whether or not each of the necessary camera permissions has been granted.
    public boolean checkCameraPermissions() {
        for (String permission : CAMERA_PERMISSIONS) {
            int result = ContextCompat.checkSelfPermission(getContext(), permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // This method starts the camera based on the camera API being implemented
    public void startCamera() {
        mCamera.startCamera();
    }

    // This method releases the camera based on the camera API being implemented
    public void releaseCamera() {
        if (mCamera != null){
            mCamera.releaseCamera();
        }
    }

    public void labelTouch() {
        // Finish the view with a result
        //finishWithResult("label"); //TODO: temporarily disabled
    }

    public void finishWithError(String errmsg){
        releaseCamera();
        propOnClose(errmsg, "error");
    }

    public void finishWithResult(String button){
        releaseCamera();
        propOnClose("", button);
    }

    public Location getExifLocation(){
        Location loc = new Location("Component props");
        loc.setLongitude(this.propExifLocationLongitude);
        loc.setLatitude(this.propExifLocationLatitude);
        loc.setTime(this.propExifLocationTimestamp);
        return loc;
    }

    ////////////////////
    // Layout methods //
    ////////////////////

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    @Override
    public void requestLayout() {
        super.requestLayout();

        // Since React Native overrides onLayout in its ViewGroups, a layout pass never
        // happens after a call to requestLayout, so we simulate one here.
        post(measureAndLayout);
    }

    //component props: functions
    //-------------------------------------
    private void _doEvent(String eventName, WritableMap event){
        ReactContext reactContext = (ReactContext)getContext();
        RCTEventEmitter rctEventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        rctEventEmitter.receiveEvent(getId(), eventName, event);
    }

    public void doPhotoTaken(File imgFile, int imgWidth, int imgHeight){
        if(!imgFile.exists()) {
            propOnClose("There was an error saving the photo file.", "error");
            return;
        }

        //invoke photoTaken prop
        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        event.putInt("imgWidth", imgWidth);
        event.putInt("imgHeight", imgHeight);
        _doEvent("photoTaken", event);
    }

    public void doPhotoAccepted(File imgFile, int imgWidth, int imgHeight){
        if(!imgFile.exists()) {
            propOnClose("There was an error saving the photo file.", "error");
            return;
        }

        // Invoke photoAccepted prop
        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        event.putInt("imgWidth", imgWidth);
        event.putInt("imgHeight", imgHeight);
        _doEvent("photoAccepted", event);
    }

    private void propOnClose(String errmsg, String button) {
        // Invoke onClose prop
        WritableMap event = Arguments.createMap();
        event.putString("errmsg", errmsg);
        event.putString("button", button);
        _doEvent("onClose", event);
    }

    private void propOnAuxModeClicked() {
        // Invoke onAuxModeClicked prop
        WritableMap event = Arguments.createMap();
        _doEvent("onAuxModeClicked", event);
    }
    //-------------------------------------

    //component props: values
    //-------------------------------------
    public void setStoragePath(String str){
        this.appPhotoDirectory = new File(str);

        // Quit with error if invalid path
        if(!appPhotoDirectory.exists()){
            finishWithError("Photo directory does not exist");
        }
    }

    public File getStoragePath() {
        return this.appPhotoDirectory;
    }

    public void setProjectName(String str){
        placeName = str;

        // Set the mPlaceName label
        if (mCameraLayout.mPlaceName != null) {
            mCameraLayout.mPlaceName.setText(placeName);
        }
    }

    public void setProjectAddress(String str){
        placeAddress = str;

        // Set the mPlaceAddress label
        if (mCameraLayout.mPlaceAddress != null) {
            mCameraLayout.mPlaceAddress.setText(placeAddress);
        }
    }

    public void setExifLat(double val){
        this.propExifLocationLatitude = val;
    }

    public void setExifLon(double val){
        this.propExifLocationLongitude = val;
    }

    public void setExifLocTimestamp(double val){
        this.propExifLocationTimestamp = (long)val;
    }

    public void setAuxModeCaption(String val){
        propAuxModeCaption = val;

        // Set mScannerLabel text
        if(mCameraLayout != null) {
            mCameraLayout.setAuxModeCaption(val);
        }
    }

    public void setShowCameraUI(boolean val){
      showCameraUI = val;

      if (showCameraUI) {
        if (mCameraLayout.getVisibility() == View.GONE) {
            mCameraLayout.setVisibility(View.VISIBLE);
        }
      } else {
        if (mCameraLayout.getVisibility() == View.VISIBLE) {
            mCameraLayout.setVisibility(View.GONE);
        }
      }
    }
    //-------------------------------------
}
