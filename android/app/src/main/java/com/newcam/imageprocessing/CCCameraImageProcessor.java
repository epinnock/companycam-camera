package com.newcam.imageprocessing;

/**
 * Created by dan on 5/1/17.
 */

public interface CCCameraImageProcessor {

    public void setBytes(byte[] data, int rotation);
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int previewFormat);
}
