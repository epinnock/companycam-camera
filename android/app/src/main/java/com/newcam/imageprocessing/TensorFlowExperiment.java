package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.io.InputStream;

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

    private static final int TF_IN_ROWS = 384;
    private static final int TF_IN_COLS = 216;
    private static final int TF_OUT_ROWS = 96;
    private static final int TF_OUT_COLS = 54;

    private static final String TF_MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";
    private static final String TF_INPUT_NODE = "data/input";
    private static final String TF_OUTPUT_NODE = "combo/output";

    private TensorFlowInferenceInterface tfi;
    private float[] floatArrayIn;
    private float[] floatArrayOut;

    // Overlay image (should match container size)
    protected int widthOverlay;
    protected int heightOverlay;
    protected Bitmap bitmapOverlay;
    protected Canvas canvasOverlay;
    protected boolean imageParamsHaveBeenSet = false;


    public TensorFlowExperiment(Context context) {
        super(context);

        // TODO: Only load when ImageProcessor starts--if done here, it will load when camera opens regardless of mode
        tfi = new TensorFlowInferenceInterface(getResources().getAssets(), TF_MODEL_FILE);
        floatArrayIn = new float[TF_IN_ROWS*TF_IN_COLS];
        floatArrayOut = new float[TF_OUT_ROWS*TF_OUT_COLS];

    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {
        widthOverlay = widthContainer;
        heightOverlay = heightContainer;
        bitmapOverlay = Bitmap.createBitmap(widthOverlay, heightOverlay, Bitmap.Config.ARGB_8888);
        canvasOverlay = new Canvas(bitmapOverlay);

        imageParamsHaveBeenSet = true;
    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {

        // TODO Convert preview bytes to appropriately sized float array


        // Feed input, compute and fetch output
        tfi.feed(TF_INPUT_NODE, floatArrayIn, 1, TF_IN_ROWS, TF_IN_COLS, 1);

        String[] outputNodes = { TF_OUTPUT_NODE };
        tfi.run(outputNodes);

        tfi.fetch(TF_OUTPUT_NODE, floatArrayOut);

        System.out.println("[TensorFlow] Feed, run, fetch: completed");

        // TODO Convert output float array to Bitmap for display


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
