package com.newcam.imageprocessing;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import com.newcam.imageprocessing.utils.DocScanUtil;
import com.newcam.imageprocessing.utils.PerspectiveRect;

import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;

/**
 * Created by dan on 5/1/17.
 */

public class DocumentScanOverlay extends View implements CCCameraImageProcessor {

    protected Context context;

    protected Bitmap bitmapOriginal;
    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;

    protected DrawableU8Android tempCanvas;

    protected boolean didReceiveImageParams = false;
    protected int width;
    protected int height;
    protected int previewFormat;
    protected int WORKING_W = 256;
    protected int WORKING_H = 256;

    protected GrayU8 imageU8;
    protected byte[] workBuffer;
    protected DocScanUtil docScanner;

    protected boolean didPrepareRenderScript = false;
    protected Allocation allocIn;
    protected Allocation allocOut;
    protected ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

    protected boolean didPrepareOverlay = false;


    public DocumentScanOverlay(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));
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
        didPrepareRenderScript = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void convertYUVToRGB(Bitmap target, byte[] data){
        if(!didPrepareRenderScript){ return; }

        allocIn.copyFrom(data);
        yuvToRgbIntrinsic.forEach(allocOut);
        allocOut.copyTo(target);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!didPrepareOverlay){ return; }

        Rect rSrc = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapTransform, rSrc, rDst, null);
    }

    // CCCameraImageProcessor
    //====================================================================
    @Override
    public void setBytes(byte[] dataOriginal) {
        if(!didReceiveImageParams){ return; }
        if(!didPrepareRenderScript){ return; }

        DEBUG_OUTPUT("Received bytes!");

        long step1MS = System.currentTimeMillis();

        convertYUVToRGB(bitmapOriginal, dataOriginal);

        Rect rSrc = new Rect(0, 0, bitmapOriginal.getWidth(), bitmapOriginal.getHeight());
        Rect rDst = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        canvasTransform.drawBitmap(bitmapOriginal, rSrc, rDst, null);

        //convert bitmap to boof U8; prepare boof U8 image for edges
        ConvertBitmap.bitmapToGray(bitmapTransform, imageU8, workBuffer);

        long step2MS = System.currentTimeMillis();

        //scan
        tempCanvas.clearBitmap(Color.argb(255,0,0,0));
        PerspectiveRect rect = docScanner.scan(tempCanvas);

        //BufferedImage output = generateOutputImage(rect, 512);

        //graphics.drawImage(imageResize, 0, 0, this);
        //if(output != null){
        //    graphics.drawImage(output, IMAGE_W + 20, 0, this);
        //}

        DrawingUtilAndroid drawutil = new DrawingUtilAndroid(new Canvas(bitmapTransform));
        docScanner.drawLastMaxContour(drawutil);
        rect.drawLines(drawutil);
        rect.drawPoints(drawutil);

        long step3MS = System.currentTimeMillis();

        DEBUG_OUTPUT("Finished processing: ");
        DEBUG_OUTPUT(" - Image prep:  " + (step2MS - step1MS) + " ms");
        DEBUG_OUTPUT(" - Doc scanner: " + (step3MS - step2MS) + " ms");

        didPrepareOverlay = true;
        this.invalidate();
    }

    @Override
    public void setImageParams(int width, int height, int previewFormat) {
        DEBUG_OUTPUT("setImageParams received!");

        this.width = width;
        this.height = height;
        this.previewFormat = previewFormat;

        bitmapOriginal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        bitmapTransform = Bitmap.createBitmap(WORKING_W, WORKING_H, Bitmap.Config.ARGB_8888);
        imageU8 = ConvertBitmap.bitmapToGray(bitmapTransform, (GrayU8)null, null);
        canvasTransform = new Canvas(bitmapTransform);
        workBuffer = ConvertBitmap.declareStorage(bitmapTransform, null);

        Bitmap bitmapExtra = Bitmap.createBitmap(WORKING_W, WORKING_H, Bitmap.Config.ARGB_8888);
        tempCanvas = new DrawableU8Android(bitmapExtra);

        docScanner = new DocScanUtil(imageU8);

        didReceiveImageParams = true;

        //NV21: YUV 12 bits per pixel
        prepareRenderScriptYUVToRGB(width*height*3/2);
    }
}
