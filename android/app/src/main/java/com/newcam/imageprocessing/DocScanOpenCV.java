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
    //--------------------------------------
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
        int[] dimsImageOutput, int maxOutputPixels, int[] imageOutput,
        /* Info about most recent scan */
        int[] scanStatus, float pRect[]);
    //--------------------------------------

    // Listeners for auto-capture
    //--------------------------------------
    protected ImageProcessorListener listener;

    @Override
    public void setListener(ImageProcessorListener listener){
        this.listener = listener;
    }

    protected void notifyListeners(){
        if(listener == null){ return; }
        listener.receiveResult();
    }
    //--------------------------------------

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocScanOpenCV: " + message);
    }

    protected static final int SCAN_STATUS_UNSTABLE = 0;
    protected static final int SCAN_STATUS_STABLE = 1;
    protected static final int SCAN_STATUS_DONE = 2;

    protected final static int COLOR_UNSTABLE = Color.argb(64, 255,0,0);
    protected final static int COLOR_STABLE = Color.argb(128, 0,128,255);
    protected final static int COLOR_DONE = Color.argb(128, 0,255,0);

    protected final static int COLOR_0000 = Color.argb(0,0,0,0);

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

    // Array used to hold the image data returned by nativeScan
    protected final static int MAX_OUTPUT_PIXELS = 1024*1024;
    protected int[] dataOutput;

    // Bitmap to hold the result of a scan
    protected boolean didPrepareOutput;
    protected int outputImageW;
    protected int outputImageH;
    protected Bitmap bitmapOutput;


    public DocScanOpenCV(Context context) {
        super(context);

        this.setBackgroundColor(COLOR_0000);
        docScanPtr = newScanner();
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

        dataOutput = new int[MAX_OUTPUT_PIXELS];
        didPrepareOutput = false;

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
        lastScanTimestampMS = curTimeMS;

        // Prepare return values from nativeScan
        int[] dimsImageOutput = new int[2];
        int[] scanStatus = new int[1];
        float[] pRect = new float[8];

        nativeScan(docScanPtr,
            /* Image to be scanned */
            widthOrig, heightOrig, data, imageBGRA,
            /* Image returned by the scanner, if any */
            dimsImageOutput, MAX_OUTPUT_PIXELS, dataOutput,
            /* Info about most recent scan */
            scanStatus, pRect);

        // Draw overlay
        Matrix matOrigToOverlay = getOrigToOverlayMatrix(rotation);
        float[] pRectTransform = new float[8];
        matOrigToOverlay.mapPoints(pRectTransform, pRect);

        int overlayColor = COLOR_UNSTABLE;
        if(scanStatus[0] == SCAN_STATUS_STABLE){ overlayColor = COLOR_STABLE; }
        if(scanStatus[0] == SCAN_STATUS_DONE){ overlayColor = COLOR_DONE; }

        bitmapOverlay.eraseColor(COLOR_0000);
        drawPerspectiveRect(canvasOverlay, pRectTransform, overlayColor);

        // Process output image if appropriate
        boolean requestNextFrame = true;
        if(scanStatus[0] == SCAN_STATUS_DONE){
            outputImageW = dimsImageOutput[0];
            outputImageH = dimsImageOutput[1];
            bitmapOutput = Bitmap.createBitmap(outputImageW, outputImageH, Bitmap.Config.ARGB_8888);
            bitmapOutput.setPixels(dataOutput, 0, outputImageW, 0, 0, outputImageW, outputImageH);
            didPrepareOutput = true;

            DEBUG_OUTPUT("Captured scan: (" + outputImageW + ", " + outputImageH + ")");
            this.notifyListeners();
            requestNextFrame = false;
        }

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
        if(!initializedBitmaps){
            DEBUG_OUTPUT("Tried to clear overlay, but overlay was null!");
            return;
        }
        DEBUG_OUTPUT("Cleared overlay");

        bitmapOverlay.eraseColor(COLOR_0000);
        this.invalidate();
    }

    @Override
    public Bitmap getOutputImage() {
        if(!didPrepareOutput){
            DEBUG_OUTPUT("Requested output image, but image was null!");
            return null;
        }
        DEBUG_OUTPUT("Returning output image");

        return bitmapOutput;
    }
}