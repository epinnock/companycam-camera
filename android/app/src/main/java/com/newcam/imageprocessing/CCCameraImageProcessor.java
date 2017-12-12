package com.newcam.imageprocessing;

import android.graphics.Bitmap;

/**
 * Created by dan on 5/1/17.
 */

public interface CCCameraImageProcessor {

    public static interface ImageProcessorListener{
        public void receiveResult();
    }

    public void setListener(ImageProcessorListener listener);

    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM);

    /** Return true if the next frame should be processed, or false if processing can stop */
    public boolean setPreviewBytes(byte[] data, int rotation);

    public void clearVisiblePreview();

    public Bitmap getOutputImage();
}
