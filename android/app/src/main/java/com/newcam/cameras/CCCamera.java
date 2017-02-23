package com.newcam.cameras;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.widget.RelativeLayout;

import com.newcam.CCCameraManager;
import com.newcam.CCCameraView;
import com.newcam.R;
import com.newcam.utils.CCCameraInterface;
import com.notagilx.companycam.react_bridges.PhotoActions;
import com.notagilx.companycam.util.StorageUtility;
import com.notagilx.companycam.util.views.CameraPreview;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mattboyd on 2/5/17.
 */

public abstract class CCCamera implements CCCameraInterface {

    public Context mContext;
    private static final String APP_PACKAGE ="com.agilx.companycam";

    // The mCameraView is a reference to the latest CCCameraView that contains this camera object
    public CCCameraView mCameraView;

    // The mOrientationListener is used to record the last orientation for use when processing captured photos
    private OrientationEventListener mOrientationListener;
    public int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    // These strings are used to save and retrieve persistent settings to the SharedPreferences
    private static final String PREFS_FLASH_MODE = "PREFS_FLASH_MODE";
    private static final String PREFS_RESOLUTION_MODE = "PREFS_RESOLUTION_MODE";
    private static final String PREFS_CAMERA_MODE = "PREFS_CAMERA_MODE";

    // The mFlashMode string defines the flash mode to use
    // "auto" = auto flash
    // "on" = flash on
    // "off" = flash off
    // "torch" = flash continuously on
    public String mFlashMode;

    // The mResolutionMode string describes the image resolution to use
    // "normal" = lowest resolution
    // "high" = high resolution
    // "super" = highest resolution
    public String mResolutionMode;

    // The mCameraMode string describes the type of camera setting to use
    // "fastcam" = FastCam mode means that the user won't be given the option to edit photos after capturing them
    // "camera" = this is the default camera mode that allows the user to edit photos after capturing them
    // "scanner" = this is a mode that tries to identify documents in the photo and transforms the image to show a flattened version of the document
    public String mCameraMode;

    // The mPreview is a custom SurfaceView for rendering the camera preview
    public CameraPreview mPreview;

    // The mPreviewWidth and mPreviewHeight are the width and height for the preview for the current camera
    public int mPreviewWidth;
    public int mPreviewHeight;

    public CCCamera(Context context, CCCameraView cameraView) {
        mContext = context;

        // Set the reference to the mCameraView
        mCameraView = cameraView;

        // Get the saved settings from the SharedPreferences.  Restrict the possible flash modes to "torch" and "off".
        SharedPreferences preferences = getSharedPreferences();
        mFlashMode = preferences.getString(PREFS_FLASH_MODE, "off");
        if (!(mFlashMode.equals("torch") || mFlashMode.equals("off"))) {
            mFlashMode.equals("off");
        }
        mResolutionMode = preferences.getString(PREFS_RESOLUTION_MODE, "normal");
        mCameraMode = preferences.getString(PREFS_CAMERA_MODE, "camera");

        // Initialize the orientation listener
        initOrientationListener();
    }

    // This method initializes an orientation listener to record the last orientation which is used to process captured photos
    private void initOrientationListener() {
        mOrientationListener =
                new OrientationEventListener(mContext) {

                    public void onOrientationChanged(int orientation) {

                            // Record the last orientation value
                            mLastOrientation = orientation;
                    }
                };
        mOrientationListener.enable();
    }

    //////////////////////
    // Abstract methods //
    //////////////////////

    // This method starts the camera based on the camera API being implemented
    public abstract void startCamera();

    // This method releases the camera based on the camera API being implemented
    public abstract void releaseCamera();

    /////////////////////////
    // Persistent settings //
    /////////////////////////

    // This method persists the flash mode to the SharedPreferences
    public void persistFlashMode(String flashMode) {

        // Persist flash mode
        mFlashMode = flashMode;
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_FLASH_MODE, flashMode);
        editor.apply();
    }

    // This method persists the resolution mode to the SharedPreferences
    public void persistResoultionMode(String resolutionMode) {

        // Persist flash mode
        mResolutionMode = resolutionMode;
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_RESOLUTION_MODE, resolutionMode);
        editor.apply();
    }

    // This method persists the camera mode to the SharedPreferences
    public void persistCameraMode(String cameraMode) {

        // Persist flash mode
        mCameraMode = cameraMode;
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_CAMERA_MODE, cameraMode);
        editor.apply();
    }

    // This method retrieves the SharedPreferences object
    protected SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
    }

    ///////////////////////////
    // Miscellaneous methods //
    ///////////////////////////

    // This method returns the minimum desired image height in pixels for the given resolution setting
    public int getDesiredImageHeightForResolution(String resolutionMode) {
        if (resolutionMode.equals("super")) {
            return 2160;
        }
        else if (resolutionMode.equals("high")) {
            return 1920;
        }
        else {
            return 1440;
        }
    }

    // This method configures the size of the mPreviewLayout given the chosen camera preview size
    public void configurePreviewLayout() {

        if (mPreview == null) {
            System.out.println("mPreview was null when configurePreviewLayout was called");
            return;
        }

        // The preview layout needs to be sized so that the chosen camera preview size fits the bounds exactly.  However, in order to get the preview
        // to exactly fill the screen without being distorted, the preview layout needs to be sized larger than the screen with negative margins.

        // Get the height and width of the screen in portrait coordinates (where height > width)
        //TODO: I guess this should really be the view size and not the screen size?
        double screenWidth = (double) mCameraView.getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
        double screenHeight = (double) mCameraView.getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();
        System.out.println("screenWidth = " + screenWidth + " screenHeight = " + screenHeight);

        // Calculate the aspect ratio of the screen
        double screenAspectRatio = screenHeight/screenWidth;

        // Get the height and width of the chosen preview size in portrait coordinates (where height > width)
        int theWidth = mPreviewWidth;
        int theHeight = mPreviewHeight;
        System.out.println("theWidth = " + theWidth + " theHeight = " + theHeight);
        double previewWidth = (double) Math.min(mPreviewWidth, mPreviewHeight);
        double previewHeight =(double) Math.max(mPreviewWidth, mPreviewHeight);

        // Calculate the aspect ratio of the chosen preview size
        double previewAspectRatio = previewHeight/previewWidth;

        // Determine the necessary height and width for the preview layout that will cause the preview to fill the screen
        int newWidth = (int) screenWidth;
        int newHeight = (int) screenHeight;
        if (previewAspectRatio > screenAspectRatio) {
            newHeight = (int) (previewAspectRatio * newWidth);
        }
        else {
            newWidth = (int) (newHeight / previewAspectRatio);
        }

        // Set the layout parameters for the mPreview.
        RelativeLayout.LayoutParams mPreviewParams = new RelativeLayout.LayoutParams(newWidth, newHeight);

        int marginX = -(newWidth - (int)screenWidth)/2;
        int marginY = -(newHeight - (int)screenHeight)/2;

        System.out.println("left margin = " + marginX);
        System.out.println("top margin = " + marginY);
        System.out.println("right margin = " + marginX);
        System.out.println("bottom margin = " + marginY);

        mPreviewParams.setMargins(marginX, marginY, marginX, marginY);
        mPreview.setLayoutParams(mPreviewParams);
        mPreview.mWidth = newWidth;
        mPreview.mHeight = newHeight;
        mPreview.mLeft = marginX;
        mPreview.mTop = marginY;

        mPreview.requestLayout();
    }

    // This method determines the space between the first two fingers of the given touch event
    public double getFingerSpacing(MotionEvent event) {

        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    // This method gets the storage path for the photo file
    public File getPhotoPath() {
        File dir = mCameraView.getStoragePath();
        dir.mkdirs();

        return (new File(dir, StorageUtility.getNewFileName()));
    }

    // This method transitions to the photo editor after capturing a photo
    public void gotoEditPhotoCapture(String photoPath, int imgWidth, int imgHeight) {

        if (photoPath == null) {
            new AlertDialog.Builder(mContext)
                    .setTitle("Error")
                    .setMessage("Something went wrong while taking this photo. Try taking a picture with your camera app and uploading it.")
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();

            // Add the touch listener back to the mFrameLayout again
            //setupListeners();

            return;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("quality", mResolutionMode);

        File file = new File(photoPath);

        mCameraView.doPhotoTaken(file, imgWidth, imgHeight);
        mCameraView.finishWithResult("capture");
    }

    // This method uploads photos taken while in FastCam mode
    public void uploadFastCamPhoto(File photo, int imgWidth, int imgHeight) {

        // If saveToPhone is set, then save the image to the device in addition to sending it to the server.
        SharedPreferences preferences = getSharedPreferences();
        boolean saveToPhone = preferences.getBoolean(PhotoActions.PREF_KEY_SAVE_TO_PHONE, false);
        if (saveToPhone) {

            // Try writing the image to the device. This method will return null if the image can't be saved successfully.
            String imageURL = PhotoActions.writeImageToDevice(mContext, Uri.fromFile(photo));
        }

        CCCameraManager.getLatestView().doPhotoAccepted(photo, imgWidth, imgHeight);
    }
}
