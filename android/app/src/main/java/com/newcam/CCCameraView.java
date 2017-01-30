package com.newcam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;

/**
 * Created by dan on 12/16/16.
 */

public abstract class CCCameraView extends RelativeLayout {

    private static final String APP_PACKAGE ="com.agilx.companycam";

    // Permissions required to take a picture
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    // Component props: values
    protected String placeName;
    protected String placeAddress;
    protected File appPhotoDirectory;
    protected double propExifLocationLatitude;
    protected double propExifLocationLongitude;
    protected long propExifLocationTimestamp;

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

    public abstract void init(Context context);

    protected Activity getActivity(){
        ThemedReactContext context = (ThemedReactContext)this.getContext();
        return context.getCurrentActivity();
    }

    protected SharedPreferences getSharedPreferences() {
        return getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
    }

    // This method returns a boolean that describes whether or not each of the necessary camera permissions has been granted.
    protected boolean checkCameraPermissions() {
        for (String permission : CAMERA_PERMISSIONS) {
            int result = ContextCompat.checkSelfPermission(getContext(), permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public abstract void startCamera();
    public abstract void releaseCamera();

    protected void finishWithError(String errmsg){
        releaseCamera();
        propOnClose(errmsg, "error");
    }

    protected void finishWithResult(String button){
        releaseCamera();
        propOnClose("", button);
    }

    protected Location getExifLocation(){
        Location loc = new Location("Component props");
        loc.setLongitude(this.propExifLocationLongitude);
        loc.setLatitude(this.propExifLocationLatitude);
        loc.setTime(this.propExifLocationTimestamp);
        return loc;
    }

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

    protected void doPhotoTaken(File imgFile, int imgWidth, int imgHeight){
        //invoke photoTaken prop
        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        event.putInt("imgWidth", imgWidth);
        event.putInt("imgHeight", imgHeight);
        _doEvent("photoTaken", event);
    }

    protected void doPhotoAccepted(File imgFile, int imgWidth, int imgHeight){
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

    public void setProjectName(String str){
        placeName = str;

        // Set the mPlaceName label
        if (mPlaceName != null) {
            mPlaceName.setText(placeName);
        }
    }

    public void setProjectAddress(String str){
        placeAddress = str;

        // Set the mPlaceAddress label
        if (mPlaceAddress != null) {
            mPlaceAddress.setText(placeAddress);
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
    //-------------------------------------
}
