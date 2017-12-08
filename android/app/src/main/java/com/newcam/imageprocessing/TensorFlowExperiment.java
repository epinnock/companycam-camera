package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

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

    public TensorFlowExperiment(Context context) {
        super(context);
    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        System.out.println("GOT PREVIEW BYTES");
        // Load the model from disk.
        TensorFlowInferenceInterface inferenceInterface =
                new TensorFlowInferenceInterface(null, null);
        inferenceInterface.close();

//        // Copy the input data into TensorFlow.
//        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
//        // Run the inference call.
//        inferenceInterface.run(outputNames, logStats);
//        // Copy the output Tensor back into the output array.
//        inferenceInterface.fetch(outputName, outputs);

        return false;
    }

    @Override
    public void clearVisiblePreview() {

    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
