package com.newcam;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
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
        inflate(context, R.layout.view_cccamera, this);
        init();
    }

    public abstract void init();

    protected Activity getActivity(){
        ThemedReactContext context = (ThemedReactContext)this.getContext();
        return context.getCurrentActivity();
    }

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
    }

    public void setProjectAddress(String str){
        placeAddress = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project address: " + str);
    }

    //callbacks
    //-------------------------------------
    private void doEvent(String eventName, WritableMap event){
        ReactContext reactContext = (ReactContext)getContext();
        RCTEventEmitter rctEventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        rctEventEmitter.receiveEvent(getId(), eventName, event);
    }

    protected void doPhotoTaken(File imgFile){
        //TODO: just testing, please delete me later!
        System.err.println("PHOTO TAKEN: " + imgFile.getAbsolutePath());

        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        doEvent("photoTaken", event);
    }

    protected void doPhotoAccepted(File imgFile){
        //TODO: just testing, please delete me later!
        System.err.println("PHOTO ACCEPTED: " + imgFile.getAbsolutePath());

        WritableMap event = Arguments.createMap();
        event.putString("filename", imgFile.getAbsolutePath());
        doEvent("photoAccepted", event);
    }

    private void propOnClose(String errmsg, String button){

        // Release the camera
        releaseCamera();

        //TODO: just testing, please delete me later!
        System.err.println("ON CLOSE: [" + errmsg + "] [" + button + "]");

        WritableMap event = Arguments.createMap();
        event.putString("errmsg", errmsg);
        event.putString("button", button);
        doEvent("onClose", event);
    }

    public abstract void releaseCamera();

    protected void finishWithError(String errmsg){
        propOnClose(errmsg, "error");
    }

    protected void finishWithResult(String button){
        propOnClose("", button);
    }
    //-------------------------------------

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

    protected void logIntercomEvent(String tag, Map<String, Object> attrs) {
        System.err.println("LOGGING INTERCOM EVENT: [" + tag + "] " + attrs.toString());
    }
    //-------------------------------------
}
