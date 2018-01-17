package com.newcam.jniexports;

/**
 * Created by dan on 1/17/18.
 */

public class JNIExports {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static native long magicColor(
        /* Input image */
        int width, int height, byte[] imageYUV, int[] imageBGRA,
        /* Output image */
        int[] imageOutput);

    public static native long newScanner();
    public static native void deleteScanner(long ptr);
    public static native void resetScanner(long ptr);

    // NOTE: In initializing imageBGRA and imageOutput: Java int is 4 bytes, so each int corresponds to one BGRA pixel
    public static native void nativeScan(long ptr,
        /* Image to be scanned */
        int width, int height, byte[] imageYUV, int[] imageBGRA,
        /* Image returned by the scanner, if any */
        int[] dimsImageOutput, int maxOutputPixels, int[] imageOutput,
        /* Info about most recent scan */
        int[] scanStatus, float[] pRect);

}
