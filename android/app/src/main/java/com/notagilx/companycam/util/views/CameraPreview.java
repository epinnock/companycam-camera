package com.notagilx.companycam.util.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.notagilx.companycam.core.web.model.Place;
import com.notagilx.companycam.util.DeviceUtil;
import com.newcam.views.NewCameraView;

public class CameraPreview extends SurfaceView {

    private SurfaceHolder mHolder;

    // mSurfaceCreated is a boolean that describes whether the surfaceCreated callback has already executed
    public boolean mSurfaceCreated = false;

    public int mWidth = 0;
    public int mHeight = 0;
    public int mLeft = 0;
    public int mTop = 0;

    int padding;
    int doubleLineOffset;
    final int ALPHA = 64;
    final int PADDING = 18;

    /*@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        System.out.println("onSizeChanged called with w = " + w + " and h = " + h);
        super.onSizeChanged(w, h, oldw, oldh);
        //super.onSizeChanged(1440, 2560, oldw, oldh);
    }*/

    // This initializer accepts the name and address of the place instead of using a Place object
    public CameraPreview(Context context) {
        super(context);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        //mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        padding = Math.round(DeviceUtil.dpToPx(context, PADDING));
        doubleLineOffset = Math.round(DeviceUtil.dpToPx(context, 5));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        System.out.println("onMeasure called with mWidth = " + mWidth + " and mHeight = " + mHeight);
        if (mWidth != 0 && mHeight != 0) {
            setMeasuredDimension(mWidth, mHeight);
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    protected void setmWidth(int width) {
        System.out.println("setmWidth called with width = " + width);
        mWidth = width;
    }
}
