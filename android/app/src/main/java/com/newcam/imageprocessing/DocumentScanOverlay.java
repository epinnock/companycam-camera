package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by dan on 5/1/17.
 */

public class DocumentScanOverlay extends View implements CCCameraImageProcessor {

    protected boolean didReceiveImageParams = false;

    protected int width;
    protected int height;
    protected int previewFormat;

    protected Bitmap overlayBitmap;
    protected Canvas overlayCanvas;

    protected byte[] data;


    public DocumentScanOverlay(Context context) {
        super(context);

        this.setBackgroundColor(Color.argb(0,0,0,0));
    }

    protected void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocumentScanOverlay: " + message);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!didReceiveImageParams){ return; }

        Rect rSrc = new Rect(0, 0, overlayBitmap.getWidth(), overlayBitmap.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(overlayBitmap, rSrc, rDst, null);
    }

    @Override
    public void setBytes(byte[] data) {
        if(!didReceiveImageParams){ return; }

        DEBUG_OUTPUT("Received bytes!");

        long startMS = System.currentTimeMillis();

        overlayBitmap.eraseColor(Color.argb(32, 255,0,0));

        int maxVal = 0;
        int maxX = 0;
        int maxY = 0;

        //TODO transformed
        int index = 0;
        for(int y = 0; y < height; y++){
        for(int x = 0; x < width; x++){
            int val = data[index] & 0xFF;

            //TODO transform
            int tx = height - y - 1;
            int ty = x;

            if((tx % 5 == 0) && (ty % 5 == 0)) {
                overlayBitmap.setPixel(tx, ty, Color.argb(192, val, val, val));
            }

            if(val > maxVal){
                maxVal = val;
                maxX = tx;
                maxY = ty;
            }

            index++;
        }}

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(128, 0,255,0));
        overlayCanvas.drawRect(maxX - 20, maxY - 20, maxX + 20, maxY + 20, paint);

        long endMS = System.currentTimeMillis();
        DEBUG_OUTPUT("Finished processing: " + (endMS - startMS) + " ms");

        this.invalidate();
    }

    @Override
    public void setImageParams(int width, int height, int previewFormat) {
        DEBUG_OUTPUT("setImageParams received!");

        this.width = width;
        this.height = height;
        this.previewFormat = previewFormat;

        //TODO transformed
        overlayBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
        overlayCanvas = new Canvas(overlayBitmap);

        didReceiveImageParams = true;
    }
}
