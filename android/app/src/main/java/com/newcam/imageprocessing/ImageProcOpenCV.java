package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.newcam.jniexports.JNIExports;

/**
 * Created by dan on 1/17/18.
 */

public class ImageProcOpenCV extends View implements CCCameraImageProcessor  {

    protected final static int COLOR_0000 = Color.argb(0,0,0,0);

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

    // Array used to hold the image data returned by imageproc
    protected int[] dataOutput;

    // Bitmap to hold the result of a scan
    protected Bitmap bitmapOutput;

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] ImageProcOpenCV: " + message);
    }

    public ImageProcOpenCV(Context context) {
        super(context);

        this.setBackgroundColor(COLOR_0000);
    }

    @Override
    public void setListener(ImageProcessorListener listener) { }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {
        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        imageBGRA = new int[widthOrig * heightOrig];

        widthOverlay = widthContainer;
        heightOverlay = heightContainer;
        bitmapOverlay = Bitmap.createBitmap(widthOverlay, heightOverlay, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        dataOutput = new int[widthOrig * heightOrig];
        bitmapOutput = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

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

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {

        JNIExports.convertYUVtoBGRA(widthOrig, heightOrig, data, imageBGRA);
        JNIExports.magicColor(widthOrig, heightOrig, imageBGRA, dataOutput);

        bitmapOutput.setPixels(dataOutput, 0, widthOrig, 0, 0, widthOrig, heightOrig);

        Matrix origToOverlay = getOrigToOverlayMatrix(rotation);
        canvasOverlay.drawBitmap(bitmapOutput, origToOverlay, null);

        this.invalidate();
        return true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!initializedBitmaps){ return; }

        Rect rSrc = new Rect(0, 0, bitmapOverlay.getWidth(), bitmapOverlay.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());

        Paint paint = new Paint();
        paint.setAlpha(192);
        canvas.drawBitmap(bitmapOverlay, rSrc, rDst, paint);
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
        return null;
    }
}
