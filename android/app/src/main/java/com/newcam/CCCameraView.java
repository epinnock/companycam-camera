package com.newcam;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.util.Map;

/**
 * Created by dan on 12/16/16.
 */

public abstract class CCCameraView extends RelativeLayout {

    // The placeName and placeAddress are the parameters passed from the Javascript app
    protected String placeName;
    protected String placeAddress;
    protected File appPhotoDirectory;

    public CCCameraView(Context context) {
        super(context);
        inflate(context, R.layout.activity_camera2, this);
        init();
    }

    public abstract void init();

    protected Activity getActivity(){
        ThemedReactContext context = (ThemedReactContext)this.getContext();
        return context.getCurrentActivity();
    }

    private void propOnClose(String errmsg, String button){
        WritableMap event = Arguments.createMap();
        event.putString("errmsg", errmsg);
        event.putString("button", button);

        ReactContext reactContext = (ReactContext)getContext();
        RCTEventEmitter rctEventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        rctEventEmitter.receiveEvent(getId(), "onClose", event);
    }

    protected void finishWithError(String errmsg){
        propOnClose(errmsg, "error");
    }

    protected void finishWithResult(String button){
        propOnClose("", button);
    }

    //TODO
    //-------------------------------------
    protected void requestSingleLocationUpdate(){

    }

    protected void requestLastLocation(){

    }

    protected Location getLastLocation(){
        Location loc = new Location("Fake location");
        loc.setLongitude(0.0d);
        loc.setLatitude(0.0d);
        return loc;
    }

    protected void logIntercomEvent(String tag, Map<String, Object> attrs){
        System.err.println("LOGGING INTERCOM EVENT: [" + tag + "] " + attrs.toString());
    }

    protected void doPhotoTaken(File imgFile){
        System.err.println("PHOTO TAKEN: " + imgFile.getAbsolutePath());
    }

    protected void doPhotoAccepted(File imgFile){
        System.err.println("PHOTO ACCEPTED: " + imgFile.getAbsolutePath());
    }
    //-------------------------------------

    public void setStoragePath(String str){
        //this.propStoragePath = new File(str);
        appPhotoDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set storage path: " + appPhotoDirectory.getAbsolutePath());
    }

    public void setProjectName(String str){
        placeName = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project name: " + str);
    }

    public void setProjectAddress(String str){
        placeAddress = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project address: " + str);
        propOnClose("An error message!", "A button name");
    }
}
