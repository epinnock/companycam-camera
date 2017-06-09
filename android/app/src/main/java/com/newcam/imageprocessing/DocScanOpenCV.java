package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by dan on 5/1/17.
 */

public class DocScanOpenCV extends View implements CCCameraImageProcessor {

    // Native stuff
    //----------------------------------------------
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();
    public native void nativeScan(int width, int height, byte yuv[], int[] rgba);
    //----------------------------------------------

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocScanOpenCV: " + message);
    }

    protected Context context;

    protected boolean didReceiveImageParams = false;

    protected int widthOrig;
    protected int heightOrig;

    protected Bitmap bitmapFromNative;

    protected int widthTransform;
    protected int heightTransform;

    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;


    public DocScanOpenCV(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));
    }

    // CCCameraImageProcessor stuff
    //====================================================================
    @Override
    public void setListener(ImageProcessorListener listener) {

    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        bitmapFromNative = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

        float scale = 1.0f;
        widthTransform = (int)((float)widthContainer * scale);
        heightTransform = (int)((float)heightContainer * scale);
        bitmapTransform = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        didReceiveImageParams = true;
        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + widthContainer + ", " + heightContainer + ")");
    }

    protected Matrix getOrigToTransform(int rotation){
        int dimTransformLarge = Math.max(widthTransform, heightTransform);
        int dimTransformSmall = Math.min(widthTransform, heightTransform);

        int dimOrigLarge = Math.max(widthOrig, heightOrig);
        int dimOrigSmall = Math.min(widthOrig, heightOrig);

        float scaleL = (float)dimTransformLarge / (float)dimOrigLarge;
        float scaleS = (float)dimTransformSmall / (float)dimOrigSmall;
        float scale = Math.max(scaleL, scaleS);

        Matrix mtransFirst = new Matrix();
        mtransFirst.setTranslate(-(float)widthOrig / 2.0f, -(float)heightOrig / 2.0f);

        Matrix mscale = new Matrix();
        mscale.setScale(scale, scale);

        Matrix mrotate = new Matrix();
        mrotate.setRotate(rotation);

        Matrix mtransLast = new Matrix();
        mtransLast.setTranslate((float)widthTransform / 2.0f, (float)heightTransform / 2.0f);

        Matrix m = mtransFirst;
        m.postConcat(mscale);
        m.postConcat(mrotate);
        m.postConcat(mtransLast);
        return m;
    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        DEBUG_OUTPUT("Received bytes! (Rotation angle: " + rotation + ")");
        DEBUG_OUTPUT("- didReceiveImageParams = " + didReceiveImageParams);

        System.err.println("[CCAM DocScan] " + stringFromJNI());

        if(!didReceiveImageParams){ return true; }

        boolean requestNextFrame = true;

        //TODO from OpenCV sample
        //https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
        //----------------------------------
        int frameSize = widthOrig*heightOrig;
        int[] rgba = new int[frameSize];

        nativeScan(widthOrig, heightOrig, data, rgba);

        bitmapFromNative.setPixels(rgba, 0, widthOrig, 0, 0, widthOrig, heightOrig);
        //----------------------------------

        Matrix matOrigToTransform = getOrigToTransform(rotation);
        canvasTransform.drawBitmap(bitmapFromNative, matOrigToTransform, null);

        this.invalidate();
        return requestNextFrame;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!didReceiveImageParams){ return; }

        Bitmap bitmapDraw = bitmapTransform;
        Rect rSrc = new Rect(0, 0, bitmapDraw.getWidth(), bitmapDraw.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapDraw, rSrc, rDst, null);
    }

    @Override
    public void clearVisiblePreview() {

    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
