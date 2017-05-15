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

    // BoofCV and scanning utility stuff
    //-------------------------------------------------
    protected DrawableU8Android tempCanvas;

    protected GrayU8 imageU8;
    protected byte[] workBuffer;
    protected DocScanUtil docScanner;
    //-------------------------------------------------

    // RenderScript stuff
    //-------------------------------------------------
    protected boolean didPrepareRenderScript = false;

    protected Allocation allocInYUV;
    protected Allocation allocOutRGBA;
    protected ScriptIntrinsicYuvToRGB scriptYUVtoRGB;
    //-------------------------------------------------

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

        if(!didPrepareOverlay){ return; }

        Rect rSrc = new Rect(0, 0, widthOrig, heightOrig);
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapOverlay, rSrc, rDst, null);
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
    private Matrix getOrigToTransform(int rotation){
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
    @Override
    public Bitmap createFinalImage(Bitmap bitmapSrc, int rotation, int OUTPUT_MAX_DIM){
        long startMS = System.currentTimeMillis();

        Bitmap bitmapOutput = null;

        //convert bitmapSrc to smaller, rotated GrayU8; scan
        //-------------------------
        Matrix matOrigToTransform = getOrigToTransform(rotation);
        canvasTransform.drawBitmap(bitmapSrc, matOrigToTransform, null);

        ConvertBitmap.bitmapToGray(bitmapTransform, imageU8, workBuffer);

        tempCanvas.clearBitmap(Color.argb(255,0,0,0));
        PerspectiveRect rect = docScanner.scan(tempCanvas);
        //-------------------------

        if(rect != null) {
            List<Point2D_F32> pointsAsPercent = rect.getPointsAsPercent();
            if (pointsAsPercent != null) {
                // Prepare bitmapOutput
                //-------------------------
                float[] dims = new float[]{ 0.0f, 0.0f };
                rect.get3DRectDims(dims);

                float scaleX = (float)OUTPUT_MAX_DIM / dims[0];
                float scaleY = (float)OUTPUT_MAX_DIM / dims[1];
                float scale = Math.min(scaleX, scaleY);

                int finalW = (int)(dims[0] * scale);
                int finalH = (int)(dims[1] * scale);

                bitmapOutput = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888);
                Canvas canvasOutput = new Canvas(bitmapOutput);
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
                //-------------------------
            }
        }

        long endMS = System.currentTimeMillis();
        DEBUG_OUTPUT("Finished processing (" + (endMS - startMS) + " ms)");

        return bitmapOutput;
    }

    // NOTE: bitmapSrc should have size (widthOrig, heightOrig)
    private void createPreview(Bitmap bitmapSrc, int rotation){
        long startMS = System.currentTimeMillis();

        bitmapOverlay.eraseColor(Color.argb(0,0,0,0));

        //convert bitmapSrc to smaller, rotated GrayU8; scan
        //-------------------------
        Matrix matOrigToTransform = getOrigToTransform(rotation);
        canvasTransform.drawBitmap(bitmapSrc, matOrigToTransform, null);

        ConvertBitmap.bitmapToGray(bitmapTransform, imageU8, workBuffer);

        tempCanvas.clearBitmap(Color.argb(255,0,0,0));
        PerspectiveRect rect = docScanner.scan(tempCanvas);
        //-------------------------

        // Debug output
        //-------------------------
        //DrawingUtilAndroid drawutil = new DrawingUtilAndroid(new Canvas(bitmapTransform));
        //docScanner.drawCannyDebug(drawutil);
        //docScanner.drawLastMaxContour(drawutil);
        //rect.drawLines(drawutil);
        //rect.drawPoints(drawutil);
        //-------------------------

        if(rect != null) {
            List<Point2D_F32> pointsAsPercent = rect.getPointsAsPercent();
            if (pointsAsPercent != null) {
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
                paint.setColor(Color.argb(128, 0, 128, 255));

                canvasOverlay.drawPath(opath, paint);
                //-------------------------
            }
        }

        long endMS = System.currentTimeMillis();
        DEBUG_OUTPUT("Finished processing (" + (endMS - startMS) + " ms)");
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
        createPreview(bitmapOriginal, rotation);

        didPrepareOverlay = true;
        this.invalidate();
    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer) {

        // YUV -> RGB conversion Bitmap and RenderScript converter
        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        bitmapOriginal = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

        prepareRenderScriptYUVToRGB();

        // Bitmaps for UI overlay and final output
        bitmapOverlay = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

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

        // Scanning util instance and a DrawableU8 used internally by the scanner
        Bitmap bitmapExtra = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        tempCanvas = new DrawableU8Android(bitmapExtra);

        docScanner = new DocScanUtil(imageU8);

        didReceiveImageParams = true;
        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + widthContainer + ", " + heightContainer + ")");
        DEBUG_OUTPUT("- Transform size: (" + widthTransform + ", " + heightTransform + ")");
    }
}
