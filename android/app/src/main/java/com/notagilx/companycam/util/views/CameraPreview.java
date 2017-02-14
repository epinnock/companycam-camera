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

    public CameraPreview(Context context) {
        super(context);

        mHolder = getHolder();

        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    // Override onMeasure to explicitly set the size of the view according to the width and height that are specified.
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
