package com.newcam.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.newcam.jniexports.JNIExports;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;

/**
 * Created by dan on 1/26/18.
 */

public class ImageprocState {

    public boolean magicColor = false; // Optional
    public boolean fourPointApplied = false; // Optional
    public float[] pRect = new float[8]; // Required iff fourPointApplied; normalized to [0,1]^2

    public void debugPrint(String tag) {
        System.out.println(tag + " ImageprocState:");
        System.out.println(tag + " - magicColor enabled?: " + (magicColor ? "Yes" : "No") + "");
        System.out.println(tag + " - fourPoint enabled?: " + (fourPointApplied ? "Yes" : "No") + "");
        if (fourPointApplied) {
            System.out.println(tag + " - fourPoint locations: " +
                    "(" + pRect[0] + ", " + pRect[1] + "), " +
                    "(" + pRect[2] + ", " + pRect[3] + "), " +
                    "(" + pRect[4] + ", " + pRect[5] + "), " +
                    "(" + pRect[6] + ", " + pRect[7] + ")");
        }
    }

    /** Write fields to a WritableMap */
    public WritableMap toWritableMap() {
        WritableMap data = Arguments.createMap();

        data.putBoolean("magicColor", magicColor);
        data.putBoolean("fourPointApplied", fourPointApplied);
        if (fourPointApplied) {
            WritableArray fourPointLocations = Arguments.createArray();
            for (int k=0; k<8; k++) {
                fourPointLocations.pushDouble((double)pRect[k]);
            }
        }

        return data;
    }

    /** Set fields from a ReadableMap.  Throws exception if invalid data */
    public void loadReadableMap(ReadableMap data) throws IllegalArgumentException {

        magicColor = ReadableMapUtils.getBoolOrFalse(data, "magicColor");
        fourPointApplied = ReadableMapUtils.getBoolOrFalse(data, "fourPointApplied");

        if (fourPointApplied) {
            ReadableArray fourPointLocations = ReadableMapUtils.getArrayOrNull(data, "fourPointLocations");
            if (fourPointLocations == null) {
                throw new IllegalArgumentException("Missing or invalid field 'fourPointLocations'");
            }
            if (fourPointLocations.size() != 8) {
                throw new IllegalArgumentException("Field 'fourPointLocations' must be [x1, y1, ..., x4, y4]");
            }
            for (int k=0; k<8; k++) {
                pRect[k] = (float)fourPointLocations.getDouble(k);
            }
        }
    }

    public Bitmap apply(Bitmap bitmapInput) {

        // Each step will apply an effect to bitmapCurrent and set bitmapCurrent to the new result.
        Bitmap bitmapCurrent = bitmapInput;

        // Magic color (if applicable)
        if (magicColor) {
            // Prepare input data
            int imgW = bitmapCurrent.getWidth();
            int imgH = bitmapCurrent.getHeight();
            int[] imageInputBGRA = new int[imgW * imgH];
            bitmapCurrent.getPixels(imageInputBGRA, 0, imgW, 0, 0, imgW, imgH);

            // Allocate output image
            int[] imageOutputBGRA = new int[imgW * imgH];

            // Perform magic color
            JNIExports.magicColor(imgW, imgH, imageInputBGRA, imageOutputBGRA);

            // Get Bitmap from output data
            Bitmap bitmapOutput = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888);
            bitmapOutput.setPixels(imageOutputBGRA, 0, imgW, 0, 0, imgW, imgH);

            // Update current Bitmap
            bitmapCurrent = bitmapOutput;
        }

        // Four point transformation (if applicable)
        if (fourPointApplied) {
            // Prepare input data
            int imgInputW = bitmapCurrent.getWidth();
            int imgInputH = bitmapCurrent.getHeight();
            int[] imageInputBGRA = new int[imgInputW * imgInputH];
            bitmapCurrent.getPixels(imageInputBGRA, 0, imgInputW, 0, 0, imgInputW, imgInputH);

            // Allocate output container
            int[] dimsImageOutput = new int[2];
            int MAX_OUTPUT_DIM = 2880;
            int[] dataOutput = new int[MAX_OUTPUT_DIM*MAX_OUTPUT_DIM]; // TODO this might be too big...

            // Do the transformation
            JNIExports.fourPoint(imgInputW, imgInputH, imageInputBGRA, dimsImageOutput, MAX_OUTPUT_DIM, dataOutput, pRect);

            // Get Bitmap from relevant region of output container
            int outputImageW = dimsImageOutput[0];
            int outputImageH = dimsImageOutput[1];
            Bitmap bitmapOutput = Bitmap.createBitmap(outputImageW, outputImageH, Bitmap.Config.ARGB_8888);
            bitmapOutput.setPixels(dataOutput, 0, outputImageW, 0, 0, outputImageW, outputImageH);

            // Update current Bitmap
            bitmapCurrent = bitmapOutput;
        }

        return bitmapCurrent;
    }
}
