package com.newcam.jniexports;

/**
 * Created by dan on 1/17/18.
 */

public class JNIExports {

    static {
        System.loadLibrary("native-lib");
    }

    // NOTE: In initializing BGRA images: Java int is 4 bytes, so each int corresponds to one BGRA pixel
    public static native void convertYUVtoBGRA(int width, int height, byte[] imageYUV, int[] imageBGRA);

    public static native void magicColor(int width, int height, int[] imageInputBGRA, int[] imageOutputBGRA);

    public static native long newScanner();
    public static native void deleteScanner(long ptr);
    public static native void resetScanner(long ptr);
    public static native void nativeScan(long ptr,
        /* Image to be scanned */
        int width, int height, int[] imageInputBGRA,
        /* Image returned by the scanner, if any */
        int[] dimsImageOutput, int maxOutputPixels, int[] imageOutputBGRA,
        /* Info about most recent scan */
        int[] scanStatus, float[] pRect);

}
