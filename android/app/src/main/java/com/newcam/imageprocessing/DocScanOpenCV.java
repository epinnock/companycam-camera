package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

import java.util.Arrays;

import georegression.struct.point.Point2D_F32;

/**
 * Created by dan on 5/1/17.
 */

/*
    TODO:
    Right now, the overlay is drawn onto bitmapOverlay, then that bitmap is transformed
    and drawn onto bitmapTransform; finally, bitmapTransform is drawn on the screen over the camera.
    Instead: Draw the overlay directly onto bitmapTransform, applying the transformation to the
    vertices in the overlay's path.  Then just get rid of bitmapOverlay.
*/

public class DocScanOpenCV extends View implements CCCameraImageProcessor {

    // Native stuff
    //----------------------------------------------
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native long newScanner();
    public native void deleteScanner(long ptr);
    public native void resetScanner(long ptr);
    public native void nativeScan(long ptr,
          /* Image to be scanned */
          int width, int height, byte imageYUV[], int[] imageBGRA,
          /* Image returned by the scanner, if any */
          boolean[] didGenerateOutput, int[] dimsImageOutput, int maxWidth, int maxHeight, int[] imageOutput,
          /* Info about most recent scan */
          int[] scanStatus, float pRect[]);
    //----------------------------------------------

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocScanOpenCV: " + message);
    }

    protected Context context;

    protected long docScanPtr;
    protected long lastScanTimestampMS = 0;

    protected boolean initializedBitmaps = false;

    protected int widthOrig;
    protected int heightOrig;

    protected int widthTransform;
    protected int heightTransform;

    protected int[] imageBGRA;

    protected Bitmap bitmapOverlay;
    protected Canvas canvasOverlay;

    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;

    protected final static int MAX_OUTPUT_DIM = 1024;
    protected int[] imageOutput;
    protected Bitmap bitmapFromNative;

    public DocScanOpenCV(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));

        docScanPtr = newScanner();
    }

    @Override
    public void setListener(ImageProcessorListener listener) {

    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;

        widthTransform = widthContainer;
        heightTransform = heightContainer;

        imageBGRA = new int[widthOrig * heightOrig];

        // Holds the overlay which is drawn on top of the camera screen
        bitmapOverlay = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        // bitmapOverlay is transformed and drawn onto bitmapTransform, to account for camera rotation
        bitmapTransform = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        // The image returned by scan is written to imageOutput and drawn onto bitmapFromNative
        imageOutput = new int[MAX_OUTPUT_DIM * MAX_OUTPUT_DIM];
        bitmapFromNative = Bitmap.createBitmap(MAX_OUTPUT_DIM, MAX_OUTPUT_DIM, Bitmap.Config.ARGB_8888);

        initializedBitmaps = true;
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

    protected void drawPerspectiveRect(Canvas target, float[] pRect){
        int overlayColor = Color.argb(128, 0,255,0);

        Path path = new Path();
        path.moveTo(pRect[0], pRect[1]);
        path.lineTo(pRect[2], pRect[3]);
        path.lineTo(pRect[4], pRect[5]);
        path.lineTo(pRect[6], pRect[7]);
        path.close();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(overlayColor);

        target.drawPath(path, paint);
    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        DEBUG_OUTPUT("Received bytes! (Rotation angle: " + rotation + ")");
        DEBUG_OUTPUT("- initializedBitmaps = " + initializedBitmaps);

        if(!initializedBitmaps){ return true; }

        final long curTimeMS = System.currentTimeMillis();
        if(curTimeMS - lastScanTimestampMS > 500){
            lastScanTimestampMS = curTimeMS;
            resetScanner(docScanPtr);
        }

        //TODO from OpenCV sample
        //https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
        //----------------------------------
        boolean[] didGenerateOutput = new boolean[1];
        int[] dimsImageOutput = new int[2];
        int[] scanStatus = new int[1];
        float[] pRect = new float[8];

        nativeScan(docScanPtr,
                /* Image to be scanned */
                widthOrig, heightOrig, data, imageBGRA,
                /* Image returned by the scanner, if any */
                didGenerateOutput, dimsImageOutput, MAX_OUTPUT_DIM, MAX_OUTPUT_DIM, imageOutput,
                /* Info about most recent scan */
                scanStatus, pRect);

        // Draw overlay
        bitmapOverlay.eraseColor(Color.argb(0,0,0,0));
        drawPerspectiveRect(canvasOverlay, pRect);

        boolean requestNextFrame = true;

        //bitmapFromNative.setPixels(imageRGB, 0, widthOrig, 0, 0, widthOrig, heightOrig);
        //----------------------------------

        Matrix matOrigToTransform = getOrigToTransform(rotation);
        bitmapTransform.eraseColor(Color.argb(0,0,0,0));
        canvasTransform.drawBitmap(bitmapOverlay, matOrigToTransform, null);

        this.invalidate();
        return requestNextFrame;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!initializedBitmaps){ return; }

        Rect rSrc = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapTransform, rSrc, rDst, null);
    }

    @Override
    public void clearVisiblePreview() {
        DEBUG_OUTPUT("Clearing visible preview!");

        if(!initializedBitmaps){ return; }

        bitmapTransform.eraseColor(Color.argb(0,0,0,0));
        this.invalidate();
    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
