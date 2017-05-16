package com.newcam.imageprocessing;

import android.graphics.Bitmap;

/**
 * Created by dan on 5/1/17.
 */

public interface CCCameraImageProcessor {

    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM);
    public void setPreviewBytes(byte[] data, int rotation);
    public Bitmap getOutputImage();
}
