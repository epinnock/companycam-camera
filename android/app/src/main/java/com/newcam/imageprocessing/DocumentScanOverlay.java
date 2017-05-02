package com.newcam.imageprocessing;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.nio.ByteBuffer;

/**
 * Created by dan on 5/1/17.
 */

public class DocumentScanOverlay extends View implements CCCameraImageProcessor, LoaderCallbackInterface {

    protected Context context;

    protected boolean didReceiveImageParams = false;
    protected int width;
    protected int height;
    protected int previewFormat;

    protected Bitmap bitmapOriginal;
    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;
    protected ByteBuffer bitmapTransformBytes;

    protected boolean allocationOkay = false;
    protected Allocation allocIn;
    protected Allocation allocOut;
    protected ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;


    public DocumentScanOverlay(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context, this);
    }

    protected void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocumentScanOverlay: " + message);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void prepareRenderScriptYUVToRGB(int byteCount){
        RenderScript rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(byteCount);
        allocIn = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        allocOut = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        yuvToRgbIntrinsic.setInput(allocIn);
        allocationOkay = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void convertYUVToRGB(Bitmap target, byte[] data){
        if(!allocationOkay){ return; }

        allocIn.copyFrom(data);
        yuvToRgbIntrinsic.forEach(allocOut);
        allocOut.copyTo(target);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!didReceiveImageParams){ return; }
        if(!allocationOkay){ return; }

        Rect rSrc = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapTransform, rSrc, rDst, null);
    }

    // CCCameraImageProcessor
    //====================================================================
    @Override
    public void setBytes(byte[] dataOriginal) {
        if(!didReceiveImageParams){ return; }

        DEBUG_OUTPUT("Received bytes!");

        long startMS = System.currentTimeMillis();

        convertYUVToRGB(bitmapOriginal, dataOriginal);

        Rect rSrc = new Rect(0, 0, bitmapOriginal.getWidth(), bitmapOriginal.getHeight());
        Rect rDst = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        canvasTransform.drawBitmap(bitmapOriginal, rSrc, rDst, null);

        bitmapTransformBytes.rewind();
        bitmapTransform.copyPixelsToBuffer(bitmapTransformBytes);
        byte[] data = bitmapTransformBytes.array();

        int maxVal = 0;
        int maxX = 0;
        int maxY = 0;

        int index = 0;
        for(int y = 0; y < bitmapTransform.getHeight(); y++){
        for(int x = 0; x < bitmapTransform.getWidth(); x++){
            int a = data[index*4+0] & 0xFF;
            int r = data[index*4+1] & 0xFF;
            int g = data[index*4+2] & 0xFF;
            int b = data[index*4+3] & 0xFF;
            int val = (int)(0.299f * (float)r + 0.587f * (float)g + 0.114f * (float)b);

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
        canvasTransform.drawRect(maxX - 5, maxY - 5, maxX + 5, maxY + 5, paint);

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

        bitmapOriginal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmapTransform = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        int bytes = bitmapTransform.getByteCount();
        bitmapTransformBytes = ByteBuffer.allocate(bytes);

        //NV21: YUV 12 bits per pixel
        prepareRenderScriptYUVToRGB(width*height*3/2);

        didReceiveImageParams = true;
    }

    // LoaderCallbackInterface
    //====================================================================
    @Override
    public void onManagerConnected(int status) {
        DEBUG_OUTPUT("LoaderCallbackInterface onManagerConnected: " + status);
    }

    @Override
    public void onPackageInstall(int operation, InstallCallbackInterface callback) {
        DEBUG_OUTPUT("LoaderCallbackInterface onPackageInstall: " + operation);
    }
}
