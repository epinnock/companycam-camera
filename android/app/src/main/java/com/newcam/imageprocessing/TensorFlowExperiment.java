package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.nio.IntBuffer;

/**
 * Created by dan on 12/8/17.
 */

public class TensorFlowExperiment extends View implements CCCameraImageProcessor {

    // Listeners for auto-capture
    //--------------------------------------
    protected ImageProcessorListener listener;

    @Override
    public void setListener(ImageProcessorListener listener){
        this.listener = listener;
    }

    protected void notifyListeners(){
        if(listener == null){ return; }
        listener.receiveResult();
    }
    //--------------------------------------

    protected final static int COLOR_0000 = Color.argb(0,0,0,0);

    protected Context context;

    // TensorFlow stuff
    //--------------------------------------
    private static final int TF_IN_ROWS = 384;
    private static final int TF_IN_COLS = 216;
    private static final int TF_OUT_ROWS = 96;
    private static final int TF_OUT_COLS = 54;

    private static final String TF_MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";
    private static final String TF_INPUT_NODE = "data/input";
    private static final String TF_OUTPUT_NODE = "combo/output";

    private TensorFlowInferenceInterface tfi;
    private float[] floatArrayTFInput;
    private float[] floatArrayTFOutput;

    // TODO: Just used for the 'testing' blocks in setPreviewBytes for now
    //--------------------------------------
    private int[] cArrayTFInput;
    private int[] cArrayTFOutput;
    private Bitmap bitmapTFInput;
    private Bitmap bitmapTFOutput;
    private Canvas canvasTFInput;

    // Overlay image (should match container size)
    //--------------------------------------
    protected boolean imageParamsHaveBeenSet = false;
    protected int widthOverlay;
    protected int heightOverlay;
    protected Bitmap bitmapOverlay;
    protected Canvas canvasOverlay;

    // RenderScript (preview bytes -> Bitmap) stuff
    //--------------------------------------
    protected boolean didPrepareRenderScript = false;
    protected Allocation allocInYUV;
    protected Allocation allocOutRGBA;
    protected ScriptIntrinsicYuvToRGB scriptYUVtoRGB;

    protected int widthOrig;
    protected int heightOrig;
    protected Bitmap bitmapOriginal;



    public TensorFlowExperiment(Context context) {
        super(context);
        this.context = context;

        // TODO: Only load when ImageProcessor starts--if done here, it will load when camera opens regardless of mode
        tfi = new TensorFlowInferenceInterface(getResources().getAssets(), TF_MODEL_FILE);
        floatArrayTFInput = new float[TF_IN_ROWS * TF_IN_COLS];
        floatArrayTFOutput = new float[TF_OUT_ROWS * TF_OUT_COLS];

        // TODO: Just used for the 'testing' blocks in setPreviewBytes for now
        //---------------------------------------------------------
        // Used to convert between bitmaps and float arrays
        cArrayTFInput = new int[TF_IN_COLS * TF_IN_ROWS];
        cArrayTFOutput = new int[TF_OUT_COLS * TF_OUT_ROWS];
        bitmapTFInput = Bitmap.createBitmap(TF_IN_COLS, TF_IN_ROWS, Bitmap.Config.ARGB_8888);
        bitmapTFOutput = Bitmap.createBitmap(TF_OUT_COLS, TF_OUT_ROWS, Bitmap.Config.ARGB_8888);
        canvasTFInput = new Canvas(bitmapTFInput);
        //---------------------------------------------------------
    }

    // RenderScript stuff
    //====================================================================
    protected void prepareRenderScriptYUVToRGB(){
        System.out.println("RenderScript (YUV -> RGB): Preparing");

        RenderScript rs = RenderScript.create(context);
        scriptYUVtoRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        int byteCount = widthOrig*heightOrig*3/2;
        Type.Builder typeYUV = new Type.Builder(rs, Element.U8(rs)).setX(byteCount);
        allocInYUV = Allocation.createTyped(rs, typeYUV.create(), Allocation.USAGE_SCRIPT);

        Type.Builder typeRGBA = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(widthOrig).setY(heightOrig);
        allocOutRGBA = Allocation.createTyped(rs, typeRGBA.create(), Allocation.USAGE_SCRIPT);

        scriptYUVtoRGB.setInput(allocInYUV);
        didPrepareRenderScript = true;

        System.out.println("RenderScript (YUV -> RGB): Ready!");
    }

    protected void convertYUVToRGB(Bitmap target, byte[] data){
        if(!didPrepareRenderScript){ return; }

        allocInYUV.copyFrom(data);
        scriptYUVtoRGB.forEach(allocOutRGBA);
        allocOutRGBA.copyTo(target);
    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

        // YUV -> RGB conversion Bitmap and RenderScript converter
        this.widthOrig = widthOrig;
        this.heightOrig = heightOrig;
        bitmapOriginal = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888);

        prepareRenderScriptYUVToRGB();

        // Bitmaps/canvas for UI overlay
        widthOverlay = widthContainer;
        heightOverlay = heightContainer;
        bitmapOverlay = Bitmap.createBitmap(widthOverlay, heightOverlay, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        imageParamsHaveBeenSet = true;
    }

    protected Matrix getOrigToInput(int rotation) {
        int dimSrcLarge = Math.max(widthOrig, heightOrig);
        int dimSrcSmall = Math.min(widthOrig, heightOrig);

        int dimDstLarge = Math.max(TF_IN_COLS, TF_IN_ROWS);
        int dimDstSmall = Math.min(TF_IN_COLS, TF_IN_ROWS);

        float scaleL = (float)dimDstLarge / (float)dimSrcLarge;
        float scaleS = (float)dimDstSmall / (float)dimSrcSmall;
        float scale = Math.max(scaleL, scaleS);

        Matrix mTransFirst = new Matrix();
        mTransFirst.setTranslate(-(float)widthOrig/2.0f, -(float)heightOrig/2.0f);

        Matrix mScale = new Matrix();
        mScale.setScale(scale, scale);

        Matrix mRotate = new Matrix();
        mRotate.setRotate(rotation);

        Matrix mTransLast = new Matrix();
        mTransLast.setTranslate((float)TF_IN_COLS/2.0f, (float)TF_IN_ROWS/2.0f);

        Matrix mFinal = mTransFirst;
        mFinal.postConcat(mScale);
        mFinal.postConcat(mRotate);
        mFinal.postConcat(mTransLast);
        return mFinal;
    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        if (!imageParamsHaveBeenSet) { return true; }

        // Convert preview bytes -> bitmapOriginal
        convertYUVToRGB(bitmapOriginal, data);

        // TODO: Just for testing; use a better method!
        // Convert: bitmapOriginal -> bitmapTFInput -> cArrayTFInput -> floatArrayTFInput
        //---------------------------------------------------------
        Matrix matOrigToInput = getOrigToInput(rotation);
        canvasTFInput.drawBitmap(bitmapOriginal, matOrigToInput, null);

        int nPixelsIn = TF_IN_COLS * TF_IN_ROWS;
        bitmapTFInput.getPixels(cArrayTFInput, 0, TF_IN_COLS, 0, 0, TF_IN_COLS, TF_IN_ROWS);
        for (int i=0; i<nPixelsIn; i++) {
            int c = cArrayTFInput[i];
            floatArrayTFInput[i] = (0.299f*(float)Color.red(c) + 0.587f*(float)Color.red(c) + 0.114f*(float)Color.red(c))/255.0f;
        }
        //---------------------------------------------------------

        // Feed input, compute and fetch output
        tfi.feed(TF_INPUT_NODE, floatArrayTFInput, 1, TF_IN_ROWS, TF_IN_COLS, 1);
        String[] outputNodes = { TF_OUTPUT_NODE };
        tfi.run(outputNodes);
        tfi.fetch(TF_OUTPUT_NODE, floatArrayTFOutput);

        // TODO: Just for testing; use a better method!
        // Convert: floatArrayTFOutput -> cArrayTFOutput -> bitmapTFOutput -> bitmapOverlay
        //---------------------------------------------------------
        int nPixelsOut = TF_OUT_COLS * TF_OUT_ROWS;

        for (int i=0; i<nPixelsOut; i++) {
            int c = (int)(255.0f * floatArrayTFOutput[i]);
            cArrayTFOutput[i] = Color.argb(192, c, c, c);
        }
        bitmapTFOutput.copyPixelsFromBuffer(IntBuffer.wrap(cArrayTFOutput));

        bitmapOverlay.eraseColor(COLOR_0000);
        Rect rSrc = new Rect(0, 0, bitmapTFOutput.getWidth(), bitmapTFOutput.getHeight());
        Rect rDst = new Rect(0, 0, bitmapOverlay.getWidth(), bitmapOverlay.getHeight());

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvasOverlay.drawBitmap(bitmapTFOutput, rSrc, rDst, paint);
        //---------------------------------------------------------

        this.invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!imageParamsHaveBeenSet){ return; }

        Rect rSrc = new Rect(0, 0, bitmapOverlay.getWidth(), bitmapOverlay.getHeight());
        Rect rDst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmapOverlay, rSrc, rDst, null);
    }

    @Override
    public void clearVisiblePreview() {
        if(!imageParamsHaveBeenSet){ return; }

        bitmapOverlay.eraseColor(COLOR_0000);
        this.invalidate();
    }

    @Override
    public Bitmap getOutputImage() {
        // TODO
        return null;
    }
}
