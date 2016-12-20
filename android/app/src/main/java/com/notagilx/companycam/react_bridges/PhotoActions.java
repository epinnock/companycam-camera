package com.notagilx.companycam.react_bridges;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

public class PhotoActions {

    public static final String PREF_KEY_SAVE_TO_PHONE = "saveToPhone";

    public PhotoActions() {

    }

    // This method saves an image to the device given the Uri.  It returns the URL if the image if it was saved successfully, otherwise
    // it returns null
    public static String writeImageToDevice(Context context, Uri imageUri) {

        String imageURL = null;

        try {

            // Get the bitmap from this Uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Save the image
            // The insertImage method will either return the URL of the saved image or null if the image couldn't be saved
            imageURL = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "", "");
        }
        catch (IOException ioe) {
        }

        return imageURL;
    }
}
