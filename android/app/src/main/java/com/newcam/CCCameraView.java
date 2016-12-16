package com.newcam;

import android.content.Context;
import android.view.ViewGroup;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

/**
 * Created by dan on 12/16/16.
 */

public class CCCameraView extends ViewGroup {

    protected String propProjectName;
    protected String propProjectAddress;

    public CCCameraView(Context context) {
        super(context);

    }

    public void invokeOnClose(String errmsg, String button){
        WritableMap event = Arguments.createMap();
        event.putString("errmsg", errmsg);
        event.putString("button", button);

        ReactContext reactContext = (ReactContext)getContext();
        RCTEventEmitter rctEventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        rctEventEmitter.receiveEvent(getId(), "onClose", event);
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
        this.invokeOnClose("An error message!", "A button name");
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {

    }
}
