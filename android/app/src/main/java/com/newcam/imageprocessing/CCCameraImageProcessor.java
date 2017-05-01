package com.newcam.imageprocessing;

/**
 * Created by dan on 5/1/17.
 */

public interface CCCameraImageProcessor {

    public void setBytes(byte[] data);
    public void setImageParams(int width, int height, int previewFormat);
}
