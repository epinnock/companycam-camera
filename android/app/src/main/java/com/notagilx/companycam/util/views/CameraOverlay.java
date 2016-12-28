package com.notagilx.companycam.util.views;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.View;

public class CameraOverlay extends View {

    private CameraPreview preview = null;
    private int [] measure_spec = new int[2];
    private boolean isError = false;
    private String errorMsg;

    public CameraOverlay(Context context, CameraPreview preview) {
        super(context);
        this.preview = preview;


        // deprecated setting, but required on Android versions prior to 3.0
        //getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated

        final Handler handler = new Handler();
        Runnable tick = new Runnable() {
            public void run() {
                invalidate();
                handler.postDelayed(this, 100);
            }
        };
        tick.run();
    }

    public void setError(String msg) {
        this.isError = true;
        this.errorMsg = msg;
    }

    public void removeError() {
        this.isError = false;
        this.errorMsg = "";
    }

    public void setErrorMsg(String msg) {
        errorMsg = msg;
    }

    @Override
    public void onDraw(Canvas canvas) {

        //preview.drawOverlay(canvas, isError, errorMsg);
    }

}
