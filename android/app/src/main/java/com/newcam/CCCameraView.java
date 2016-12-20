package com.newcam;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.Map;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraView extends RelativeLayout {

    protected String propStoragePath;
    protected String propProjectName;
    protected String propProjectAddress;

    public CCCameraView(Context context) {
        super(context);
        inflate(context, R.layout.activity_camera2, this);
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
        return null;
    }

    protected void logIntercomEvent(String tag, Map<String, Object> attrs){

    }
    //-------------------------------------

    public void setStoragePath(String str){
        this.propStoragePath = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set storage path: " + str);
    }

    public void setProjectName(String str){
        this.propProjectName = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project name: " + str);
    }

    public void setProjectAddress(String str){
        this.propProjectAddress = str;

        //TODO: just testing, please delete me later!
        System.err.println("[CCC] Set project address: " + str);
        this.propOnClose("An error message!", "A button name");
    }
}
