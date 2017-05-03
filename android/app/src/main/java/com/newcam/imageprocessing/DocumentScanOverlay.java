package com.newcam.imageprocessing;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import java.util.List;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.android.ConvertBitmap;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;

/**
 * Created by dan on 5/1/17.
 */

public class DocumentScanOverlay extends View implements CCCameraImageProcessor {

    protected Context context;

    protected Bitmap bitmapOriginal;
    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;

    protected boolean didReceiveImageParams = false;
    protected int width;
    protected int height;
    protected int previewFormat;

    protected boolean didPrepareBoof = false;
    protected CannyEdge<GrayU8,GrayS16> canny;
    protected GrayU8 boofOrigImg;
    protected GrayU8 boofProcImg;
    byte[] workBuffer;

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
        if(!didPrepareBoof){ return; }
        if(!didPrepareRenderScript){ return; }

        DEBUG_OUTPUT("Received bytes!");

        long step1MS = System.currentTimeMillis();

        convertYUVToRGB(bitmapOriginal, dataOriginal);

        Rect rSrc = new Rect(0, 0, bitmapOriginal.getWidth(), bitmapOriginal.getHeight());
        Rect rDst = new Rect(0, 0, bitmapTransform.getWidth(), bitmapTransform.getHeight());
        canvasTransform.drawBitmap(bitmapOriginal, rSrc, rDst, null);

        //convert bitmap to boof U8; prepare boof U8 image for edges
        ConvertBitmap.bitmapToGray(bitmapTransform, boofOrigImg, workBuffer);

        long step2MS = System.currentTimeMillis();

        canny.process(boofOrigImg, 0.1f, 0.3f, boofProcImg);

        long step3MS = System.currentTimeMillis();

        List<Contour> contours = BinaryImageOps.contour(boofProcImg, ConnectRule.EIGHT, null);

        long step4MS = System.currentTimeMillis();

        //convert edges image back to bitmap
        //byte[] workBuffer = ConvertBitmap.declareStorage(bitmapTransform, null);
        //ConvertBitmap.grayToBitmap(boofOrigImg, bitmapTransform, workBuffer);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(255, 0,255,0));

        for(Contour c : contours){
            Path cpath = new Path();

            int i = 0;
            for(Point2D_I32 p : c.external) {
                if(i == 0) {
                    cpath.moveTo(p.getX(), p.getY());
                }else {
                    cpath.lineTo(p.getX(), p.getY());
                }
                i++;
            }

            canvasTransform.drawPath(cpath, paint);
        }

        long step5MS = System.currentTimeMillis();

        DEBUG_OUTPUT("Finished processing: ");
        DEBUG_OUTPUT(" - Image prep: " + (step2MS - step1MS) + " ms");
        DEBUG_OUTPUT(" - Canny:      " + (step3MS - step2MS) + " ms");
        DEBUG_OUTPUT(" - Contours:   " + (step4MS - step3MS) + " ms");
        DEBUG_OUTPUT(" - Draw lines: " + (step5MS - step4MS) + " ms");

        didPrepareOverlay = true;
        this.invalidate();
    }

    @Override
    public void setImageParams(int width, int height, int previewFormat) {
        DEBUG_OUTPUT("setImageParams received!");

        this.width = width;
        this.height = height;
        this.previewFormat = previewFormat;
        didReceiveImageParams = true;

        bitmapOriginal = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmapTransform = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        canny = FactoryEdgeDetectors.canny(2, true, true, GrayU8.class, GrayS16.class);
        boofOrigImg = ConvertBitmap.bitmapToGray(bitmapTransform, (GrayU8)null, null);
        boofProcImg = boofOrigImg.createSameShape();
        workBuffer = ConvertBitmap.declareStorage(bitmapTransform, null);
        didPrepareBoof = true;

        //NV21: YUV 12 bits per pixel
        prepareRenderScriptYUVToRGB(width*height*3/2);


    }
}
