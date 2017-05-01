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

    protected Context context;

    protected boolean didReceiveImageParams = false;

    protected int width;
    protected int height;
    protected int previewFormat;

    protected Bitmap overlayBitmap;
    protected Canvas overlayCanvas;

    protected byte[] data;


    public DocumentScanOverlay(Context context) {
        super(context);
        this.context = context;

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

    public Allocation renderScriptNV21ToRGBA888(Context context, int width, int height, byte[] nv21) {
        if(android.os.Build.VERSION.SDK_INT < 17) {
            return null;
        }

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }

    @Override
    public void setBytes(byte[] data) {
        if(!didReceiveImageParams){ return; }

        DEBUG_OUTPUT("Received bytes!");

        long startMS = System.currentTimeMillis();

        Allocation bmData = renderScriptNV21ToRGBA888(context, width, height, data);
        bmData.copyTo(overlayBitmap);

        int maxVal = 0;
        int maxX = 0;
        int maxY = 0;

        int index = 0;
        for(int y = 0; y < height; y++){
        for(int x = 0; x < width; x++){
            int val = data[index] & 0xFF;

            if(val > maxVal){
                maxVal = val;
                maxX = x;
                maxY = y;
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

        overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        overlayCanvas = new Canvas(overlayBitmap);

        didReceiveImageParams = true;
    }
}
