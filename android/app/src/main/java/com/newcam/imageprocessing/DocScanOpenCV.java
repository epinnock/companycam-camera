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

    protected long docScanPtr;
    protected long lastScanTimestampMS = 0;

    protected boolean initializedBitmaps = false;

    // Size of preview image
    protected int widthOrig;
    protected int heightOrig;

    // Array to hold the result of converting YUV -> RGB
    protected int[] imageBGRA;

    // Overlay image (should match container size)
    protected int widthOverlay;
    protected int heightOverlay;
    protected Bitmap bitmapOverlay;
    protected Canvas canvasOverlay;

    // Array and bitmap used to hold the image returned by nativeScan
    protected final static int MAX_OUTPUT_DIM = 1024;
    protected int[] imageOutput;
    protected Bitmap bitmapFromNative;

    public DocScanOpenCV(Context context) {
        super(context);

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
        imageBGRA = new int[widthOrig * heightOrig];

        widthOverlay = widthContainer;
        heightOverlay = heightContainer;
        bitmapOverlay = Bitmap.createBitmap(widthOverlay, heightOverlay, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        imageOutput = new int[MAX_OUTPUT_DIM * MAX_OUTPUT_DIM];
        bitmapFromNative = Bitmap.createBitmap(MAX_OUTPUT_DIM, MAX_OUTPUT_DIM, Bitmap.Config.ARGB_8888);

        initializedBitmaps = true;
        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + widthContainer + ", " + heightContainer + ")");
    }

    protected Matrix getOrigToOverlayMatrix(int rotation){
        int dimOverlayLarge = Math.max(widthOverlay, heightOverlay);
        int dimOverlaySmall = Math.min(widthOverlay, heightOverlay);

        int dimOrigLarge = Math.max(widthOrig, heightOrig);
        int dimOrigSmall = Math.min(widthOrig, heightOrig);

        float scaleL = (float)dimOverlayLarge / (float)dimOrigLarge;
        float scaleS = (float)dimOverlaySmall / (float)dimOrigSmall;
        float scale = Math.max(scaleL, scaleS);

        Matrix mtransFirst = new Matrix();
        mtransFirst.setTranslate(-(float)widthOrig / 2.0f, -(float)heightOrig / 2.0f);

        Matrix mscale = new Matrix();
        mscale.setScale(scale, scale);

        Matrix mrotate = new Matrix();
        mrotate.setRotate(rotation);

        Matrix mtransLast = new Matrix();
        mtransLast.setTranslate((float)widthOverlay / 2.0f, (float)heightOverlay / 2.0f);

        Matrix m = mtransFirst;
        m.postConcat(mscale);
        m.postConcat(mrotate);
        m.postConcat(mtransLast);
        return m;
    }

    protected void drawPerspectiveRect(Canvas target, float[] pRect, int color){
        Path path = new Path();
        path.moveTo(pRect[0], pRect[1]);
        path.lineTo(pRect[2], pRect[3]);
        path.lineTo(pRect[4], pRect[5]);
        path.lineTo(pRect[6], pRect[7]);
        path.close();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

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

        // Prepare return values from nativeScan
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
        Matrix matOrigToOverlay = getOrigToOverlayMatrix(rotation);
        float[] pRectTransform = new float[8];
        matOrigToOverlay.mapPoints(pRectTransform, pRect);

        bitmapOverlay.eraseColor(Color.argb(0,0,0,0));
        drawPerspectiveRect(canvasOverlay, pRectTransform, Color.argb(128, 0,255,0));

        boolean requestNextFrame = true;
        //bitmapFromNative.setPixels(imageRGB, 0, widthOrig, 0, 0, widthOrig, heightOrig);

        this.invalidate();
        return requestNextFrame;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!initializedBitmaps){ return; }

        Rect rSrc = new Rect(0, 0, bitmapOverlay.getWidth(), bitmapOverlay.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapOverlay, rSrc, rDst, null);
    }

    @Override
    public void clearVisiblePreview() {
        if(!initializedBitmaps){ return; }

        bitmapOverlay.eraseColor(Color.argb(0,0,0,0));
        this.invalidate();
    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
