package com.newcam.cameras;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.RelativeLayout;

import com.newcam.CCCameraView;
import com.newcam.R;
import com.newcam.utils.ExifUtils;
import com.newcam.utils.PhotoUtils;
import com.notagilx.companycam.util.LogUtil;
import com.notagilx.companycam.util.StorageUtility;
import com.notagilx.companycam.util.views.CameraPreview;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mattboyd on 2/5/17.
 */

public class CCCamera1 extends CCCamera implements SurfaceHolder.Callback {

    private static String TAG = CCCamera1.class.getSimpleName();

    // The mCamera is the reference to the current camera and the mCameraId is the id of that camera
    private Camera mCamera;
    private int mCameraId;

    // mCameraType is a reference to the camera type (rear- or forward-facing) currently being used
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    // Define the maximum preview width and height guaranteed by the Camera1 API.
    private static final int MAX_PREVIEW_WIDTH = 4000;
    private static final int MAX_PREVIEW_HEIGHT = 3000;

    // The HIGH_QUALITY int is used to define the JPEG compression quality when processing a photo.
    private final int HIGH_QUALITY = 80;

    // The zoomdistance is used while handling pinch and zoom gestures to
    private double zoomdistance;

    public CCCamera1(Context context, CCCameraView cameraView) {
        super(context, cameraView);

        // Start the camera preview
        startCamera();
    }

    //////////////////////////
    // Camera setup methods //
    //////////////////////////

    // This method starts the camera
    @Override
    public void startCamera() {
        startPreview();
    }

    // This method releases the camera reference
    @Override
    public void releaseCamera() {

        System.err.println("[CCCamera1] Releasing camera");
        if (mCamera != null) {

            // Close the current camera
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            System.err.println("[CCCamera1] Success!");
        }else {
            System.err.println("[CCCamera1] Nothing to release!");
        }
    }

    // This method is a safe way to get an instance of a Camera object
    public Camera getCameraInstance() {
        Camera c = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == mCameraType) {
                try {

                    // Attempt to get a Camera instance
                    c = Camera.open(camIdx);
                    mCameraId = camIdx;
                    LogUtil.e(TAG, "The camera instance was retrieved.");
                    return c;
                }
                catch (Exception e){
                    // Camera is not available (in use or does not exist)
                }
            }
        }

        // Return null if camera is unavailable
        return c;
    }

    // This method sets the display orientation for the camera
    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = mCameraView.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    // This method updates the flash mode in the camera parameters
    private void updateFlashSetting(String flashMode) {

        if (mCamera != null) {
            Camera.Parameters p = mCamera.getParameters();

            // Make sure that setting the flash setting is supported or setting the camera parameters will fail
            if (p.getFlashMode() != null) {
                p.setFlashMode(flashMode);
                safeSetParameters(mCamera, p, "updateFlashSetting()");
            }
        }
    }

    // This method initializes the camera for the camera preview
    private void initializeCameraForPreview() {

        final Camera.Parameters p = mCamera.getParameters();

        List<String> supportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
        boolean hasContinuousFocus = supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        if (hasContinuousFocus) {
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            safeSetParameters(mCamera, p, "initializeCameraForPreview()");
        }

        // Create the preview if is hasn't been created before.  If it's already been created, then the camera preview can just be started again.
        if (mPreview == null) {
            mPreview = new CameraPreview(mContext);
            mPreview.getHolder().addCallback(this);
            mCameraView.mPreviewLayout.addView(mPreview);
        }
        else {
            if (mCamera != null) {
                try {

                    // Set the camera display orientation
                    setCameraDisplayOrientation(0, mCamera);

                    // Set the visibility of the flash button
                    mCameraView.mCameraLayout.setFlashButtonVisibility();
                    updateFlashSetting(mFlashMode);

                    // Set the resolution mode
                    setResolution(mResolutionMode);

                    // Start the camera preview
                    mCamera.setPreviewDisplay(mPreview.getHolder());
                    mCamera.startPreview();
                }
                catch (IOException ioe) {
                }
            }
        }
    }

    // This method safely sets the camera parameters and catches the RunTimeException that can be thrown if any of the parameters are
    // invalid.  It returns a boolean that describes whether or not the parameters were set without throwing the exception
    private boolean safeSetParameters(Camera camera, Camera.Parameters params, String message) {

        boolean safelySetParameters = false;

        // Try to set the camera parameters
        try {
            camera.setParameters(params);
            safelySetParameters = true;
        }
        catch (RuntimeException rte) {
            // If any of the parameters are invalid, trying to call setParameters will throw a RuntimeException.  The error doesn't
            // provide any information about which parameter was invalid.
            Log.d(TAG, "mCamera.setParameters failed when called from " + message);
        }

        return safelySetParameters;
    }

    // This method gets a reference to the camera and starts the camera preview
    private void startPreview() {
        System.err.println("[CCCamera1] Starting preview");

        // Create an instance of Camera
        mCamera = getCameraInstance();

        if (mCamera != null) {

            // These are returned in descending order
            Camera.Parameters param = mCamera.getParameters();
            List<Camera.Size> lsps = param.getSupportedPreviewSizes();

            // Choose the optimal preview size based on the available output sizes, the screen size, and the preview layout size.
            Camera.Size optimalSize;
            if (this.hasShortAspectRatio()) {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 4, 3);
            }
            else {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 16, 9);
            }

            // Update the camera parameters
            param.setPreviewSize(optimalSize.width, optimalSize.height);
            safeSetParameters(mCamera, param, "startPreview()");

            // Once the preview size is determined, updated the size of mPreview so that the camera view will fill the screen properly.
            mPreviewWidth = optimalSize.width;
            mPreviewHeight = optimalSize.height;
            configurePreviewLayout();

            // Initialize the camera for the preview
            initializeCameraForPreview();
            System.err.println("[CCCamera1] Success!");
        }
        else {

            // Finish with an error describing that the camera is already in use
            mCameraView.finishWithError("camera in use");
            System.err.println("[CCCamera1] Camera already in use!");
        }
    }

    //////////////////////
    // Camera callbacks //
    //////////////////////

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);

            // Animate the screen flash when the image is captured
            mCameraView.post(new Runnable(){
                @Override
                public void run() {
                    mCameraView.mCameraLayout.animateScreenFlash();
                }
            });
        }
    };

    private final Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            if (data != null) {
                Bitmap bPhoto = null;
                try {

                    int orientation = ((mLastOrientation + 45) / 90) * 90;
                    int rotation = 0;
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraId, info);

                    rotation = (info.orientation + orientation) % 360;

                    BitmapFactory.Options options = new BitmapFactory.Options();

                    // Decoding the data with inJustDecodeBounds = true returns a null bitmap, but it decodes the size without having to
                    // allocate memory for all the pixels.
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, options);

                    System.out.println("The data length is " + data.length);

                    // Decode bitmap
                    options.inJustDecodeBounds = false;

                    // Get the bitmap (options will resize it if set)
                    bPhoto = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                    // The attempt to decode the data can sometimes fail and return null.  If that happens, display a message to the user and restart the camera preview..
                    if (bPhoto == null) {

                        new AlertDialog.Builder(mContext)
                                .setTitle("Error")
                                .setMessage("Something went wrong while taking this photo. Try taking a picture with your camera app and uploading it.")
                                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mCamera.startPreview();
                                        //setupListeners();
                                        dialog.dismiss();
                                    }
                                }).create().show();

                        return;
                    }

                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);

                    Log.d(TAG, "calculations done, ready to rotate");
                    bPhoto = Bitmap.createBitmap(bPhoto, 0, 0, bPhoto.getWidth(), bPhoto.getHeight(), matrix, true);

                    Log.d(TAG, "Before cropping the photo is " + bPhoto.getWidth() + " x " + bPhoto.getHeight());

                    // Get the height and width of the screen in portrait coordinates (where height > width)
                    //TODO: I guess this should really be the view size and not the screen size?
                    double screenWidth = (double) mCameraView.getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
                    double screenHeight = (double) mCameraView.getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();

                    // Crop the image to the screen aspect ratio
                    bPhoto = PhotoUtils.cropBitmapToScreen(bPhoto, screenWidth, screenHeight);

                    Log.d(TAG, "After cropping the photo is " + bPhoto.getWidth() + " x " + bPhoto.getHeight());

                    Log.d(TAG, "bPhoto rotated and ready for storage.");

                    File photo = getPhotoPath();
                    if (photo.exists()) {
                        photo.delete();
                    }

                    FileOutputStream out = new FileOutputStream(photo.getPath());
                    BufferedOutputStream bos = new BufferedOutputStream(out);
                    bPhoto.compress(Bitmap.CompressFormat.JPEG, HIGH_QUALITY, bos);
                    int imgWidth = bPhoto.getWidth();
                    int imgHeight = bPhoto.getHeight();

                    bos.flush();
                    bos.close();
                    out.close();

                    // Transition to the EditPhotoCaptureActivity as long as the current mode isn't FastCam
                    if (!mCameraMode.equals("fastcam")) {
                        gotoEditPhotoCapture(photo.getPath(), imgWidth, imgHeight);
                    }

                    // If the current mode is FastCam, then upload the photo immediately
                    else {
                        uploadFastCamPhoto(photo, imgWidth, imgHeight);

                        // Start the camera preview again
                        mCamera.startPreview();
                        //setupListeners();
                    }

                    try {
                        ExifUtils.setAttributes(photo, mCameraView.getExifLocation(), mFlashMode);
                    } catch (IOException e) {
                        LogUtil.e(TAG, e.getLocalizedMessage());
                    }

                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                } catch (OutOfMemoryError oome) {
                    Log.e(TAG, "OutOfMemoryError: " + oome.getMessage());
                    mCameraView.finishWithError("Out of memory: " + oome.getMessage());
                } finally {
                    if (bPhoto != null) {
                        bPhoto.recycle();
                        bPhoto = null;
                    }
                }
            }
        }
    };

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectWidth       The width of the container view
     * @param aspectHeight      The height of the container view
     * @return The optimal {@code Camera.Size}, or an arbitrary one if none were big enough
     */
    private Camera.Size chooseOptimalSize(List<Camera.Size> choices, int maxWidth, int maxHeight, int aspectWidth, int aspectHeight) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();

        // Collect the supported resolutions that are smaller than the preview Surface
        List<Camera.Size> notBigEnough = new ArrayList<>();

        // Get the width and height of the reference size that's passed in.
        int w = aspectWidth;
        int h = aspectHeight;

        // Go through each of the camera's supported preview sizes and compare them to the reference size to find the optimal preview size to use.
        for (Camera.Size option : choices) {

            // Check if this preview size is no bigger than the max allowed width and height and that its aspect ratio matches the aspect ratio
            // of the reference size.
            if ((option.width <= maxWidth) && (option.height <= maxHeight) && (option.height*w == option.width*h)) {

                // Ignore any preview sizes that are exactly square
                if (option.width == option.height) {
                    continue;
                }

                // Check if the largest dimension of this preview size is at least as large as the minimum requirement for the current
                // resolution selection
                if (Math.max(option.width, option.height) >= getDesiredImageHeightForResolution(mResolutionMode)) {
                    bigEnough.add(option);
                }
                else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest preview size of those big enough. If there is none big enough, pick the largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CCCamera1.CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CCCamera1.CompareSizesByArea());
        }
        else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices.get(0);
        }
    }

    // This class compares two Size objects and returns a 1 if the area of the first Size is larger or a -1 if the area of the second Size is larger.
    static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {

            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    }

    // This method returns a boolean if the CCCameraView has an aspect ratio closer to 4:3 than 16:9
    public boolean hasShortAspectRatio() {

        // Find the largest preview size that will fit the aspect ratio of the preview layout.

        // Get the height and width of the screen in portrait coordinates (where height > width)
        //TODO: I guess this should really be the view size and not the screen size?
        int screenWidth = mCameraView.getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
        int screenHeight = mCameraView.getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();

        // Calculate the aspect ratio of the screen
        double screenAspectRatio = (double)screenHeight/(double)screenWidth;

        // Determine if the screen's aspect ratio is closer to 4x3 or 16x9
        double aspect4x3 = 4.0/3.0;
        double aspect16x9 = 16.0/9.0;
        boolean hasShortAspect = false;
        if (Math.abs(screenAspectRatio - aspect4x3) < Math.abs(screenAspectRatio - aspect16x9)) {
            hasShortAspect = true;
        }
        else {
            hasShortAspect = false;
        }

        return hasShortAspect;
    }

    ///////////////////////////////
    // CCCameraInterface methods //
    ///////////////////////////////

    @Override
    public void setResolution(String resolutionMode) {

        if (mCamera != null) {

            Camera.Parameters param = mCamera.getParameters();

            //these are returned in descending order
            List<Camera.Size> lsps = param.getSupportedPictureSizes();

            /*if (resolutionMode.equals("super")) {
                for (Camera.Size size : lsps) {
                    if (size.width < 2160) {
                        break;
                    }
                    else if (size.width == size.height) {
                        continue;
                    }
                    mPreviewWidth = size.width;
                    mPreviewHeight = size.height;
                }
                LogUtil.e(TAG, "IN SUPER HIGH RES MODE");
            }
            else if (resolutionMode.equals("high")) {
                for (Camera.Size size : lsps) {
                    if (size.width < 1920) {
                        break;
                    }
                    else if (size.width == size.height) {
                        continue;
                    }
                    mPreviewWidth = size.width;
                    mPreviewHeight = size.height;
                }
                LogUtil.e(TAG, "IN HIGH RES MODE");
            }
            else {
                for (Camera.Size size : lsps) {
                    if (size.width < 1440) {
                        break;
                    }
                    else if (size.width == size.height) {
                        continue;
                    }
                    mPreviewWidth = size.width;
                    mPreviewHeight = size.height;
                }
                LogUtil.e(TAG, "IN LOW RES MODE");
            }*/

            // Choose the optimal preview size based on the available output sizes, the screen size, and the preview layout size.
            Camera.Size optimalSize;
            if (this.hasShortAspectRatio()) {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 4, 3);
            }
            else {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 16, 9);
            }

            // Update the camera parameters
            param.setPictureSize(optimalSize.width, optimalSize.height);
            param.setJpegQuality(100);
            safeSetParameters(mCamera, param, "setResolution()");

            LogUtil.e(TAG, "Width is " + mCamera.getParameters().getPictureSize().width + " height is " + mCamera.getParameters().getPictureSize().height);
        }
    }

    @Override
    public boolean hasFrontCamera() {

        // Check if this device contains a front-facing camera
        boolean foundFrontCamera = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                foundFrontCamera = true;
            }
        }

        return foundFrontCamera;
    }

    @Override
    public boolean hasRearCamera() {

        // Check if this device contains a rear-facing camera
        boolean foundRearCamera = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                foundRearCamera = true;
            }
        }

        return foundRearCamera;
    }

    @Override
    public void toggleCamera() {

        if (mCameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else {
            mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        // Close the current camera
        releaseCamera();

        // Initialize the camera again
        startCamera();
    }

    @Override
    public boolean hasFlash() {

        if (mCamera == null) {
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        if (parameters.getFlashMode() == null) {
            return false;
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes == null || supportedFlashModes.isEmpty() || supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
            return false;
        }

        return true;
    }

    @Override
    public void toggleFlash() {

        // Uncomment this section to allow the user to toggle between all four different available flash modes
        /*if (mFlashMode.equals("auto")) {
            mFlashMode = "on";
        } else if (mFlashMode.equals("on")) {
            mFlashMode = "torch";
        } else if(mFlashMode.equals("torch")) {
            mFlashMode = "off";
        } else if (mFlashMode.equals("off")) {
            mFlashMode = "auto";
        }*/

        if(mFlashMode.equals("torch")) {
            mFlashMode = "off";
        }
        else {
            mFlashMode = "torch";
        }

        // Update the flash mode in the camera parameters
        updateFlashSetting(mFlashMode);
    }

    @Override
    public void takePicture() {

        if (mCamera != null) {
            try {
                mCamera.takePicture(shutterCallback, null, mPicture);

                // Remove the touch listener from the mFrameLayout after a picture has been taken
                mCameraView.mPreviewLayout.setOnTouchListener(null);
            } catch (RuntimeException re) {
                Log.e(TAG, "RuntimeEx takePicture" + re.getMessage());
            }
        } else {
            //TODO Add snackbar
            //http://www.androidhive.info/2015/09/android-material-design-snackbar-example/
        }
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event) {

        // If the camera is null, try to get a reference to it again
        if (mCamera == null) {
            LogUtil.e(TAG, "The camera instance was null when the screen was touched.");

            // Call startPreview to get a reference to the camera and setup the camera preview
            startPreview();

            return false;
        }

        // Handle the touch event as long as the camera isn't null
        if (mCamera != null) {

            // Get the pointer ID
            final Camera.Parameters params = mCamera.getParameters();
            final int action = event.getAction();
            if (event.getPointerCount() > 1) {
                // handle multi-touch events
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    zoomdistance = getFingerSpacing(event);
                } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                    mCamera.cancelAutoFocus();
                    handleZoom(event, params);
                }
            } else {
                // handle single touch events
                if (action == MotionEvent.ACTION_UP) {
                    handleFocus(event, params);
                }
            }
        } else {
            LogUtil.e(TAG, "Unable to retrieve camera instance in the touch listener.");
            return false;
        }

        return true;
    }

    //////////////////////////
    // Touch event handling //
    //////////////////////////

    // This method handles zoom events
    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        double newDist = getFingerSpacing(event);
        if (newDist > zoomdistance) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < zoomdistance) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        zoomdistance = newDist;
        params.setZoom(zoom);
        safeSetParameters(mCamera, params, "handleZoom()");
    }

    // This method handles auto focus events
    public void handleFocus(MotionEvent event, Camera.Parameters params) {

        LogUtil.e(TAG, "handleFocus was called");

        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);

        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {

            // Check to make sure the surface has been created, otherwise the autofocus will fail
            if (mPreview != null && mPreview.mSurfaceCreated) {

                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        // currently set to auto-focus on single touch
                        LogUtil.e(TAG, "handleFocus was called and autofocus was actually fired");
                    }
                });
            }
        }
    }

    ///////////////////////////////////
    // SurfaceHolderCallback methods //
    ///////////////////////////////////

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated");

        // Set the SurfaceCreated flag
        mPreview.mSurfaceCreated = true;

        // The Surface has been created, now tell the camera where to draw the preview.
        try {

            if (mCamera == null) {

                // Create an instance of Camera
                mCamera = getCameraInstance();
            }

            // These are returned in descending order
            Camera.Parameters param = mCamera.getParameters();
            List<Camera.Size> lsps = param.getSupportedPreviewSizes();

            // Choose the optimal preview size based on the available output sizes, the screen size, and the preview layout size.
            Camera.Size optimalSize;
            if (this.hasShortAspectRatio()) {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 4, 3);
            }
            else {
                optimalSize = chooseOptimalSize(lsps, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, 16, 9);
            }

            // Update the camera parameters
            param.setPreviewSize(optimalSize.width, optimalSize.height);
            safeSetParameters(mCamera, param, "surfaceCreated()");

            // Once the preview size is determined, updated the size of mPreview so that the camera view will fill the screen properly.
            mPreviewWidth = optimalSize.width;
            mPreviewHeight = optimalSize.height;
            configurePreviewLayout();

            // Set the camera display orientation
            setCameraDisplayOrientation(0, mCamera);

            // Set the visibility of the flash button
            mCameraView.mCameraLayout.setFlashButtonVisibility();
            updateFlashSetting(mFlashMode);

            // Set the resolution mode
            setResolution(mResolutionMode);

            // Start the camera preview
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            /*if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
            else {

                if (mCamera != null) {

                    // Set the camera display orientation
                    setCameraDisplayOrientation(0, mCamera);

                    // Set the visibility of the flash button
                    mCameraView.mCameraLayout.setFlashButtonVisibility();
                    updateFlashSetting(mFlashMode);

                    // Set the resolution mode
                    setResolution(mResolutionMode);
                }
            }*/
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.d(TAG, "surfaceDestroyed");

        // Set the mSurfaceCreated flag
        mPreview.mSurfaceCreated = false;

        // Release the camera
        releaseCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Log.d(TAG, "surfaceChanged");

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mPreview.getHolder().getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}
