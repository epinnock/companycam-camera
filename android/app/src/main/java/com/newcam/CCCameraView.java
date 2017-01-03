package com.newcam;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
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

    public CCCameraView(Context context) {
        super(context);
        inflate(context, R.layout.view_cccamera, this);
        init();
    }

    public abstract void init();

    protected Activity getActivity(){
        ThemedReactContext context = (ThemedReactContext)this.getContext();
        return context.getCurrentActivity();
    }

    public abstract void releaseCamera();

    protected void finishWithError(String errmsg){
        propOnClose(errmsg, "error");
    }

    protected void finishWithResult(String button){
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
        //TODO: just testing, please delete me later!
        System.err.println("PHOTO TAKEN: " + imgFile.getAbsolutePath());

        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        event.putInt("imgWidth", imgWidth);
        event.putInt("imgHeight", imgHeight);
        _doEvent("photoTaken", event);
    }

    protected void doPhotoAccepted(File imgFile, int imgWidth, int imgHeight){
        //TODO: just testing, please delete me later!
        System.err.println("PHOTO ACCEPTED: " + imgFile.getAbsolutePath());

        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        event.putInt("imgWidth", imgWidth);
        event.putInt("imgHeight", imgHeight);
        _doEvent("photoAccepted", event);
    }

    private void propOnClose(String errmsg, String button) {

        // Release the camera
        releaseCamera();

        //TODO: just testing, please delete me later!
        System.err.println("ON CLOSE: [" + errmsg + "] [" + button + "]");

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
        if(!appPhotoDirectory.exists()){
            finishWithError("Photo directory does not exist");
        }

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set storage path: " + appPhotoDirectory.getAbsolutePath());
    }

    public void setProjectName(String str){
        placeName = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project name: " + str);

        // Set the mPlaceName label
        if (mPlaceName != null) {
            mPlaceName.setText(placeName);
        }
    }

    public void setProjectAddress(String str){
        placeAddress = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project address: " + str);

        // Set the mPlaceAddress label
        if (mPlaceAddress != null) {
            mPlaceAddress.setText(placeAddress);
        }
    }

    public void setExifLat(double val){
        this.propExifLocationLatitude = val;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set EXIF latitude: " + val);
    }

    public void setExifLon(double val){
        this.propExifLocationLongitude = val;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set EXIF longitude: " + val);
    }

    public void setExifLocTimestamp(long val){
        this.propExifLocationTimestamp = val;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set EXIF location timestamp: " + val);
    }
    //-------------------------------------
}
