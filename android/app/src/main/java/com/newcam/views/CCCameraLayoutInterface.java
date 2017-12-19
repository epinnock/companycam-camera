package com.newcam.views;

import com.newcam.enums.CameraMode;
import com.newcam.enums.FlashMode;
import com.newcam.enums.ResolutionMode;

/**
 * Created by mattboyd on 2/10/17.
 */

public interface CCCameraLayoutInterface {

    // The CCCameraLayoutInterface provides methods that allow an object to access certain UI features of a generic camera layout.

    // This method sets the flash button visibility
    void setFlashButtonVisibility();

    // This method sets the flash button image
    void setFlashModeImage(FlashMode mode);

    // This method sets the resolution button image
    void setResolutionImage(ResolutionMode mode);

    // This method sets the camera button visibility
    void setCameraButtonVisibility();

    // This method sets the camera mode layout features
    void setCameraMode(CameraMode mode);

    // This method shows the resolution layout
    void showResolutionLayout();

    // This method hides the resolution layout
    void hideResolutionLayout();

    // This method animates the screen flash after capturing a photo
    void animateScreenFlash();

    // This method shows an auto focus indicator view at the given position while the camera is focusing and/or exposing
    void showAutoFocusIndicator(float x, float y, final boolean setRepeating);

    // This method hides the auto focus indicator view
    void hideAutoFocusIndicator();
}
