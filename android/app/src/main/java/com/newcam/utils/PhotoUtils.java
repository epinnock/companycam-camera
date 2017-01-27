package com.newcam.utils;

import android.graphics.Bitmap;

/**
 * Created by dan on 1/27/17.
 */

public class PhotoUtils {

    // This method crops the given photo to have the same aspect ratio as the device screen
    public static Bitmap cropBitmapToScreen(Bitmap bPhoto, double screenWidth, double screenHeight) {

        // Calculate the aspect ratio of the screen
        double screenAspectRatio = screenHeight/screenWidth;

        // Get the height and width of the bitmap in portrait coordinates (where height > width)
        double bitmapWidth = Math.min(bPhoto.getWidth(), bPhoto.getHeight());
        double bitmapHeight = Math.max(bPhoto.getWidth(), bPhoto.getHeight());

        // Calculate the aspect ratio of the bitmap
        double bitmapAspectRatio = bitmapHeight/bitmapWidth;

        // Crop the bitmap evenly so that its aspect ratio matches the screen aspect ratio
        int newWidth = (int) bitmapWidth;
        int newHeight = (int) bitmapHeight;
        if (bitmapAspectRatio > screenAspectRatio) {
            newHeight = (int) (screenAspectRatio * newWidth);
        }
        else {
            newWidth = (int) (newHeight / screenAspectRatio);
        }

        int offsetX = ((int)bitmapWidth - newWidth)/2;
        int offsetY = ((int)bitmapHeight - newHeight)/2;

        // Double check to make sure that the crop region is smaller than the original bitmap.  Some devices with an exact 16x9 screen
        // may throw an exception when trying to crop 0 pixels off of the image.
        if (screenAspectRatio != bitmapAspectRatio && offsetX + newWidth <= (int)bitmapWidth && offsetY + newHeight <= (int)bitmapHeight) {

            // For some devices, the x,y coordinates of the bitmap change relative to the width and height that are returned by
            // bitmap.getWidth() and getHeight() when the device is rotated.  If this happens, then trying to create the new bitmap
            // will throw an IllegalArgumentException.  Catch that exception and try to reverse the coordinates if necessary
            try {
                return Bitmap.createBitmap(bPhoto, offsetX, offsetY, newWidth, newHeight);
            }
            catch (IllegalArgumentException iae) {

                // Try reversing the x,y coordinates
                try {
                    return Bitmap.createBitmap(bPhoto, offsetY, offsetX, newHeight, newWidth);
                }
                catch (IllegalArgumentException iae2) {

                    // If an IllegalArgumentException is still thrown, then simply return the original bitmap
                    return bPhoto;
                }
            }

        }
        else {
            return bPhoto;
        }
    }

}
