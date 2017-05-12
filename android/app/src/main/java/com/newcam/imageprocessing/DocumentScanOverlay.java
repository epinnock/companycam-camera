package com.newcam.imageprocessing;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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

    protected boolean didReceiveImageParams = false;
    protected boolean didPrepareOverlay = false;

    protected int widthOrig;
    protected int heightOrig;
    protected Bitmap bitmapOriginal;

    protected int widthTransform;
    protected int heightTransform;
    protected Bitmap bitmapTransform;
    protected Canvas canvasTransform;

    protected DrawableU8Android tempCanvas;

    protected GrayU8 imageU8;
    protected byte[] workBuffer;
    protected DocScanUtil docScanner;

    protected boolean didPrepareRenderScript = false;
    protected Allocation allocIn;
    protected Allocation allocOut;
    protected ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;


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

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(widthOrig).setY(heightOrig);
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

        Rect rSrc = new Rect(0, 0, widthTransform, heightTransform);
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapTransform, rSrc, rDst, null);
    }

    // CCCameraImageProcessor
    //====================================================================
    @Override
    public void setBytes(byte[] dataOriginal, int rotation) {
        if(!didReceiveImageParams){ return; }
        if(!didPrepareRenderScript){ return; }

        DEBUG_OUTPUT("Received bytes! (Rotation angle: " + rotation + ")");

        long step1MS = System.currentTimeMillis();

        convertYUVToRGB(bitmapOriginal, dataOriginal);

        float scaleX = (float)widthTransform / (float)widthOrig;
        float scaleY = (float)heightTransform / (float)heightOrig;
        float scale = Math.max(scaleX, scaleY);

        //------------------------------------
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
        //------------------------------------

        canvasTransform.setMatrix(m);
        canvasTransform.drawBitmap(bitmapOriginal, 0, 0, null);

        //convert bitmap to boof U8; prepare boof U8 image for edges
        ConvertBitmap.bitmapToGray(bitmapTransform, imageU8, workBuffer);

        long step2MS = System.currentTimeMillis();

        //DrawingUtilAndroid drawutil = new DrawingUtilAndroid(new Canvas(bitmapTransform));
        //docScanner.drawCannyDebug(drawutil);

        //scan
        tempCanvas.clearBitmap(Color.argb(255,0,0,0));
        PerspectiveRect rect = docScanner.scan(tempCanvas);

        //TODO
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
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int previewFormat) {
        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        bitmapOriginal = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

        int MAX_WORKING_DIM = 384;
        float scaleX = (float)MAX_WORKING_DIM / (float)this.getWidth();
        float scaleY = (float)MAX_WORKING_DIM / (float)this.getHeight();
        float scale = Math.min(scaleX, scaleY);

        this.widthTransform = (int)((float)this.getWidth() * scale);
        this.heightTransform = (int)((float)this.getHeight() * scale);
        bitmapTransform = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        canvasTransform = new Canvas(bitmapTransform);

        imageU8 = ConvertBitmap.bitmapToGray(bitmapTransform, (GrayU8)null, null);
        workBuffer = ConvertBitmap.declareStorage(bitmapTransform, null);
        docScanner = new DocScanUtil(imageU8);

        Bitmap bitmapExtra = Bitmap.createBitmap(widthTransform, heightTransform, Bitmap.Config.ARGB_8888);
        tempCanvas = new DrawableU8Android(bitmapExtra);

        didReceiveImageParams = true;

        //NV21: YUV 12 bits per pixel
        prepareRenderScriptYUVToRGB(widthOrig*heightOrig*3/2);

        DEBUG_OUTPUT("setImageParams received!");
        DEBUG_OUTPUT("- Raw preview size: (" + widthOrig + ", " + heightOrig + ")");
        DEBUG_OUTPUT("- Container size: (" + this.getWidth() + ", " + this.getHeight() + ")");
        DEBUG_OUTPUT("- Transform size: (" + widthTransform + ", " + heightTransform + ")");

    }
}
