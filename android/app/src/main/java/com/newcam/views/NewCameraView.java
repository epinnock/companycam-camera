package com.newcam.views;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.location.Location;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.newcam.CCCameraView;
import com.newcam.R;
import com.notagilx.companycam.core.events.OutOfMemoryEvent;
import com.notagilx.companycam.react_bridges.PhotoActions;
import com.notagilx.companycam.util.ImageEditorUtility;
import com.notagilx.companycam.util.LogUtil;
import com.notagilx.companycam.util.SingleClickListener;
import com.notagilx.companycam.util.StorageUtility;
import com.notagilx.companycam.util.views.CameraPreview;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NewCameraView extends CCCameraView implements SurfaceHolder.Callback {

    private static String TAG = NewCameraView.class.getSimpleName();

    private static final String APP_PACKAGE ="com.agilx.companycam";
    private static final String OOME_STRING = "Out of memory!"; //TODO: getString(R.string.oome_camera);

    private static final String PREFS_FLASH_MODE = "PREFS_FLASH_MODE";
    private static final String PREFS_RESOLUTION_MODE = "PREFS_RESOLUTION_MODE";
    private static final String PREFS_CAMERA_MODE = "PREFS_CAMERA_MODE";

    public static final int PORTRAIT_TOP_UP = 1;
    public static final int PORTRAIT_TOP_DOWN = 2;
    public static final int LANDSCAPE_TOP_LEFT = 3;
    public static final int LANDSCAPE_TOP_RIGHT = 4;

    private Camera mCamera;
    private int mCameraId;
    private CameraPreview mPreview;
    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private RelativeLayout mPreviewLayout;
    private ImageButton mCaptureButton;

    // mCameraType is a reference to the camera type (rear- or forward-facing) currently being used
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    // The mFlashMode string defines the flash mode to use
    // "auto" = auto flash
    // "on" = flash on
    // "off" = flash off
    // "torch" = flash continuously on
    private String mFlashMode;

    // The mResolutionMode string describes the image resolution to use
    // "normal" = lowest resolution
    // "high" = high resolution
    // "super" = highest resolution
    private String mResolutionMode;

    // The mCameraMode string describes the type of camera setting to use
    // "fastcam" = FastCam mode means that the user won't be given the option to edit photos after capturing them
    // "camera" = this is the default camera mode that allows the user to edit photos after capturing them
    // "scanner" = this is a mode that tries to identify documents in the photo and transforms the image to show a flattened version of the document
    private String mCameraMode;

    // The mPhonePosition is used as a helper to track the orientation of the device.
    private int mPhonePosition;

    // The HIGH_QUALITY int is used to define the JPEG compression quality when processing a photo.
    private final int HIGH_QUALITY = 80;

    private double zoomdistance;
    ExifInterface exif;
    private Location mLastLocation;

    // Permissions required to take a picture
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    protected LinearLayout mLabelTouchTarget;
    protected ImageButton mToggleResolution;
    protected ImageButton mToggleFlash;
    protected ImageButton mCloseButton;

    // The mToggleCamera button allows the user to switch between rear- and forward-facing cameras
    protected ImageButton mToggleCamera;

    // The mTopLayout contains the place label, close button, and resolution button
    private LinearLayout mTopLayout;

    // The mBottomLayout contains the camera buttons and camera mode labels
    private LinearLayout mBottomLayout;

    // These views and text labels are for the camera options labels
    private LinearLayout mFastCamLayout;
    private ImageView mFastCamIndicator;
    private TextView mFastCamLabel;
    private LinearLayout mCameraLayout;
    private ImageView mCameraIndicator;
    private TextView mCameraLabel;
    private LinearLayout mScannerLayout;
    private ImageView mScannerIndicator;
    private TextView mScannerLabel;

    // These views and text labels are for the resolution selection layout
    private LinearLayout mResolutionLayout;
    private ImageButton mNormalButton;
    private ImageButton mHighButton;
    private ImageButton mSuperButton;
    private TextView mResolutionLabel1;
    private TextView mResolutionLabel2;
    private ImageButton mResolutionDismissButton;

    // These views and text labels are for the resolution selection layout in landscape
    private LinearLayout mResolutionLayoutLand;
    private ImageButton mNormalButtonLand;
    private ImageButton mHighButtonLand;
    private ImageButton mSuperButtonLand;
    private LinearLayout mResolutionLabelLayoutNormal;
    private LinearLayout mResolutionLabelLayoutHigh;
    private LinearLayout mResolutionLabelLayoutSuper;
    private ImageButton mResolutionDismissButtonLand;

    // This is the animation distance for the resolution layout in dp
    private final int RESOLUTION_ANIMATION_DIST_DP = 150;

    // The mResolutionLayoutVisible flag indicates whether or not the resolution layout is currently visible
    private boolean mResolutionLayoutVisible = false;

    // The mScreenFlashView is used to provide a flash effect when the user snaps a photo
    private FrameLayout mScreenFlashView;

    // The CLICK_REJECTION_INTERVAL is an amount of time in milliseconds that must elapse before a button click will be processed.
    // This is used to reject multiple clicks in quick succession.
    private static int CLICK_REJECTION_INTERVAL = 1500;

    public NewCameraView(Context context) {
        super(context);
    }

    @Override
    public void init(){

        // Verify that the permissions exist in case user turned them off while on the camera preview
        // Close the activity if the permissions aren't available
        if (!checkCameraPermissions()) {
            finishWithError("No camera permissions");
            return;
        }

        // Get references to the subviews
        mPreviewLayout = (RelativeLayout) findViewById(R.id.camera_preview);
        mPlaceName = (TextView) findViewById(R.id.place_name);
        mPlaceAddress = (TextView) findViewById(R.id.place_address);
        mCaptureButton = (ImageButton) findViewById(R.id.capture);
        mTopLayout = (LinearLayout) findViewById(R.id.top_layout);
        mBottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
        mFastCamLayout = (LinearLayout) findViewById(R.id.fastcam_layout);
        mFastCamLabel = (TextView) findViewById(R.id.fastcam_label);
        mFastCamIndicator = (ImageView) findViewById(R.id.fastcam_selected_icon);
        mCameraLayout = (LinearLayout) findViewById(R.id.camera_layout);
        mCameraLabel = (TextView) findViewById(R.id.camera_label);
        mCameraIndicator = (ImageView) findViewById(R.id.camera_selected_icon);
        mScannerLayout = (LinearLayout) findViewById(R.id.scanner_layout);
        mScannerLabel = (TextView) findViewById(R.id.scanner_label);
        mScannerIndicator = (ImageView) findViewById(R.id.scanner_selected_icon);
        mResolutionLayout = (LinearLayout) findViewById(R.id.resolution_layout);
        mNormalButton = (ImageButton) findViewById(R.id.normal_button);
        mHighButton = (ImageButton) findViewById(R.id.high_button);
        mSuperButton = (ImageButton) findViewById(R.id.super_button);
        mResolutionLabelLayoutNormal = (LinearLayout) findViewById(R.id.resolution_text_layout_1);
        mResolutionLabelLayoutHigh = (LinearLayout) findViewById(R.id.resolution_text_layout_2);
        mResolutionLabelLayoutSuper = (LinearLayout) findViewById(R.id.resolution_text_layout_3);
        mResolutionDismissButton = (ImageButton) findViewById(R.id.resolution_dismiss_button);
        mResolutionLayoutLand = (LinearLayout) findViewById(R.id.resolution_layout_land);
        mNormalButtonLand = (ImageButton) findViewById(R.id.normal_button_land);
        mHighButtonLand = (ImageButton) findViewById(R.id.high_button_land);
        mSuperButtonLand = (ImageButton) findViewById(R.id.super_button_land);
        mResolutionLabel1 = (TextView) findViewById(R.id.resolution_text_1);
        mResolutionLabel2 = (TextView) findViewById(R.id.resolution_text_2);
        mResolutionDismissButtonLand = (ImageButton) findViewById(R.id.resolution_dismiss_button_land);
        mScreenFlashView = (FrameLayout) findViewById(R.id.screen_flash_view);

        // Set the gradient backgrounds for the layouts
        mTopLayout.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.transparent_gray_gradient_270));
        mBottomLayout.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.transparent_gray_gradient_90));

        mLabelTouchTarget = (LinearLayout) findViewById(R.id.label_touch_target);
        mToggleResolution = (ImageButton) findViewById(R.id.toggle_resolution);
        mToggleFlash = (ImageButton) findViewById(R.id.toggle_flash);
        mCloseButton = (ImageButton) findViewById(R.id.close_button);
        mToggleCamera = (ImageButton) findViewById(R.id.toggle_camera);

        // Set the place name label
        mPlaceName.setText("This will be set later!!"); //TODO this is a placeholder for testing and should be removed

        // Set the button orientations for the resolution layout
        setupResolutionLayout();

        //mEventBus.register(this); //TODO

        // Get the saved settings from the SharedPreferences.  Restrict the possible flash modes to "torch" and "off".
        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        mFlashMode = preferences.getString(PREFS_FLASH_MODE, "off");
        if (!(mFlashMode.equals("torch") || mFlashMode.equals("off"))) {
            mFlashMode.equals("off");
        }
        mResolutionMode = preferences.getString(PREFS_RESOLUTION_MODE, "normal");
        mCameraMode = preferences.getString(PREFS_CAMERA_MODE, "camera");
        setCameraMode(mCameraMode);

        // Set the default button orientations
        int rotationValue = 0;
        mCloseButton.setRotation(rotationValue);
        mToggleResolution.setRotation(rotationValue);
        mToggleFlash.setRotation(rotationValue);
        mToggleCamera.setRotation(rotationValue);

        startPreview();

        initOrientationListener();

        // Set the visibility of the flash button
        setFlashButtonVisibility();

        // Set the visibility of the camera button
        setCameraButtonVisibility();
    }

    public void labelTouch() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // Finish the activity with a result
        finishWithResult("label");
    }

    protected void toggleResolution() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // The action for the close button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape
        if (mPhonePosition == PORTRAIT_TOP_UP) {

            // This is the resolution button action
            showResolutionLayout();
        }
        else {

            // This is the close button action
            // Finish the activity
            finishWithResult("close");
        }
    }

    protected void toggleFlashTapped() {

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
        setFlashModeImage(mFlashMode);

        updateFlashSetting(mFlashMode);
    }

    protected void closeButtonClick() {
        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // The action for the close button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape
        if (mPhonePosition == PORTRAIT_TOP_UP) {

            // This is the close button action
            // Finish the activity
            finishWithResult("close");
        }
        else {

            // This is the resolution button action
            showResolutionLayout();
        }
    }

    // This method sets the proper button orientation for the mResolutionLayout
    private void setupResolutionLayout() {
        mNormalButtonLand.setRotation(90);
        mHighButtonLand.setRotation(90);
        mSuperButtonLand.setRotation(90);
        mResolutionDismissButtonLand.setRotation(180);
    }

    // This method animates the presentation of the resolution layout when the resolution button is tapped
    private void showResolutionLayout() {

        // Perform the correct animation based on the device orientation
        if (mPhonePosition == PORTRAIT_TOP_UP) {

            // Set the opacity of the resolution layout to 0 before starting the animation
            mResolutionLayout.setAlpha(0.0f);

            // Animate the position and opacity of the resolution layout
            mResolutionLayout.animate().y(0.0f).alpha(1.0f).setDuration(300).start();

            // Animate the opacity of the top layout
            mTopLayout.animate().alpha(0.0f).setDuration(300).start();
        }
        else {

            // Set the opacity of the landscape resolution layout to 0 before starting the animation
            mResolutionLayoutLand.setAlpha(0.0f);

            // Convert the RESOLUTION_ANIMATION_DIST_DP to pixels
            float animationDistPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RESOLUTION_ANIMATION_DIST_DP, getResources().getDisplayMetrics());
            float screenWidthPx = this.getWidth();

            // Animate the position and opacity of the landscape resolution layout
            mResolutionLayoutLand.animate().x(screenWidthPx - animationDistPx).alpha(1.0f).setDuration(300).start();

            // Animate the opacity of the top and bottom layouts
            mTopLayout.animate().alpha(0.0f).setDuration(300).start();
            mBottomLayout.animate().alpha(0.0f).setDuration(300).start();
        }

        // Set the mResolutionLayoutVisible flag
        mResolutionLayoutVisible = true;
    }

    // This method animates the dismissal of the resolution layout
    private void hideResolutionLayout() {

        // Convert the RESOLUTION_ANIMATION_DIST_DP to pixels
        float animationDistPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RESOLUTION_ANIMATION_DIST_DP, getResources().getDisplayMetrics());

        // Perform the correct animation based on the device orientation
        if (mPhonePosition == PORTRAIT_TOP_UP) {

            // Animate the position and opacity of the resolution layout
            mResolutionLayout.animate().y(-animationDistPx).alpha(0.0f).setDuration(300).start();

            // Animate the opacity of the top layout
            mTopLayout.animate().alpha(1.0f).setDuration(300).start();
        }
        else {

            // Animate the position and opacity of the landscape resolution layout
            mResolutionLayoutLand.animate().x(this.getWidth()).alpha(0.0f).setDuration(300).start();

            // Animate the opacity of the top and bottom layouts
            mTopLayout.animate().alpha(1.0f).setDuration(300).start();
            mBottomLayout.animate().alpha(1.0f).setDuration(300).start();
        }

        // Set the mResolutionLayoutVisible flag
        mResolutionLayoutVisible = false;
    }


    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance() {
        Camera c = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == mCameraType) {
                try {
                    c = Camera.open(camIdx); // attempt to get a Camera instance
                    mCameraId = camIdx;
                    LogUtil.e(TAG, "The camera instance was retrieved.");
                    return c;
                }
                catch (Exception e){
                    // Camera is not available (in use or does not exist)
                }
            }
        }

        return c; // returns null if camera is unavailable
    }

    // This method releases the camera reference
    @Override
    public void releaseCamera() {

        if (mCamera != null) {

            // Close the current camera
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void initOrientationListener() {
        mOrientationListener =
                new OrientationEventListener(getContext()) {

                    public void onOrientationChanged(int orientation) {
                        // We keep the last known orientation. So if the user
                        // first orient the camera then point the camera to
                        // floor/sky, we still have the correct orientation.
                        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN && mPreview != null) {

                            // Change the rotation of the interface elements if the device has crossed the threshold between portrait and landscape
                            int rotationValue = -1;
                            if ((orientation >= 315 || orientation < 45) && !(mLastOrientation >= 315 || mLastOrientation < 45)) {
                                rotationValue = 0;

                                // Hide the resolution layout if it's showing
                                if (mResolutionLayoutVisible) {
                                    hideResolutionLayout();
                                }

                                mPhonePosition = PORTRAIT_TOP_UP;

                                // Set the icons for the mToggleResolution and mCloseButton
                                setResolutionImage(mResolutionMode);
                                mCloseButton.setImageResource(R.drawable.close_icon);

                            }
                            else if ((orientation < 315 && orientation >= 225) && !(mLastOrientation < 315 && mLastOrientation >= 225)) {
                                rotationValue = 90;

                                // Hide the resolution layout if it's showing
                                if (mResolutionLayoutVisible) {
                                    hideResolutionLayout();
                                }

                                mPhonePosition = LANDSCAPE_TOP_LEFT;

                                // Set the icons for the mToggleResolution and mCloseButton
                                mToggleResolution.setImageResource(R.drawable.close_icon);
                                setResolutionImage(mResolutionMode);
                            }
                            else if (mLastOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                                rotationValue = 0;

                                // Hide the resolution layout if it's showing
                                if (mResolutionLayoutVisible) {
                                    hideResolutionLayout();
                                }
                                mPhonePosition = PORTRAIT_TOP_UP;
                            }

                            if (rotationValue != -1) {

                                mCloseButton.animate().rotation(rotationValue);
                                mToggleResolution.animate().rotation(rotationValue);
                                mToggleFlash.animate().rotation(rotationValue);
                                mToggleCamera.animate().rotation(rotationValue);
                            }

                            // Record the last orientation value
                            mLastOrientation = orientation;
                        }
                    }
                };
        mOrientationListener.enable();
    }

    private void startPreview() {
        // Create an instance of Camera
        mCamera = getCameraInstance();

        if (mCamera != null) {

            // Initialize the camera for the preview
            initializeCameraForPreview();
        }
        else {

            // Finish with an error describing that the camera is already in use
            finishWithError("camera in use");
        }
    }

    // This method initializes the camera for the camera preview
    private void initializeCameraForPreview() {

        final Camera.Parameters p = mCamera.getParameters();

        List<String> supportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
        boolean hasContinuousFocus = supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        if (hasContinuousFocus) {
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(p);
        }

        // The previous implementation initialized the CameraPreview with a Place object from Realm.  The new React Native implementation
        // initializes the CameraPreview with the placeName and placeAddress given directly from the Javascript app.
        // Create the preview if is hasn't been created before.  If it's already been created, then the camera preview can just be started again.
        if (mPreview == null) {
            mPreview = new CameraPreview(getContext());
            mPreview.getHolder().addCallback(this);
            mPreviewLayout = (RelativeLayout) findViewById(R.id.camera_preview);
            mPreviewLayout.addView(mPreview);
        }
        else {
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(mPreview.getHolder());
                    mCamera.startPreview();
                }
                catch (IOException ioe) {
                }
            }
        }

        setCameraDisplayOrientation(0, mCamera);

        // Set the visibility of the flash button
        setFlashButtonVisibility();

        setFlashModeImage(mFlashMode);
        updateFlashSetting(mFlashMode);
        setResolution(mResolutionMode);

        //Set up Listeners
        setupListeners();
    }

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            AudioManager mgr = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);

            // Animate the screen flash when the image is captured if the camera is in FastCam mode.
            if (mCameraMode.equals("fastcam")) {
                post(new Runnable(){
                    @Override
                    public void run() {
                        animateScreenFlash();
                    }
                });
            }
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

                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage("Something went wrong while taking this photo. Try taking a picture with your camera app and uploading it.")
                                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mCamera.startPreview();
                                        setupListeners();
                                        dialog.dismiss();
                                    }
                                }).create().show();

                        return;
                    }

                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    bPhoto = Bitmap.createBitmap(bPhoto, 0, 0, bPhoto.getWidth(), bPhoto.getHeight(), matrix, true);

                    File photo = getPhotoPath();
                    if (photo.exists()) {
                        photo.delete();
                    }

                    FileOutputStream out = new FileOutputStream(photo.getPath());
                    BufferedOutputStream bos = new BufferedOutputStream(out);
                    bPhoto.compress(Bitmap.CompressFormat.JPEG, HIGH_QUALITY, bos);
                    bos.flush();
                    bos.close();
                    out.close();

                    // Transition to the EditPhotoCaptureActivity as long as the current mode isn't FastCam
                    if (!mCameraMode.equals("fastcam")) {
                        gotoEditPhotoCapture(photo.getPath());
                    }

                    // If the current mode is FastCam, then upload the photo immediately
                    else {
                        uploadFastCamPhoto(photo);

                        // Start the camera preview again
                        mCamera.startPreview();
                        setupListeners();
                    }

                    requestSingleLocationUpdate();
                    mLastLocation = getLastLocation();

                    if (mLastLocation != null) {
                        Log.e("TAG", "GPS is on");
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();
                    }
                    else{
                        requestLastLocation();
                        mLastLocation = getLastLocation();
                    }

                    try {
                        exif = new ExifInterface(photo.getPath());

                        double latitude = 0;
                        double longitude = 0;
                        if (mLastLocation != null) {
                            latitude = Math.abs(mLastLocation.getLatitude());
                            longitude = Math.abs(mLastLocation.getLongitude());
                        }

                        int num1Lat = (int)Math.floor(latitude);
                        int num2Lat = (int)Math.floor((latitude - num1Lat) * 60);
                        double num3Lat = (latitude - ((double)num1Lat+((double)num2Lat/60))) * 3600000;

                        int num1Lon = (int)Math.floor(longitude);
                        int num2Lon = (int)Math.floor((longitude - num1Lon) * 60);
                        double num3Lon = (longitude - ((double)num1Lon+((double)num2Lon/60))) * 3600000;

                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat+"/1,"+num2Lat+"/1,"+num3Lat+"/1000");
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon+"/1,"+num2Lon+"/1,"+num3Lon+"/1000");

                        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
                        mFlashMode = preferences.getString(PREFS_FLASH_MODE, "auto");


                        exif.setAttribute(ExifInterface.TAG_FLASH, mFlashMode);

                        Calendar calendar = Calendar.getInstance();

                        calendar.setTimeInMillis(mLastLocation.getTime());
                        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                        int minutes = calendar.get(Calendar.MINUTE);
                        int seconds = calendar.get(Calendar.SECOND);

                        String exifGPSTimestamp = hourOfDay + "/1," + minutes + "/1," + seconds + "/1";

                        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exifGPSTimestamp);
                        exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL);
                        exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER);



                        if (mLastLocation.getLatitude() > 0) {
                            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                        } else {
                            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                        }

                        if (mLastLocation.getLongitude() > 0) {
                            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                        } else {
                            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                        }

                        exif.saveAttributes();
                    } catch (IOException e) {
                        LogUtil.e(TAG, e.getLocalizedMessage());
                    }

                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                } catch (OutOfMemoryError oome) {
                    Log.e(TAG, "OutOfMemoryError: " + oome.getMessage());
                    //EventBus.getDefault().post(new OutOfMemoryEvent(OOME_STRING));
                    finishWithError("Out of memory: " + oome.getMessage());
                } finally {
                    if (bPhoto != null) {
                        bPhoto.recycle();
                        bPhoto = null;
                    }
                }
            }
        }
    };

    private File getPhotoPath() {
        File dir = appPhotoDirectory;
        dir.mkdirs();

        return (new File(dir, StorageUtility.getNewFileName()));
    }

    private void gotoEditPhotoCapture(String photoPath) {

        if (photoPath == null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Error")
                    .setMessage("Something went wrong while taking this photo. Try taking a picture with your camera app and uploading it.")
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();

            // Add the touch listener back to the mFrameLayout again
            setupListeners();

            return;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("quality", mResolutionMode);
        logIntercomEvent("took_photo", attributes);

        File file = new File(photoPath);

        doPhotoTaken(file);
        finishWithResult("capture");
    }

    private int greatestCommonFactor(int width, int height) {
        return (height == 0) ? width : greatestCommonFactor(height, width % height);
    }

    // This method sets the visibility of the flash button
    public void setFlashButtonVisibility() {

        // Hide the flash button if the selected camera doesn't support flash
        if (hasFlash()) {
            mToggleFlash.setVisibility(View.VISIBLE);
        } else {
            mToggleFlash.setVisibility(View.INVISIBLE);
        }
    }

    private void setFlashModeImage(String flashMode) {
        int imageRes;
        if (flashMode.equals("auto")) {
            imageRes = R.drawable.flashlight_off;
        } else if (flashMode.equals("on")) {
            imageRes = R.drawable.flashlight_on;
        } else if (flashMode.equals("torch")) {
            imageRes = R.drawable.flashlight_on;
        } else {
            imageRes = R.drawable.flashlight_off;
        }

        mToggleFlash.setImageResource(imageRes);

        // Persist flash mode
        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_FLASH_MODE, flashMode);
        editor.apply();
    }

    // This method briefly flashes the screen as a visual indicator that a photo was captured
    private void animateScreenFlash() {

        // Set the mScreenFlashView's alpha to 1.0 and show it
        mScreenFlashView.setAlpha(1.0f);
        mScreenFlashView.setVisibility(View.VISIBLE);

        // Set a duration in milliseconds for each segment of the animation
        final int duration = 60;

        // Animate the mScreenFlashView's alpha to 0.0
        System.out.println("starting screen flash animations");
        mScreenFlashView.animate()
                .setDuration(duration)
                .alpha(0.0f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("first screen flash animation finished");

                        // Hide the mScreenFlashView again
                        mScreenFlashView.setVisibility(View.INVISIBLE);
                    }
                })
                .start();

    }

    // This method sets the visibility of the mToggleCamera button
    private void setCameraButtonVisibility() {

        // Check if this device contains both a rear- and front-facing camera that have a hardware support level greater than LEGACY
        boolean foundRearCamera = false;
        boolean foundFrontCamera = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                foundRearCamera = true;
            }
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                foundFrontCamera = true;
            }
        }

        // Show the camera button only if the device has both a rear- and front-facing camera
        if (foundRearCamera && foundFrontCamera) {
            mToggleCamera.setVisibility(View.VISIBLE);
        }
        else {
            mToggleCamera.setVisibility(View.INVISIBLE);
        }
    }

    // This method switches between rear- and front-facing cameras
    protected void toggleCameraTapped() {

        if (mCameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else {
            mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        // Reset the current zoom level
        //mCurrentZoomLevel = 1.0;

        // Close the current camera
        releaseCamera();

        // Initialize the camera again
        //mPreviewLayout.removeView(mPreview);
        startPreview();
    }

    private void setResolutionImage(String resolutionMode) {

        // Determine whether the mToggleResolution button or the mCloseButton is currently controlling the resolution selection based on the
        // device orientation
        ImageButton resolutionButton = mToggleResolution;
        if (mPhonePosition == LANDSCAPE_TOP_LEFT) {
            resolutionButton = mCloseButton;
        }

        if (resolutionMode.equals("super")) {
            // Set the button images
            resolutionButton.setImageResource(R.drawable.superfine_size_icon);
            mNormalButton.setImageResource(R.drawable.normal_icon);
            mHighButton.setImageResource(R.drawable.high_icon);
            mSuperButton.setImageResource(R.drawable.super_fine_on_icon);
            mNormalButtonLand.setImageResource(R.drawable.normal_icon);
            mHighButtonLand.setImageResource(R.drawable.high_icon);
            mSuperButtonLand.setImageResource(R.drawable.super_fine_on_icon);

            // Show the appropriate label layout for landscape orientation
            mResolutionLabelLayoutNormal.setVisibility(View.INVISIBLE);
            mResolutionLabelLayoutHigh.setVisibility(View.INVISIBLE);
            mResolutionLabelLayoutSuper.setVisibility(View.VISIBLE);

            // Set the resolution text labels for portrait orientation
            mResolutionLabel1.setText("Best for capturing great details.");
            mResolutionLabel2.setText("Largest file size.  Uses the most data.");

            mResolutionMode = "super";
        }
        else if (resolutionMode.equals("high")) {
            // Set the button images
            resolutionButton.setImageResource(R.drawable.high_size_icon);
            mNormalButton.setImageResource(R.drawable.normal_icon);
            mHighButton.setImageResource(R.drawable.high_on_icon);
            mSuperButton.setImageResource(R.drawable.super_fine_icon);
            mNormalButtonLand.setImageResource(R.drawable.normal_icon);
            mHighButtonLand.setImageResource(R.drawable.high_on_icon);
            mSuperButtonLand.setImageResource(R.drawable.super_fine_icon);

            // Show the appropriate label layout for landscape orientation
            mResolutionLabelLayoutNormal.setVisibility(View.INVISIBLE);
            mResolutionLabelLayoutHigh.setVisibility(View.VISIBLE);
            mResolutionLabelLayoutSuper.setVisibility(View.INVISIBLE);

            // Set the resolution text labels for portrait orientation
            mResolutionLabel1.setText("Best for balancing image quality and file size.");
            mResolutionLabel2.setText("Uses more data.");

            mResolutionMode = "high";
        }
        else {
            // Set the button images
            resolutionButton.setImageResource(R.drawable.normal_size_icon);
            mNormalButton.setImageResource(R.drawable.normal_on_icon);
            mHighButton.setImageResource(R.drawable.high_icon);
            mSuperButton.setImageResource(R.drawable.super_fine_icon);
            mNormalButtonLand.setImageResource(R.drawable.normal_on_icon);
            mHighButtonLand.setImageResource(R.drawable.high_icon);
            mSuperButtonLand.setImageResource(R.drawable.super_fine_icon);

            // Show the appropriate label layout for landscape orientation
            mResolutionLabelLayoutNormal.setVisibility(View.VISIBLE);
            mResolutionLabelLayoutHigh.setVisibility(View.INVISIBLE);
            mResolutionLabelLayoutSuper.setVisibility(View.INVISIBLE);

            // Set the resolution text labels for portrait orientation
            mResolutionLabel1.setText("Best for everyday use.");
            mResolutionLabel2.setText("Smallest file size.  Uses the least data.");

            mResolutionMode = "normal";
        }

        // Persist resolution mode
        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_RESOLUTION_MODE, mResolutionMode);
        editor.apply();
    }

    private void updateFlashSetting(String flashMode) {

        if (mCamera != null) {
            Camera.Parameters p = mCamera.getParameters();

            // Make sure that setting the flash setting is supported or setting the camera parameters will fail
            if (p.getFlashMode() != null) {
                p.setFlashMode(flashMode);
                mCamera.setParameters(p);
            }
        }
    }

    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
        mCamera.setParameters(params);
    }

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

    /** Determine the space between the first two fingers */
    private double getFingerSpacing(MotionEvent event) {

        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }


    private void setResolution(String resolution) {

        if (mCamera != null) {

            Camera.Parameters param = mCamera.getParameters();
            List<Camera.Size> lsps = param.getSupportedPictureSizes();

            if (resolution.equals("super")) {
                for (Camera.Size size : lsps) {
                    if (size.width < 2160) {
                        break;
                    }
                    param.setPictureSize(size.width, size.height);
                }
                setResolutionImage("super");
                LogUtil.e(TAG, "IN SUPER HIGH RES MODE");
            }
            else if (resolution.equals("high")) {
                for (Camera.Size size : lsps) {
                    if (size.width < 1920) {
                        break;
                    }
                    param.setPictureSize(size.width, size.height);
                }
                setResolutionImage("high");
                LogUtil.e(TAG, "IN HIGH RES MODE");
            }
            else {
                for (Camera.Size size : lsps) {
                    if (size.width == 1280 && size.height == 720) {
                        param.setPictureSize(size.width, size.height);
                    }
                }
                setResolutionImage("normal");
                LogUtil.e(TAG, "IN LOW RES MODE");
            }

            param.setJpegQuality(100);
            mCamera.setParameters(param);

            LogUtil.e(TAG, "Width is " + mCamera.getParameters().getPictureSize().width + " height is " + mCamera.getParameters().getPictureSize().height);
        }
    }

    // This method sets the camera mode and updates the camera mode labels accordingly
    private void setCameraMode(String cameraMode) {

        // FastCam mode
        if (cameraMode.equals("fastcam")) {
            mCaptureButton.setImageResource(R.drawable.fast_cam_icon);
            mFastCamIndicator.setVisibility(View.VISIBLE);
            mFastCamLabel.setTextColor(getResources().getColor(R.color.sun_yellow));
            mFastCamLabel.setAlpha(1.0f);
            mCameraIndicator.setVisibility(View.INVISIBLE);
            mCameraLabel.setTextColor(Color.WHITE);
            mCameraLabel.setAlpha(0.6f);
            mScannerIndicator.setVisibility(View.INVISIBLE);
            mScannerLabel.setTextColor(Color.WHITE);
            mScannerLabel.setAlpha(0.6f);
        }

        // Camera mode
        else if (cameraMode.equals("camera")) {
            mCaptureButton.setImageResource(R.drawable.snap_icon);
            mFastCamIndicator.setVisibility(View.INVISIBLE);
            mFastCamLabel.setTextColor(Color.WHITE);
            mFastCamLabel.setAlpha(0.6f);
            mCameraIndicator.setVisibility(View.VISIBLE);
            mCameraLabel.setTextColor(getResources().getColor(R.color.sun_yellow));
            mCameraLabel.setAlpha(1.0f);
            mScannerIndicator.setVisibility(View.INVISIBLE);
            mScannerLabel.setTextColor(Color.WHITE);
            mScannerLabel.setAlpha(0.6f);
        }

        // Scanner mode
        else if (cameraMode.equals("scanner")) {
            mCaptureButton.setImageResource(R.drawable.snap_icon);
            mFastCamIndicator.setVisibility(View.INVISIBLE);
            mFastCamLabel.setTextColor(Color.WHITE);
            mFastCamLabel.setAlpha(0.6f);
            mCameraIndicator.setVisibility(View.INVISIBLE);
            mCameraLabel.setTextColor(Color.WHITE);
            mCameraLabel.setAlpha(0.6f);
            mScannerIndicator.setVisibility(View.VISIBLE);
            mScannerLabel.setTextColor(getResources().getColor(R.color.sun_yellow));
            mScannerLabel.setAlpha(1.0f);
        }

        // Save the camera mode
        mCameraMode = cameraMode;
        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_CAMERA_MODE, cameraMode);
        editor.apply();
    }

    private void setupListeners() {

        if (mCamera != null) {

            mPreviewLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {

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
            });
            mCaptureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCamera != null) {
                        try {
                            mCamera.takePicture(shutterCallback, null, mPicture);

                            // Remove the touch listener from the mFrameLayout after a picture has been taken
                            mPreviewLayout.setOnTouchListener(null);
                        } catch (RuntimeException re) {
                            Log.e(TAG, "RuntimeEx takePicture" + re.getMessage());
                        }
                    } else {
                        //TODO Add snackbar
                        //http://www.androidhive.info/2015/09/android-material-design-snackbar-example/
                    }
                }
            });
        }

        // Set a listener for the fastcam layout
        mFastCamLayout.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setCameraMode("fastcam");
            }
        });

        // Set a listener for the camera layout
        mCameraLayout.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setCameraMode("camera");
            }
        });

        // Set a listener for the scanner layout
        mScannerLayout.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setCameraMode("scanner");
            }
        });

        // Set a listener for the mNormalButton
        mNormalButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("normal");
            }
        });

        // Set a listener for the mHighButton
        mHighButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("high");
            }
        });

        // Set a listener for the mSuperButton
        mSuperButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("super");
            }
        });

        // Set a listener for the mNormalButtonLand
        mNormalButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("normal");
            }
        });

        // Set a listener for the mHighButtonLand
        mHighButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("high");
            }
        });

        // Set a listener for the mSuperButtonLand
        mSuperButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                setResolution("super");
            }
        });

        // Set a listener for the mResolutionDismissButton
        mResolutionDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideResolutionLayout();
            }
        });

        // Set a listener for the mResolutionDismissButtonLand
        mResolutionDismissButtonLand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideResolutionLayout();
            }
        });

        mToggleResolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleResolution();
            }
        });

        mToggleFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashTapped();
            }
        });

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeButtonClick();
            }
        });

        mLabelTouchTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelTouch();
            }
        });

        mToggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCameraTapped();
            }
        });
    }

    private boolean checkCameraPermissions() {
        for (String permission : CAMERA_PERMISSIONS) {
            int result = ContextCompat.checkSelfPermission(getContext(), permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // This method uploads photos taken while in FastCam mode
    private void uploadFastCamPhoto(File photo) {

        // If saveToPhone is set, then save the image to the device in addition to sending it to the server.
        SharedPreferences preferences = getContext().getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        boolean saveToPhone = preferences.getBoolean(PhotoActions.PREF_KEY_SAVE_TO_PHONE, false);
        if (saveToPhone) {

            // Try writing the image to the device. This method will return null if the image can't be saved successfully.
            String imageURL = PhotoActions.writeImageToDevice(getContext(), Uri.fromFile(photo));
        }

        new ProcessPhotoAsyncTask(photo).execute();
    }

    // This class uploads photos on a background thread
    class ProcessPhotoAsyncTask extends AsyncTask<Object, Void, File> {

        private File mFile;

        public ProcessPhotoAsyncTask(File file) {
            mFile = file;
        }

        @Override
        protected File doInBackground(Object... params) {
            return ImageEditorUtility.processImageWithEdit(mFile, 0, null);
        }

        @Override
        protected void onPostExecute(File editedPhoto) {
            super.onPostExecute(editedPhoto);

            doPhotoAccepted(mFile);
        }
    }

    // —————————————————————————————————————————————————————————————————————————————————————————————
    // Surface Holder methods
    // —————————————————————————————————————————————————————————————————————————————————————————————
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated");

        // Set the SurfaceCreated flag
        mPreview.mSurfaceCreated = true;

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
            else {

                // Create an instance of Camera
                mCamera = getCameraInstance();

                if (mCamera != null) {

                    setCameraDisplayOrientation(0, mCamera);

                    // Set the visibility of the flash button
                    setFlashButtonVisibility();

                    setFlashModeImage(mFlashMode);
                    updateFlashSetting(mFlashMode);
                    setResolution(mResolutionMode);

                    //Set up Listeners
                    setupListeners();
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.d(TAG, "surfaceDestroyed");

        // Set the mSurfaceCreated flag
        mPreview.mSurfaceCreated = false;

        // Release the camera
        releaseCamera();
    }

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