package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import com.newcam.imageprocessing.utils.BoofLogUtil;
import com.newcam.imageprocessing.utils.DocScanUtil;
import com.newcam.imageprocessing.utils.PerspectiveRect;

import java.util.List;

import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F32;

/**
 * Created by dan on 5/1/17.
 */

public class DocumentScanOverlay extends View implements CCCameraImageProcessor {

    protected Context context;

    protected boolean didReceiveImageParams = false;
    protected boolean didPrepareOverlay = false;

    protected int widthOrig;
    protected int heightOrig;

    protected Bitmap bitmapOriginal;

    protected Bitmap bitmapOverlay;
    protected Canvas canvasOverlay;

    protected int widthTransform;
    protected int heightTransform;

    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;

    // Color overlay based on stability of PerspectiveRect
    //-------------------------------------------------
    protected final static int RECTANGLE_UNSTABLE = 0;
    protected final static int RECTANGLE_STABLE = 1;

    protected final static int RECTANGLE_COLOR_UNSTABLE = Color.argb(64, 255,0,0);
    protected final static int RECTANGLE_COLOR_STABLE = Color.argb(128, 0,128,255);
    protected final static int RECTANGLE_COLOR_FOUND = Color.argb(128, 0,255,0);

    protected final static long RECTANGLE_FOUND_THRESHOLD = 1000;
    protected final static float RECT_STABILITY_THRESHOLD = 15.0f;

    protected PerspectiveRect rectPrevious1 = null;
    protected PerspectiveRect rectPrevious2 = null;

    protected int overlayRectangleColor = RECTANGLE_COLOR_UNSTABLE;
    protected int rectangleStability = RECTANGLE_UNSTABLE;
    protected long stableStartMS = 0;

    // Output image
    //-------------------------------------------------
    protected Bitmap bitmapOutput;
    protected Canvas canvasOutput;
    protected int MAX_OUTPUT_DIM;

    protected boolean didPrepareOutput = false;
    protected int outputImageW = 100;
    protected int outputImageH = 100;

    // BoofCV and scanning utility stuff
    //-------------------------------------------------
    protected GrayU8 imageU8;
    protected byte[] workBuffer;
    protected DocScanUtil docScanner;

    // RenderScript stuff
    //-------------------------------------------------
    protected boolean didPrepareRenderScript = false;

    protected Allocation allocInYUV;
    protected Allocation allocOutRGBA;
    protected ScriptIntrinsicYuvToRGB scriptYUVtoRGB;


    public DocumentScanOverlay(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));
    }

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocumentScanOverlay: " + message);
    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

        // YUV -> RGB conversion Bitmap and RenderScript converter
        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        bitmapOriginal = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

        prepareRenderScriptYUVToRGB();

        // Bitmaps for UI overlay and final output
        bitmapOverlay = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        this.MAX_OUTPUT_DIM = MAX_OUTPUT_DIM;
        bitmapOutput = Bitmap.createBitmap(MAX_OUTPUT_DIM, MAX_OUTPUT_DIM, Bitmap.Config.ARGB_8888);
        canvasOutput = new Canvas(bitmapOutput);

        // Bitmap and GrayU8 on which to do the actual CV ops
        int MAX_WORKING_DIM = 384;
        float scaleX = (float)MAX_WORKING_DIM / (float)widthContainer;
        float scaleY = (float)MAX_WORKING_DIM / (float)heightContainer;
        float scale = Math.min(scaleX, scaleY);

        widthTransform = (int)((float)widthContainer * scale);
        heightTransform = (int)((float)heightContainer * scale);
        bitmapTransform = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        imageU8 = ConvertBitmap.bitmapToGray(bitmapTransform, (GrayU8)null, null);
        workBuffer = ConvertBitmap.declareStorage(bitmapTransform, null);

        // Scanner
        docScanner = new DocScanUtil(imageU8);

        didReceiveImageParams = true;
        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + widthContainer + ", " + heightContainer + ")");
        DEBUG_OUTPUT("- Transform size: (" + widthTransform + ", " + heightTransform + ")");
    }

    // RenderScript stuff
    //====================================================================
    protected void prepareRenderScriptYUVToRGB(){
        DEBUG_OUTPUT("RenderScript (YUV -> RGB): Preparing");

        RenderScript rs = RenderScript.create(context);
        scriptYUVtoRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        int byteCount = widthOrig*heightOrig*3/2;
        Type.Builder typeYUV = new Type.Builder(rs, Element.U8(rs)).setX(byteCount);
        allocInYUV = Allocation.createTyped(rs, typeYUV.create(), Allocation.USAGE_SCRIPT);

        Type.Builder typeRGBA = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(widthOrig).setY(heightOrig);
        allocOutRGBA = Allocation.createTyped(rs, typeRGBA.create(), Allocation.USAGE_SCRIPT);

        scriptYUVtoRGB.setInput(allocInYUV);
        didPrepareRenderScript = true;

        DEBUG_OUTPUT("RenderScript (YUV -> RGB): Ready!");
    }

    protected void convertYUVToRGB(Bitmap target, byte[] data){
        if(!didPrepareRenderScript){ return; }

        allocInYUV.copyFrom(data);
        scriptYUVtoRGB.forEach(allocOutRGBA);
        allocOutRGBA.copyTo(target);
    }

    // Scanning and preview/output generation
    //====================================================================
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

    // NOTE: bitmapSrc should have size (widthOrig, heightOrig)
    protected void generatePreviewAndOutput(Bitmap bitmapSrc, int rotation){

        //convert bitmapSrc to smaller, rotated GrayU8; scan
        //-------------------------
        long startMS = System.currentTimeMillis();

        Matrix matOrigToTransform = getOrigToTransform(rotation);
        canvasTransform.drawBitmap(bitmapSrc, matOrigToTransform, null);

        ConvertBitmap.bitmapToGray(bitmapTransform, imageU8, workBuffer);

        PerspectiveRect rectCurrent = docScanner.scan();

        long endMS = System.currentTimeMillis();
        DEBUG_OUTPUT("Finished processing (" + (endMS - startMS) + " ms)");
        //-------------------------

        // Determine stability and color
        //-------------------------
        rectPrevious2 = rectPrevious1;
        rectPrevious1 = rectCurrent;

        float dist = PerspectiveRect.maxScreenSpacePointDistance(rectPrevious1, rectPrevious2);
        boolean isStable = !Float.isNaN(dist) && (dist < RECT_STABILITY_THRESHOLD);

        BoofLogUtil.v("COMPARING RECTS: ");
        BoofLogUtil.v("- dist: " + dist);
        BoofLogUtil.v("- isNaN: " + Float.isNaN(dist));
        BoofLogUtil.v("- isStable: " + isStable);
        BoofLogUtil.v("- timer: " + stableStartMS);

        if(!isStable){
            BoofLogUtil.v("Unstable! resetting");
            //if unstable, reset everything and color unstable
            overlayRectangleColor = RECTANGLE_COLOR_UNSTABLE;
            rectangleStability = RECTANGLE_UNSTABLE;
            stableStartMS = 0;
        }else{
            if(rectangleStability == RECTANGLE_UNSTABLE){
                BoofLogUtil.v("Transition unstable -> stable");
                //if transitioning from unstable -> stable, color as stable and start timer
                overlayRectangleColor = RECTANGLE_COLOR_STABLE;
                rectangleStability = RECTANGLE_STABLE;
                stableStartMS = System.currentTimeMillis();
            }else{
                BoofLogUtil.v("Maintaining stable -> stable");
                //if stable -> stable, check if enough time has elapsed
                long stableTime = System.currentTimeMillis() - stableStartMS;
                rectangleStability = RECTANGLE_STABLE;
                overlayRectangleColor = (stableTime > RECTANGLE_FOUND_THRESHOLD) ? RECTANGLE_COLOR_FOUND : RECTANGLE_COLOR_STABLE;
            }
        }
        //-------------------------

        bitmapOverlay.eraseColor(Color.argb(0,0,0,0));

        if(rectCurrent == null){ return; }
        List<Point2D_F32> pointsAsPercent = rectCurrent.getPointsAsPercent();
        if(pointsAsPercent == null){ return; }

        // Debug output
        //-------------------------
        //DrawingUtilAndroid drawutil = new DrawingUtilAndroid(new Canvas(bitmapTransform));
        //docScanner.drawCannyDebug(drawutil);
        //docScanner.drawLastMaxContour(drawutil);
        //rect.drawLines(drawutil);
        //rect.drawPoints(drawutil);
        //-------------------------

        // Draw translucent overlay quad onto bitmapOverlay
        //-------------------------
        Path opath = new Path();
        int i = 0;
        for (Point2D_F32 point : pointsAsPercent) {
            float px = point.getX() * widthOrig;
            float py = point.getY() * heightOrig;

            if (i == 0) {
                opath.moveTo(px, py);
            } else {
                opath.lineTo(px, py);
            }
            i++;
        }
        opath.close();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(overlayRectangleColor);

        canvasOverlay.drawPath(opath, paint);
        didPrepareOverlay = true;
        //-------------------------

        // Determine output image size
        //-------------------------
        float[] dims = new float[]{ 0.0f, 0.0f };
        rectCurrent.get3DRectDims(dims);

        float scaleX = (float) MAX_OUTPUT_DIM / dims[0];
        float scaleY = (float) MAX_OUTPUT_DIM / dims[1];
        float scale = Math.min(scaleX, scaleY);

        int finalW = (int)(dims[0] * scale);
        int finalH = (int)(dims[1] * scale);
        //-------------------------

        // Draw perspective-corrected quad onto bitmapOutput
        //-------------------------
        float[] pOut = new float[]{
                0.0f, finalH,
                finalW, finalH,
                finalW, 0.0f,
                0.0f, 0.0f,
        };

        float[] pIn = new float[8];
        int j = 0;
        for (Point2D_F32 point : pointsAsPercent) {
            float px = point.getX() * widthTransform;
            float py = point.getY() * heightTransform;

            pIn[j*2  ] = px;
            pIn[j*2+1] = py;

            j++;
        }

        Matrix matPerspective = new Matrix();
        matPerspective.setPolyToPoly(pIn, 0, pOut, 0, 4);

        matPerspective.preConcat(matOrigToTransform);

        canvasOutput.drawBitmap(bitmapSrc, matPerspective, null);
        didPrepareOutput = true;
        outputImageW = finalW;
        outputImageH = finalH;
        //-------------------------
    }

    // CCCameraImageProcessor
    //====================================================================
    @Override
    public void setPreviewBytes(byte[] dataOriginal, int rotation) {
        DEBUG_OUTPUT("Received bytes! (Rotation angle: " + rotation + ")");
        DEBUG_OUTPUT("- didReceiveImageParams = " + didReceiveImageParams);
        DEBUG_OUTPUT("- didPrepareRenderScript = " + didPrepareRenderScript);

        if(!didReceiveImageParams){ return; }
        if(!didPrepareRenderScript){ return; }

        //original YUV -> rotated/scaled bitmapTransform -> GrayU8
        convertYUVToRGB(bitmapOriginal, dataOriginal);
        generatePreviewAndOutput(bitmapOriginal, rotation);

        this.invalidate();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!didPrepareOverlay){ return; }

        Rect rSrc = new Rect(0, 0, widthOrig, heightOrig);
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapOverlay, rSrc, rDst, null);
    }

    @Override
    public Bitmap getOutputImage(){
        if(!didPrepareOutput){ return null; }

        return Bitmap.createBitmap(bitmapOutput, 0, 0, outputImageW, outputImageH);
    }
}
