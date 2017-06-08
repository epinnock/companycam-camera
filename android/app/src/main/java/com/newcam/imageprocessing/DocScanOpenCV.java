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

public class DocScanOpenCV extends View implements CCCameraImageProcessor {

    // Native stuff
    //----------------------------------------------
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();
    //----------------------------------------------

    // RenderScript stuff
    //-------------------------------------------------
    protected boolean didPrepareRenderScript = false;

    protected Allocation allocInYUV;
    protected Allocation allocOutRGBA;
    protected ScriptIntrinsicYuvToRGB scriptYUVtoRGB;
    //-------------------------------------------------

    protected static void DEBUG_OUTPUT(String message){
        System.err.println("[CCAM] DocScanOpenCV: " + message);
    }

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


    public DocScanOpenCV(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));
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

    // CCCameraImageProcessor stuff
    //====================================================================
    @Override
    public void setListener(ImageProcessorListener listener) {

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

        // Bitmap and GrayU8 on which to do the actual CV ops
        int MAX_WORKING_DIM = 384;
        float scaleX = (float)MAX_WORKING_DIM / (float)widthContainer;
        float scaleY = (float)MAX_WORKING_DIM / (float)heightContainer;
        float scale = Math.min(scaleX, scaleY);

        widthTransform = (int)((float)widthContainer * scale);
        heightTransform = (int)((float)heightContainer * scale);
        bitmapTransform = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        didReceiveImageParams = true;
        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + widthContainer + ", " + heightContainer + ")");
        DEBUG_OUTPUT("- Transform size: (" + widthTransform + ", " + heightTransform + ")");
    }

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

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        DEBUG_OUTPUT("Received bytes! (Rotation angle: " + rotation + ")");
        DEBUG_OUTPUT("- didReceiveImageParams = " + didReceiveImageParams);
        DEBUG_OUTPUT("- didPrepareRenderScript = " + didPrepareRenderScript);

        System.err.println("[CCAM DocScan] " + stringFromJNI());

        if(!didReceiveImageParams){ return true; }
        if(!didPrepareRenderScript){ return true; }

        convertYUVToRGB(bitmapOriginal, data);
        boolean requestNextFrame = true;

        Matrix matOrigToTransform = getOrigToTransform(rotation);
        canvasTransform.drawBitmap(bitmapOriginal, matOrigToTransform, null);

        //TODO from OpenCV sample
        //https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
        //----------------------------------
        /*
        int frameSize = getFrameWidth() * getFrameHeight();
        int[] rgba = new int[frameSize];

        FindFeatures(getFrameWidth(), getFrameHeight(), data, rgba);

        Bitmap bmp = Bitmap.createBitmap(getFrameWidth(), getFrameHeight(), Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, getFrameWidth(), 0, 0, getFrameWidth(), getFrameHeight());
        */
        //----------------------------------

        this.invalidate();
        return requestNextFrame;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //if(!didPrepareOverlay){ return; }

        Bitmap bitmapDraw = bitmapTransform;

        Rect rSrc = new Rect(0, 0, bitmapDraw.getWidth(), bitmapDraw.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapDraw, rSrc, rDst, null);
    }

    @Override
    public void clearVisiblePreview() {

    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
