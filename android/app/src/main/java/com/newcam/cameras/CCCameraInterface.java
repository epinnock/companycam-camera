package com.newcam.cameras;

import android.view.MotionEvent;

import com.newcam.enums.FlashMode;
import com.newcam.enums.ResolutionMode;

/**
 * Created by mattboyd on 2/5/17.
 */

public interface CCCameraInterface {

    // The CCCameraInterface provides methods to operate a camera and query it about its properties.

    // This method sets the camera resolution based on the given string
    void setResolution(ResolutionMode mode);

    // This method returns a boolean that describes whether or not the device has an available front-facing camera
    boolean hasFrontCamera();

    // This method returns a boolean that describes whether or not the device has an available rear-facing camera
    boolean hasRearCamera();

    // This method toggles the camera between forward-facing and rear-facing
    void toggleCamera();

    // This method returns a boolean that describes whether or not the current camera has flash capability
    boolean hasFlash();

    // This method toggles the flash state
    void toggleFlash();

    // This method sets the flash state
    void setFlash(FlashMode mode);

    // This method captures a photo from the camera
    void takePicture();

    // This method handles a screen touch event
    boolean handleTouchEvent(MotionEvent event);
}
