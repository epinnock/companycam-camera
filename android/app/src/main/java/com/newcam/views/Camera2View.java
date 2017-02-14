package com.newcam.views;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.newcam.CCCameraView;
import com.newcam.R;
import com.newcam.utils.ExifUtils;
import com.newcam.utils.PhotoUtils;
import com.notagilx.companycam.react_bridges.PhotoActions;
import com.notagilx.companycam.util.LogUtil;
import com.notagilx.companycam.util.SingleClickListener;
import com.notagilx.companycam.util.StorageUtility;
import com.notagilx.companycam.util.views.CameraPreview;
import com.notagilx.companycam.util.views.FocusIndicatorView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.Context.CAMERA_SERVICE;

@TargetApi(21)
public class Camera2View extends CCCameraView implements SurfaceHolder.Callback {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Much of the camera-related content of this Activity is taken from the Google example project Camera2Basic on github.
    // https://github.com/googlesamples/android-Camera2Basic
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String TAG = Camera2View.class.getSimpleName();

    // These strings are used to save and retrieve persistent settings to the SharedPreferences
    private static final String PREFS_FLASH_MODE = "PREFS_FLASH_MODE";
    private static final String PREFS_RESOLUTION_MODE = "PREFS_RESOLUTION_MODE";
    private static final String PREFS_CAMERA_MODE = "PREFS_CAMERA_MODE";

    // The ORIENTATIONS array is used to assign numerical values to the Surface.ROTATION constants when handling the device and camera sensor
    // orientations.
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static final int PORTRAIT_TOP_UP = 1;
    public static final int PORTRAIT_TOP_DOWN = 2;
    public static final int LANDSCAPE_TOP_LEFT = 3;
    public static final int LANDSCAPE_TOP_RIGHT = 4;

    // The following constants are used to track the state of the camera

    //Camera state: Showing camera preview.
    private static final int STATE_PREVIEW = 0;

    // Camera state: Waiting for the focus to be locked.
    private static final int STATE_WAITING_LOCK = 1;

    // Camera state: Waiting for the exposure to be precapture state.
    private static final int STATE_WAITING_PRECAPTURE = 2;

    // Camera state: Waiting for the exposure state to be something other than precapture.
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    // Camera state: Picture was taken.
    private static final int STATE_PICTURE_TAKEN = 4;

    // mState is the current state of camera state for taking pictures.
    private int mState = STATE_PREVIEW;

    // Define the maximum preview width and height guaranteed by the Camera2 API.
    //private static final int MAX_PREVIEW_WIDTH = 1920;
    //private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final int MAX_PREVIEW_WIDTH = 4000;
    private static final int MAX_PREVIEW_HEIGHT = 3000;

    // The mStateCallback handles the program flow when the camera state changes.
    private CameraDevice.StateCallback mStateCallback;

    // The mCaptureCallback handles the program flow during a photo capture.
    private CameraCaptureSession.CaptureCallback mCaptureCallback;

    // The mPreviewRequestBuilder is used to create the camera preview.
    private CaptureRequest.Builder mPreviewRequestBuilder;

    // The mPreviewRequest is generated by the mPreviewRequestBuilder and used to create the camera preview.
    private CaptureRequest mPreviewRequest;

    // The mCaptureSession is used to display the camera preview and to capture the photo.
    private CameraCaptureSession mCaptureSession;

    // The mCameraOpenCloseLock Semaphore is used to prevent the app from exiting before closing the camera.
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    // mCamera is a reference to the camera currently being used
    private CameraDevice mCamera;
    private String mCameraId;

    // mCameraType is a reference to the camera type (rear- or forward-facing) currently being used
    private int mCameraType = CameraCharacteristics.LENS_FACING_BACK;

    private CameraPreview mPreview;
    private Surface mJpegCaptureSurface, mPreviewSurface;
    private Size mPreviewSize;
    private CameraCharacteristics mCharacteristics;

    // The mOrientationListener is used to properly orient the buttons on the screen as the device orientation changes since the Activity
    // orientation is fixed in landscape.
    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    private RelativeLayout mPreviewLayout;

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

    // The mTopLayout contains the place label, close button, and resolution button
    private LinearLayout mTopLayout;

    // The mBottomLayout contains the camera buttons and camera mode labels
    private LinearLayout mBottomLayout;

    protected LinearLayout mLabelTouchTarget;
    protected LinearLayout mLabelTouchTargetLand;
    protected ImageButton mCloseButton;
    protected ImageButton mToggleResolution;
    protected ImageButton mToggleFlash;

    // The mToggleCamera button allows the user to switch between rear- and forward-facing cameras
    protected ImageButton mToggleCamera;

    // The mCaptureButton allows the user to capture a photo
    private ImageButton mCaptureButton;

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

    // The mTabletButtonView is the layout that holds all the buttons for tablets
    private TabletButtonView mTabletButtonView;
    private TabletButtonView mTabletButtonViewLand;

    private int mPhonePosition;

    private final int HIGH_QUALITY = 80;

    // This is the animation distance for the resolution layout in dp
    private final int RESOLUTION_ANIMATION_DIST_DP = 150;

    // The mResolutionLayoutVisible flag indicates whether or not the resolution layout is currently visible
    private boolean mResolutionLayoutVisible = false;

    ImageReader mJPEGReader;

    // The mCurrentZoomLevel and mCurrentFingerSpacing are used to handle pinch/zoom gestures
    private double mCurrentZoomLevel = 1.0;
    private double mStartingZoomLevel = 1.0;
    private double mStartingFingerSpacing = -1.0;
    private boolean mMultiTouchDetected = false;

    // The mFocusIndicatorView is used to indicate that the camera is in the process of focusing
    public FocusIndicatorView mFocusIndicatorView;

    // The mManualAutoFocus flag indicates whether or not the user initialized tap-to-autofocus
    private boolean mManualAutoFocus = false;

    // The mMeteringRect is the metering rectangle to be used for auto focus and auto exposure based on the user's touch point
    private MeteringRectangle[] mMeteringRect = {new MeteringRectangle(0,0,0,0,0)};

    // The mScreenFlashView is used to provide a flash effect when the user snaps a photo
    private FrameLayout mScreenFlashView;

    // The CLICK_REJECTION_INTERVAL is an amount of time in milliseconds that must elapse before a button click will be processed.
    // This is used to reject multiple clicks in quick succession.
    private static int CLICK_REJECTION_INTERVAL = 1500;

    // The mBackgroundThread is an additional thread for running tasks that shouldn't block the UI.
    private HandlerThread mBackgroundThread;

    // The mBackgroundHandler handles tasks running in the background.
    private Handler mBackgroundHandler;

    // The mCameraClosedCallback is a string that describes whether or not a specific callback should execute after the camera is successfully closed
    private String mCameraClosedCallback = "none";


    public Camera2View(Context context) { super(context); }

    @Override
    public void init(Context context) {

        // Set the default camera type
        mCameraType = CameraCharacteristics.LENS_FACING_BACK;

        // Get references to the subviews
        mPreviewLayout = (RelativeLayout) findViewById(R.id.camera_preview);
        mPlaceName = (TextView) findViewById(R.id.place_name);
        mPlaceAddress = (TextView) findViewById(R.id.place_address);
        mLabelTouchTarget = (LinearLayout) findViewById(R.id.label_touch_target);
        mLabelTouchTargetLand = (LinearLayout) findViewById(R.id.label_touch_target_land);
        mToggleResolution = (ImageButton) findViewById(R.id.toggle_resolution);
        mToggleFlash = (ImageButton) findViewById(R.id.toggle_flash);
        mCloseButton = (ImageButton) findViewById(R.id.close_button);
        mToggleCamera = (ImageButton) findViewById(R.id.toggle_camera);
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
        mFocusIndicatorView = (FocusIndicatorView) findViewById(R.id.focus_indicator_view);
        mScreenFlashView = (FrameLayout) findViewById(R.id.screen_flash_view);

        // Initialize the tablet button view if the device is using the tablet layout
        LinearLayout tabletButtonLayout = (LinearLayout) findViewById(R.id.tablet_button_view);
        LinearLayout tableButtonLayoutLand = (LinearLayout) findViewById(R.id.tablet_button_view_land);
        if (tabletButtonLayout != null) {

            // Create two TabletButtonViews
            LinearLayout.LayoutParams tabletParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mTabletButtonView = new TabletButtonView(context);
            tabletButtonLayout.addView(mTabletButtonView, tabletParams);
            mTabletButtonViewLand = new TabletButtonView(context);
            tableButtonLayoutLand.addView(mTabletButtonViewLand, tabletParams);

            // Set the layout resources for the two tablet button views
            mTabletButtonView.layoutResourceID = R.layout.view_tablet_button;
            mTabletButtonViewLand.layoutResourceID = R.layout.view_tablet_button_land;

            // Initialize the tablet button views
            mTabletButtonView.initView(context);
            mTabletButtonViewLand.initView(context);

            // Set the useTableLayout flag
            useTabletLayout = true;

            // Add the click listener to the mLabelTouchTarget and mLabelTouchTargetLand
            mLabelTouchTarget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    labelTouch();
                }
            });
            mLabelTouchTarget.setClickable(true);
            mLabelTouchTargetLand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    labelTouch();
                }
            });
            mLabelTouchTargetLand.setClickable(true);

            // Show the mTableButtonView by default
            showTabletButtonView();
        }
        else {

            // Set the gradient backgrounds for the layouts
            mTopLayout.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.transparent_gray_gradient_270));
            mBottomLayout.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.transparent_gray_gradient_90));

            // Add the click listener to the mLabelTouchTarget
            mLabelTouchTarget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    labelTouch();
                }
            });
            mLabelTouchTarget.setClickable(true);
        }

        // Set the place name label--will be updated when props are received
        mPlaceName.setText("Location");

        // Set the button orientations for the resolution layout
        setupResolutionLayout();

        // Initialize the mStateCallback for the camera
        mStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {

                System.out.println("mStateCallback onOpened called");

                // This method is called when the camera is opened.  We start camera preview here.
                mCameraOpenCloseLock.release();
                mCamera = cameraDevice;

                // Initialize the camera preview
                initPreview();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {

                System.out.println("mStateCallback onDisconnected called");

                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCamera = null;
            }

            @Override
            public void onClosed(@NonNull CameraDevice cameraDevice) {

                System.out.println("mStateCallback onClosed called");

                mCameraOpenCloseLock.release();

                // If there's a callback to execute after the camera is closed, then execute it and reset the mCameraClosedCallback string
                if(mCameraClosedCallback != null){ 
                  if (mCameraClosedCallback.equals("openCamera")) {
                      mCameraClosedCallback = "";
                      openCamera(mPreview.getHolder());
                  }
                }
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {

                String errorMsg = "Unknown camera error";
                switch (error) {
                    case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                        errorMsg = "Camera is already in use.";
                        break;
                    case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                        errorMsg = "Max number of cameras are open, close previous cameras first.";
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                        errorMsg = "Camera is disabled, e.g. due to device policies.";
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                        errorMsg = "Camera device has encountered a fatal error, please try again.";
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                        errorMsg = "Camera service has encountered a fatal error, please try again.";
                        break;
                }
                System.out.println("mStateCallback onError called (" + errorMsg + ")");

                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCamera = null;

                finishWithError(errorMsg);
            }
        };

        // Initialize the mCaptureCallback
        mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

            // The lock states are used to track the real states of the AF and AE routines and ignore transient null or invalid states
            private int numConsecutiveAFLockStates = 0;
            private int numConsecutiveAFInactiveStates = 0;
            private int numConsecutiveAFScanStates = 0;

            // Check the current camera state whenever the capture has progressed or completed to determine the correct behavior
            private void process(CaptureResult result) {
                switch (mState) {
                    case STATE_PREVIEW: {

                        // Simply make sure the that AF and AE state tracking variables are set to 0 before any auto focus event starts
                        numConsecutiveAFLockStates = 0;
                        numConsecutiveAFInactiveStates = 0;
                        numConsecutiveAFScanStates = 0;

                        break;
                    }
                    case STATE_WAITING_LOCK: {

                        System.out.println("mCaptureCallback: STATE_WAITING_LOCK");

                        // Check the auto focus state and auto exposure state
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                        if (afState != null) {
                            System.out.println("auto focus state was " + getAFStateString(afState) + " in STATE_WAITING_LOCK");
                        }
                        if (aeState != null) {
                            System.out.println("auto exposure state was " + getAEStateString(aeState) + " in STATE_WAITING_LOCK");
                        }

                        // If the auto focus state is null or INACTIVE, then the auto focus completion handler can be called.
                        if (afState == null || afState == 0) {

                            if (afState == null) {
                                System.out.println("auto focus state was null in STATE_WAITING_LOCK");
                            }
                            else if (afState == 0) {
                                System.out.println("auto focus state was zero in STATE_WAITING_LOCK");
                            }

                            // Reset the numConsecutiveAFLockStates and numConsecutiveAFScanState since the auto focus isn't locked or scanning
                            numConsecutiveAFLockStates = 0;
                            numConsecutiveAFScanStates = 0;

                            // Increment the numConsecutiveAFInactiveStates
                            numConsecutiveAFInactiveStates++;

                            // If there have been at least three consecutive callbacks where the auto focus state is null or INACTIVE, then the
                            // onAutoFocusComplete function can be called.  If there haven't been at least three consecutive callbacks, then this
                            // auto focus state may be transient.
                            if (numConsecutiveAFInactiveStates >= 3) {
                                onAutoFocusComplete();
                                return;
                            }
                        }

                        // If the auto focus state is in a scan mode, then monitor the number of consecutive callbacks.  Some devices seems to
                        // get stuck in scan modes and never finish
                        else if (CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN == afState || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {

                            // Reset the numConsecutiveAFLockState and numConsecutiveAFInactiveState since the auto focus is scanning
                            numConsecutiveAFLockStates = 0;
                            numConsecutiveAFInactiveStates = 0;

                            // Increment the numConsecutiveAFScanStates
                            numConsecutiveAFScanStates++;

                            // If there have been at least 30 consecutive scan states, then the device may not ever find focus, so call the
                            // onAutoFocusComplete function
                            if (numConsecutiveAFScanStates >= 30) {
                                onAutoFocusComplete();
                                return;
                            }

                        }

                        // If the auto focus is locked, then the photo can be captured if the auto exposure has finished
                        else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {

                            // Reset the numConsecutiveAFInactiveStates since the auto focus is locked
                            numConsecutiveAFInactiveStates = 0;
                            numConsecutiveAFScanStates = 0;

                            // Increment the numConsecutiveAFLockStates
                            numConsecutiveAFLockStates++;

                            // If there have been at least three consecutive callbacks where the auto focus is locked, then the onAutoFocusComplete
                            // function can be called for normal Camera mode.  If there haven't been at least three consecutive callbacks, then this
                            // auto focus state may be transient.
                            if (numConsecutiveAFLockStates >= 3 && !mCameraMode.equals("fastcam") && !mFlashMode.equals("auto")) {
                                onAutoFocusComplete();
                                return;
                            }

                            // Check the CONTROL_AE_STATE to see if the auto exposure has finished.  It can be null on some devices.
                            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {

                                System.out.println("auto focus state was locked and ae state was converged in STATE_WAITING_LOCK");

                                // If there have been at least three consecutive callbacks where the auto focus state is locked, then the
                                // onAutoFocusComplete function can be called.  If there haven't been at least three consecutive callbacks, then this
                                // auto focus state may be transient.
                                if (numConsecutiveAFLockStates >= 3) {
                                    onAutoFocusComplete();
                                    return;
                                }
                            }

                            // If the auto exposure hasn't started, then run the precapture sequence.
                            else {
                                runPrecaptureSequence();
                            }
                        }
                        else {

                            // If the auto focus and auto exposure fall into any other category, reset all the lock states
                            numConsecutiveAFLockStates = 0;
                            numConsecutiveAFInactiveStates = 0;
                            numConsecutiveAFScanStates = 0;
                        }
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {

                        System.out.println("mCaptureCallback: STATE_WAITING_PRECAPTURE");

                        // Check the CONTROL_AE_STATE to see if the auto exposure has finished.  It can be null on some devices.
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {

                            // Update the camera state to reflect that the precapture sequence is complete
                            mState = STATE_WAITING_NON_PRECAPTURE;
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {

                        System.out.println("mCaptureCallback: STATE_WAITING_NON_PRECAPTURE");

                        // Check the CONTROL_AE_STATE to see if the auto exposure has finished.  It can be null on some devices.
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {

                            // Call onAutoFocusComplete to capture the still photo
                            onAutoFocusComplete();
                        }
                        break;
                    }
                    default:
                        break;
                }
            }

            // This method either initiates the still picture capture or unlocks the focus after autofocus is complete based on whether the
            // autofocus was part of a photo capture sequence or a result of tap-to-autofocus.
            private void onAutoFocusComplete() {

                // After auto focus is complete, reset the state counters
                numConsecutiveAFLockStates = 0;
                numConsecutiveAFInactiveStates = 0;

                if (mManualAutoFocus) {

                    // If this was a tap-to-autofocus event, then reset the mManualAutoFocus and unlock the focus again
                    mManualAutoFocus = false;
                    unlockFocus();
                }
                else {

                    // If this wasn't a tap-to-autofocus event, then capture the photo
                    mState = STATE_PICTURE_TAKEN;
                    captureStillPicture();
                }
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureResult partialResult) {
                //System.out.println("onCaptureProgressed for tag = " + request.getTag());

                //process(partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {

                //System.out.println("onCaptureCompleted for tag = " + request.getTag());

                /*Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                if (afState != null) {
                    System.out.println("auto focus state was " + getAFStateString(afState) + " in onCaptureCompleted");
                }
                if (aeState != null) {
                    System.out.println("auto exposure state was " + getAEStateString(aeState) + " in onCaptureCompleted");
                }*/

                process(result);
            }

        };

        //old onResume
        //-------------------------------------
        startBackgroundThread();

        // Setup the custom orientation change listener
        initOrientationListener();

        // Get the saved settings from the SharedPreferences.  Restrict the possible flash modes to "torch" and "off".
        SharedPreferences preferences = getSharedPreferences();
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

        // Create the camera preview
        System.err.println("[DEBUG] createPreview being called - Constructor"); //TODO
        createPreview();

        // Setup all the button listeners
        setupListeners();

        // Set the visibility of the flash button
        setFlashButtonVisibility();

        // Set the visibility of the camera button
        setCameraButtonVisibility();
        //-------------------------------------
    }

    //TODO
    /*

    @Override
    protected void onPause() {
        //mEventBus.unregister(this);

        // Camera is released once preview is destroyed
        if (mPreview != null && mPreviewLayout != null) {
            mPreviewLayout.removeView(mPreview);
            mPreviewLayout = null;
        }

        // Close the camera and stop the background thread
        closeCamera();
        stopBackgroundThread();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
    }
    */

    @Override
    public void startCamera() {
        createPreview();
    }

    @Override
    public void releaseCamera() {
        mCameraClosedCallback = "";
        closeCamera();
    }

    // This method either shows the resolution layout or dismisses the Activity depending on the current orientation of the device
    protected void toggleResolution() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // The action for the close button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape in the phone layout
        if (mPhonePosition == PORTRAIT_TOP_UP || useTabletLayout) {

            // This is the resolution button action
            showResolutionLayout();
        }
        else {

            // This is the close button action
            // Finish the activity
            finishWithResult("close");
        }
    }

    // This method either shows the resolution layout or dismisses the Activity depending on the current orientation of the device
    protected void closeButtonClick() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // The action for the close button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape in the phone layout
        if (mPhonePosition == PORTRAIT_TOP_UP || useTabletLayout) {

            // This is the close button action
            // Finish the activity
            finishWithResult("close");
        }
        else {

            // This is the resolution button action
            showResolutionLayout();
        }
    }

    // This method initializes the orientation listener so that the buttons can be set to the proper orientation
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

                                // Show the mTabletButtonView for tablets
                                if (useTabletLayout) {
                                    showTabletButtonView();
                                }
                                else {

                                    // Set the icons for the mToggleResolution and mCloseButton
                                    setResolutionImage(mResolutionMode);
                                    mCloseButton.setImageResource(R.drawable.close_icon);
                                }
                            }
                            else if ((orientation < 315 && orientation >= 225) && !(mLastOrientation < 315 && mLastOrientation >= 225)) {
                                rotationValue = 90;

                                // Hide the resolution layout if it's showing
                                if (mResolutionLayoutVisible) {
                                    hideResolutionLayout();
                                }

                                mPhonePosition = LANDSCAPE_TOP_LEFT;

                                // Show the mTabletButtonViewLand for tablets
                                if (useTabletLayout) {
                                    showTabletButtonViewLand();
                                }
                                else {

                                    // Set the icons for the mToggleResolution and mCloseButton
                                    mToggleResolution.setImageResource(R.drawable.close_icon);
                                    setResolutionImage(mResolutionMode);
                                }
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

            if (useTabletLayout) {

                // Animate the opacity of the mLabelTouchTarget, mCloseButton, and mToggleResolution
                mLabelTouchTarget.animate().alpha(0.0f).setDuration(300).start();
                mCloseButton.animate().alpha(0.0f).setDuration(300).start();
                mToggleResolution.animate().alpha(0.0f).setDuration(300).start();
            }
            else {

                // Animate the opacity of the top layout
                mTopLayout.animate().alpha(0.0f).setDuration(300).start();
            }
        }
        else {

            // Set the opacity of the landscape resolution layout to 0 before starting the animation
            mResolutionLayoutLand.setAlpha(0.0f);

            // Convert the RESOLUTION_ANIMATION_DIST_DP to pixels
            float animationDistPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RESOLUTION_ANIMATION_DIST_DP, getResources().getDisplayMetrics());
            float screenWidthPx = this.getWidth();

            // Animate the position and opacity of the landscape resolution layout
            mResolutionLayoutLand.animate().x(screenWidthPx - animationDistPx).alpha(1.0f).setDuration(300).start();

            if (useTabletLayout) {

                // Animate the opacity of the mLabelTouchTargetLand, mCloseButton, and mToggleResolution
                mLabelTouchTargetLand.animate().alpha(0.0f).setDuration(300).start();
                mCloseButton.animate().alpha(0.0f).setDuration(300).start();
                mToggleResolution.animate().alpha(0.0f).setDuration(300).start();
            }
            else {

                // Animate the opacity of the top and bottom layouts
                mTopLayout.animate().alpha(0.0f).setDuration(300).start();
                mBottomLayout.animate().alpha(0.0f).setDuration(300).start();
            }
        }

        // Remove the click listener for the mLabelTouchTarget and mLabelTouchTargetLand layouts while the resolution layout is visible
        mLabelTouchTarget.setOnClickListener(null);
        mLabelTouchTarget.setClickable(false);
        if (mLabelTouchTargetLand != null) {
            mLabelTouchTargetLand.setOnClickListener(null);
            mLabelTouchTargetLand.setClickable(false);
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

            if (useTabletLayout) {

                // Animate the opacity of the mLabelTouchTarget, mCloseButton, and mToggleResolution
                mLabelTouchTarget.animate().alpha(1.0f).setDuration(300).start();
                mCloseButton.animate().alpha(1.0f).setDuration(300).start();
                mToggleResolution.animate().alpha(1.0f).setDuration(300).start();
            }
            else {

                // Animate the opacity of the top layout
                mTopLayout.animate().alpha(1.0f).setDuration(300).start();
            }
        }
        else {

            // Animate the position and opacity of the landscape resolution layout
            mResolutionLayoutLand.animate().x(this.getWidth()).alpha(0.0f).setDuration(300).start();

            if (useTabletLayout) {

                // Animate the opacity of the mLabelTouchTargetLand, mCloseButton, and mToggleResolution
                mLabelTouchTargetLand.animate().alpha(1.0f).setDuration(300).start();
                mCloseButton.animate().alpha(1.0f).setDuration(300).start();
                mToggleResolution.animate().alpha(1.0f).setDuration(300).start();
            }
            else {

                // Animate the opacity of the top and bottom layouts
                mTopLayout.animate().alpha(1.0f).setDuration(300).start();
                mBottomLayout.animate().alpha(1.0f).setDuration(300).start();
            }
        }

        // Add the click listener back to the mLabelTouchTarget and mLabelTouchTargetLand layouts while the resolution layout is hidden
        mLabelTouchTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelTouch();
            }
        });
        mLabelTouchTarget.setClickable(true);
        if (mLabelTouchTargetLand != null) {
            mLabelTouchTargetLand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    labelTouch();
                }
            });
            mLabelTouchTargetLand.setClickable(true);
        }

        // Set the mResolutionLayoutVisible flag
        mResolutionLayoutVisible = false;
    }

    // This method shows the mTabletButtonView and hides the mTabletButtonViewLand
    public void showTabletButtonView() {

        // Set the view references
        mCaptureButton = mTabletButtonView.mCaptureButton;
        mToggleResolution = mTabletButtonView.mToggleResolution;
        mToggleFlash = mTabletButtonView.mToggleFlash;
        mCloseButton = mTabletButtonView.mCloseButton;
        mToggleCamera = mTabletButtonView.mToggleCamera;
        mFastCamLayout = mTabletButtonView.mFastCamLayout;
        mFastCamLabel = mTabletButtonView.mFastCamLabel;
        mFastCamIndicator = mTabletButtonView.mFastCamIndicator;
        mCameraLayout = mTabletButtonView.mCameraLayout;
        mCameraIndicator = mTabletButtonView.mCameraIndicator;
        mCameraLabel = mTabletButtonView.mCameraLabel;
        mScannerLayout = mTabletButtonView.mScannerLayout;
        mScannerIndicator = mTabletButtonView.mScannerIndicator;
        mScannerLabel = mTabletButtonView.mScannerLabel;

        // Setup the listeners again
        setupListeners();

        // Set the button states
        if (mCameraMode != null) {
            setCameraMode(mCameraMode);
        }
        if (mResolutionMode != null) {
            setResolution(mResolutionMode);
        }
        if (mFlashMode != null) {
            setFlashModeImage(mFlashMode);
        }
        setCameraButtonVisibility();
        setFlashButtonVisibility();


        // Animate the opacity of the mTableButtonView and the mTableButtonViewLand
        mTabletButtonView.animate().alpha(1.0f).setDuration(300).start();
        mTabletButtonViewLand.animate().alpha(0.0f).setDuration(300).start();

        // Animate the opacity of the mLabelTouchTarget and mLabelTouchTargetLand
        mLabelTouchTarget.animate().alpha(1.0f).setDuration(300).start();
        mLabelTouchTargetLand.animate().alpha(0.0f).setDuration(300).start();
    }

    // This method shows the mTabletButtonViewLand and hides the mTabletButtonView
    public void showTabletButtonViewLand() {

        // Set the view references
        mCaptureButton = mTabletButtonViewLand.mCaptureButton;
        mToggleResolution = mTabletButtonViewLand.mToggleResolution;
        mToggleFlash = mTabletButtonViewLand.mToggleFlash;
        mCloseButton = mTabletButtonViewLand.mCloseButton;
        mToggleCamera = mTabletButtonViewLand.mToggleCamera;
        mFastCamLayout = mTabletButtonViewLand.mFastCamLayout;
        mFastCamLabel = mTabletButtonViewLand.mFastCamLabel;
        mFastCamIndicator = mTabletButtonViewLand.mFastCamIndicator;
        mCameraLayout = mTabletButtonViewLand.mCameraLayout;
        mCameraIndicator = mTabletButtonViewLand.mCameraIndicator;
        mCameraLabel = mTabletButtonViewLand.mCameraLabel;
        mScannerLayout = mTabletButtonViewLand.mScannerLayout;
        mScannerIndicator = mTabletButtonViewLand.mScannerIndicator;
        mScannerLabel = mTabletButtonViewLand.mScannerLabel;

        // Setup the listeners again
        setupListeners();

        // Set the button states
        if (mCameraMode != null) {
            setCameraMode(mCameraMode);
        }
        if (mResolutionMode != null) {
            setResolution(mResolutionMode);
        }
        if (mFlashMode != null) {
            setFlashModeImage(mFlashMode);
        }
        setCameraButtonVisibility();
        setFlashButtonVisibility();

        // Animate the opacity of the mTableButtonView and the mTableButtonViewLand
        mTabletButtonView.animate().alpha(0.0f).setDuration(300).start();
        mTabletButtonViewLand.animate().alpha(1.0f).setDuration(300).start();

        // Animate the opacity of the mLabelTouchTarget and mLabelTouchTargetLand
        mLabelTouchTarget.animate().alpha(0.0f).setDuration(300).start();
        mLabelTouchTargetLand.animate().alpha(1.0f).setDuration(300).start();
    }

    // This method initializes the camera preview and adds it to the appropriate layout
    private void createPreview() {

        Log.d(TAG, "createPreview");

        // Reset the mManualAutoFocus flag to its default value
        mManualAutoFocus = false;

        // Setup the outputs for the chosen camera
        try {
            setupCameraOutputs();
        }
        catch (CameraAccessException cae) {
        }

        // If necessary, create the preview view and set it as the content of the activity.  Set the size of the SurfaceHolder to the chosen preview size
        if (mPreview == null) {
            mPreview = new CameraPreview(getContext());
            mPreview.getHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreview.getHolder().addCallback(this);
            if (mPreviewLayout == null) {
                mPreviewLayout = (RelativeLayout) findViewById(R.id.camera_preview);
            }
            mPreviewLayout.addView(mPreview);
        }
        else {

            // Set the flash mode and resolution mode images
            setFlashModeImage(mFlashMode);
            setResolutionImage(mResolutionMode);

            // Initialize the camera again by closing the current camera and opening the new one after the onClosed camera state callback has executed
            mCameraClosedCallback = "openCamera";
            closeCamera();
        }
    }

    // This method sets up the camera outputs for the first camera of the chosen type; front- or rear-facing.
    private void setupCameraOutputs() throws CameraAccessException {

        CameraManager cm = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);

        // Get the id of the first camera of the chosen type that has a hardware support level greater than LEGACY
        String[] cameraIDs = cm.getCameraIdList();
        mCameraId = null;
        CameraCharacteristics cc = null;
        for (String id : cameraIDs) {
            cc = cm.getCameraCharacteristics(id);
            int deviceLevel = cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (cc.get(CameraCharacteristics.LENS_FACING) == mCameraType &&
                    deviceLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                mCameraId = id;
                break;
            }
        }
        if (mCameraId == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Could not find suitable camera.");
        }

        // Save the camera characteristics for the chosen camera
        mCharacteristics = cc;
        StreamConfigurationMap streamConfigs = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // Double check to make sure the chosen camera can output JPEGs.
        boolean supportsJpeg = false;
        for (int format : streamConfigs.getOutputFormats()) {
            if (format == ImageFormat.JPEG) {
                supportsJpeg = true;
            }
        }
        if (!supportsJpeg) {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Could not find supported image format.");
        }

        // Find the largest preview size that will fit the aspect ratio of the preview layout.

        // Get the height and width of the screen in portrait coordinates (where height > width)
        //TODO: I guess this should really be the view size and not the screen size?
        int screenWidth = getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
        int screenHeight = getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();

        // Calculate the aspect ratio of the screen
        double screenAspectRatio = (double)screenHeight/(double)screenWidth;

        // Determine if the screen's aspect ratio is closer to 4x3 or 16x9
        double aspect4x3 = 4.0/3.0;
        double aspect16x9 = 16.0/9.0;
        Size nearestAspect;
        if (Math.abs(screenAspectRatio - aspect4x3) < Math.abs(screenAspectRatio - aspect16x9)) {
            nearestAspect = new Size(4, 3);
        }
        else {
            nearestAspect = new Size(16, 9);
        }

        // Choose the optimal preview size based on the available output sizes, the screen size, and the preview layout size.
        mPreviewSize = chooseOptimalSize(streamConfigs.getOutputSizes(SurfaceHolder.class), MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, nearestAspect);
    }

    // This method initializes the camera object and sets the preview layout size once the mPreviewSurface is ready.
    private void initCamera(SurfaceHolder holder) {
        Log.d(TAG, "initCamera");

        // Initialize the ImageReader with the optimum preview size.
        mPreviewSurface = holder.getSurface();
        setJPEGImageReader(mPreviewSize);

        // Once the preview size is determined, updated the size of mPreview so that the camera view will fill the screen properly.
        configurePreviewLayout();
    }

    // This method begins displaying the camera preview after the camera object has been initialized.
    private void initPreview() {

        Log.d(TAG, "initPreview");

        try {

            // Create a CaptureRequest.Builder with the mPreviewSurface
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mPreviewSurface);

            // Set the visibility of the flash button
            setFlashButtonVisibility();

            // Create a CameraCaptureSession for the camera preview
            List<Surface> surfaces = Arrays.asList(mPreviewSurface, mJpegCaptureSurface);
            mCamera.createCaptureSession(surfaces, new StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {

                    // Make sure the camera hasn't already been closed
                    if (mCamera == null) {
                        return;
                    }

                    // Assign the mCaptureSession variable and update the camera preview
                    mCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to create camera capture session", e);
        }
    }

    // This method configures the size of the mPreviewLayout given the chosen camera preview size
    private void configurePreviewLayout() {

        // The preview layout needs to be sized so that the chosen camera preview size fits the bounds exactly.  However, in order to get the preview
        // to exactly fill the screen without being distorted, the preview layout needs to be sized larger than the screen with negative margins.

        // Get the height and width of the screen in portrait coordinates (where height > width)
        //TODO: I guess this should really be the view size and not the screen size?
        double screenWidth = (double) getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
        double screenHeight = (double) getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();

        // Calculate the aspect ratio of the screen
        double screenAspectRatio = screenHeight/screenWidth;

        // Get the height and width of the chosen preview size in portrait coordinates (where height > width)
        int theWidth = mPreviewSize.getWidth();
        int theHeight = mPreviewSize.getHeight();
        System.out.println("theWidth = " + theWidth + " theHeight = " + theHeight);
        double mPreviewWidth = (double) Math.min(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        double mPreviewHeight =(double) Math.max(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        // Calculate the aspect ratio of the chosen preview size
        double previewAspectRatio = mPreviewHeight/mPreviewWidth;

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

    // Call this whenever some camera control changes (e.g., focus distance, white balance, etc) that should affect the preview
    private void updatePreview() {

        Log.d(TAG, "updatePreview");

        try {
            if (mCamera == null || mCaptureSession == null) {
                Log.d(TAG, "updatePreview camera is null");
                return;
            }

            // Set the visibility of the flash button
            setFlashButtonVisibility();

            // Set a continuous auto focus for the camera preview
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            setFlashModeForRequest(mPreviewRequestBuilder);

            // Set the zoom level for the request
            if (mCurrentZoomLevel != 1.0) {
                Rect zoomRect = getCurrentZoomRect();
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }

            // Create the mPreviewRequest
            mPreviewRequestBuilder.setTag(1);
            mPreviewRequest = mPreviewRequestBuilder.build();

            // Start displaying the camera preview
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start preview.");
        }
    }

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
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private Size chooseOptimalSize(Size[] choices, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();

        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        // Get the width and height of the reference size that's passed in.
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        // Go through each of the camera's supported preview sizes and compare them to the reference size to find the optimal preview size to use.
        for (Size option : choices) {

            // Check if this preview size is no bigger than the max allowed width and height and that its aspect ratio matches the aspect ratio
            // of the reference size.
            if ((option.getWidth() <= maxWidth) && (option.getHeight() <= maxHeight) && (option.getHeight()*w == option.getWidth()*h)) {

                // Check if the largest dimension of this preview size is at least as large as the minimum requirement for the current
                // resolution selection
                if (Math.max(option.getWidth(), option.getHeight()) >= getDesiredImageHeightForResolution(mResolutionMode)) {
                    bigEnough.add(option);
                }
                else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest preview size of those big enough. If there is none big enough, pick the largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    // This class compares two Size objects and returns a 1 if the area of the first Size is larger or a -1 if the area of the second Size is larger.
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {

            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    // This method presents the EditPhotoCaptureActivity as long as there's a valid photoPath
    private void gotoEditPhotoCapture(String photoPath, int imgWidth, int imgHeight) {
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

        File file = new File(photoPath);

        doPhotoTaken(file, imgWidth, imgHeight);
        finishWithResult("capture");
    }

    // This method returns a boolean value describing whether or not the currently selected camera has flash capability
    public boolean hasFlash() {
        if (mCamera == null) {
            return false;
        }

        return mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
    }

    // This method sets the visibility of the flash button
    public void setFlashButtonVisibility() {

        // Make sure this runs on the main thread since it updates the UI.  Sometimes this method gets called from a background thread.
        post(new Runnable() {
            @Override
            public void run() {

                // Hide the flash button if the selected camera doesn't support flash
                if (hasFlash()) {
                    mToggleFlash.setVisibility(View.VISIBLE);
                }
                else {
                    mToggleFlash.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    // This method sets the flash setting to auto flash
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (hasFlash()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    // This method updates the flash setting
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

        // Call updatePreview to get the light to turn on or off if necessary
        updatePreview();
    }

    // This method sets the appropriate image resource for the flash button and records the given flash mode
    private void setFlashModeImage(String flashMode) {
        int imageRes;
        if (flashMode.equals("auto")) {
            imageRes = R.drawable.flash_auto;
        } else if (flashMode.equals("on")) {
            imageRes = R.drawable.flash_on;
        } else if (flashMode.equals("torch")) {
            imageRes = R.drawable.flashlight_on;
        } else {
            imageRes = R.drawable.flashlight_off;
        }

        mToggleFlash.setImageResource(imageRes);

        // Persist flash mode
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_FLASH_MODE, flashMode);
        editor.apply();
    }

    // This method sets the flash mode for the given CaptureRequestBuilder
    private void setFlashModeForRequest(CaptureRequest.Builder builder) {

        if (mFlashMode.equals("auto")) {
            setAutoFlash(builder);
        }
        else if (mFlashMode.equals("on")) {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
        }
        else if (mFlashMode.equals("torch")) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        }
        else {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
        }
    }

    // This method sets the visibility of the mToggleCamera button
    private void setCameraButtonVisibility() {

        // Check if this device contains both a rear- and front-facing camera that have a hardware support level greater than LEGACY
        boolean foundRearCamera = false;
        boolean foundFrontCamera = false;
        try {
            CameraManager cm = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);
            String[] cameraIDs = cm.getCameraIdList();
            for (String id : cameraIDs) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                int deviceLevel = cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                        deviceLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    foundRearCamera = true;
                }
                else if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT &&
                        deviceLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    foundFrontCamera = true;
                }
            }
        }
        catch (CameraAccessException cae) {
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

        if (mCameraType == CameraCharacteristics.LENS_FACING_BACK) {
            mCameraType = CameraCharacteristics.LENS_FACING_FRONT;
        }
        else {
            mCameraType = CameraCharacteristics.LENS_FACING_BACK;
        }

        // Reset the current zoom level
        mCurrentZoomLevel = 1.0;

        // Initialize the camera again
        //mPreviewLayout.removeView(mPreview);
        createPreview();
    }

    // This method sets the proper orientation for the JPEG for the given CaptureRequestBuilder as per the Android documentation
    // https://developer.android.com/reference/android/hardware/camera2/CaptureRequest.html#JPEG_ORIENTATION
    private void setJpegOrientationForRequest(CaptureRequest.Builder builder, int deviceOrientation) {

        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }
        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        if (mCameraType == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        System.out.println("The jpegOrientation is " + jpegOrientation);

        // Set the JPEG orientation
        builder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
    }

    // This method sets the appropriate images resources for all the buttons in the resolution layouts and records the resolution mode.
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
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_RESOLUTION_MODE, mResolutionMode);
        editor.apply();
    }

    // This method returns the minimum desired image height in pixels for the given resolution setting
    private int getDesiredImageHeightForResolution(String resolutionMode) {
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

    // This method updates the zoom level for the camera preview given the current motion event
    private void handleZoom(MotionEvent event) {

        // Get the maximum digital zoom level for the current camera
        //float maxZoom = (mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
        float maxZoom = (mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));

        // Get the finger spacing for this touch event
        double fingerSpacing = getFingerSpacing(event);

        if (fingerSpacing != 0) {

            // Calculate the ratio of the finger spacing at the beginning of this touch event to this finger spacing
            double fingerRatio = mStartingFingerSpacing/fingerSpacing;

            // Calculate a new zoom level that's a multiple of the zoom level at the beginning of this touch event
            double newZoom = mStartingZoomLevel/fingerRatio;

            // Bound the new zoom level by the minimum and maximum zoom levels
            newZoom = Math.min(newZoom, maxZoom);
            newZoom = Math.max(newZoom, 1.0);

            System.out.println("mStartingFingerSpacing = " + mStartingFingerSpacing + " and fingerSpacing = " + fingerSpacing);
            System.out.println("fingerRatio = " + fingerRatio + " and newZoom = " + newZoom);
            System.out.println("mStaringZoomLevel = " + mStartingZoomLevel + " and mCurrentZoomLevel = " + mCurrentZoomLevel);

            // Set the new zoom level and update the camera preview
            mCurrentZoomLevel = newZoom;
            updatePreview();
        }
    }

    // This method calculates the zoom rect for the current zoom level and camera
    private Rect getCurrentZoomRect() {
        return getRectForZoomLevel(mCurrentZoomLevel);
    }

    // This method calculates the zoom rect for the given zoom level
    private Rect getRectForZoomLevel(double zoom) {

        // Get the maximum digital zoom level for the current camera
        float minZoom = 1;
        float maxZoom = (mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));

        // Get the rect representing the full size of the current camera sensor. This rect is expressed in the camera sensor coordinate system.
        Rect m = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        // Calculate the difference between the minimum and maximum zoom levels
        double delta_z = maxZoom - minZoom;

        // Calculate the width and height of the cropped region at the maximum zoom level
        double w_zmax = m.width()/maxZoom;
        double h_zmax = m.height()/maxZoom;

        // Calculate the delta width and height between the minimum and maximum zoom levels
        double delta_w = m.width() - w_zmax;
        double delta_h = m.height() - h_zmax;

        // Calculate the cropped width and height at the current zoom level
        double w_z = ((minZoom - zoom)/(delta_z)) * delta_w + m.width();
        double h_z = ((minZoom - zoom)/(delta_z)) * delta_h + m.height();

        // Calculate the offset of the cropped region
        int centerX = m.width()/2;
        int centerY = m.height()/2;
        int deltaX = (int)(0.5f * w_z);
        int deltaY = (int)(0.5f * h_z);
        int offsetX = centerX - deltaX;
        int offsetY = centerY - deltaY;

        //System.out.println("centerX = " + centerX + ", centerY = " + centerY + ", deltaX = " + deltaX + ", deltaY = " + deltaY);
        //System.out.println("offsetX = " + offsetX + ", offsetY = " + offsetY + ", w_z = " + w_z + ", h_z = " + h_z);

        return new Rect(offsetX, offsetY, centerX + deltaX, centerY + deltaY);

    }

    // This method determines if the width and height of the current camera sensor are represented in a reference frame that's oriented
    // differently from the device's display orientation.  In this case, the width and height reported from the camera need to be swapped.
    private boolean cameraHasSwappedDimensions() {

        // Get the orientation of the display and of the current camera sensor
        int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {

            // If the camera sensor orientation isn't aligned with the display orientation, then the width and height values need to be swapped.
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        return swappedDimensions;
    }

    // This method handles the auto focus and auto exposure based on the user's touch point
    public void handleFocus(MotionEvent event) {

        LogUtil.e(TAG, "handleFocus was called");

        // Define the size of the FocusIndicatorView as 80dp
        float focusIndicatorSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());

        // Get the first pointer for this event
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);

        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        // Normalize the touch point coordinates with respect to the height and width of the preview layout
        int previewWidth = mPreviewLayout.getWidth();
        int previewHeight = mPreviewLayout.getHeight();

        // The x and y coordinates from the touch event need to be converted to normalized portrait coordinates for the regionsForNormalizedCoord() method.
        float n_y = y/previewHeight;
        float n_x = x/previewWidth;
        if (mCameraType == CameraMetadata.LENS_FACING_FRONT) {
            n_x = 1.0f - x/previewWidth;
        }

        System.out.println("handleFocus called and n_x = " + n_x + " n_y = " + n_y);
        System.out.println("handleFocus called and previewWidth = " + previewWidth + " previewHeight = " + previewHeight);

        // Get the auto focus region for this touch point
        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Rect cropRegion = getCurrentZoomRect();
        mMeteringRect = regionsForNormalizedCoord(n_x, n_y, 0.1f, cropRegion, sensorOrientation);

        // Set the position of the mFocusIndicatorView based on the touch point
        mFocusIndicatorView.left = (int)(x - (focusIndicatorSize/2));
        mFocusIndicatorView.right = (int)(x + (focusIndicatorSize/2));
        mFocusIndicatorView.top = (int)(y - (focusIndicatorSize/2));
        mFocusIndicatorView.bottom = (int)(y + (focusIndicatorSize/2));

        mFocusIndicatorView.requestLayout();
        mPreview.requestLayout();

        // Set the mManualAutoFocus flag and then lock the autofocus
        mManualAutoFocus = true;
        lockFocusToRegion(mMeteringRect);
    }

    /** Compute 3A regions for a sensor-referenced touch coordinate.
     * Returns a MeteringRectangle[] with length 1.
     *
     * @param n_x x coordinate of the touch point, in normalized portrait coordinates.
     * @param n_y y coordinate of the touch point, in normalized portrait coordinates.
     * @param fraction Fraction in [0,1]. Multiplied by min(cropRegion.width(), cropRegion.height())
     *             to determine the side length of the square MeteringRectangle.
     * @param cropRegion Crop region of the image.
     * @param sensorOrientation sensor orientation as defined by
     *             CameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION).
     */
    private static MeteringRectangle[] regionsForNormalizedCoord(float n_x, float n_y, float fraction, final Rect cropRegion, int sensorOrientation) {

        // Calculate half of the side length of the metering square based on the smaller of the cropRegion width or height
        int minCropEdge = Math.min(cropRegion.width(), cropRegion.height());
        int halfSideLength = (int) (0.5f * fraction * minCropEdge);

        // Calculate the output MeteringRectangle in the camera sensor reference frame.
        // n_x, n_y are normalized coordinates in the screen reference frame.
        // The cropRegion is expressed in the camera sensor reference frame.
        // Calculate the point nsc which is (n_x, n_y) expressed in the camera sensor reference frame.
        PointF nsc = convertNormalizedCoords(n_x, n_y, sensorOrientation);

        // Calculate the touch point represented in the camera sensor frame.
        int xCenterSensor = (int)(cropRegion.left + nsc.x * cropRegion.width());
        int yCenterSensor = (int)(cropRegion.top + nsc.y * cropRegion.height());

        // Calculate the metering region that's centered at the touch point and has a side length of 2*halfSideLength
        Rect meteringRegion = new Rect(xCenterSensor - halfSideLength, yCenterSensor - halfSideLength, xCenterSensor + halfSideLength, yCenterSensor + halfSideLength);

        // Make sure that the meteringRegion to isn't outside the cropRegion.
        meteringRegion.left = Math.max(meteringRegion.left, cropRegion.left);
        meteringRegion.left = Math.min(meteringRegion.left, cropRegion.right);
        meteringRegion.top = Math.max(meteringRegion.top, cropRegion.top);
        meteringRegion.top = Math.min(meteringRegion.top, cropRegion.bottom);
        meteringRegion.right = Math.max(meteringRegion.right, cropRegion.left);
        meteringRegion.right = Math.min(meteringRegion.right, cropRegion.right);
        meteringRegion.bottom = Math.max(meteringRegion.bottom, cropRegion.top);
        meteringRegion.bottom = Math.min(meteringRegion.bottom, cropRegion.bottom);

        System.out.println("The metering region is " + meteringRegion.toString());

        // Return a new MeteringRectangle array
        return new MeteringRectangle[]{new MeteringRectangle(meteringRegion, 800)};
    }

    // This method returns normalized coordinates in a reference frame that's rotated by rotationAngle from the reference frame in which
    // n_x and n_y are represented.
    public static PointF convertNormalizedCoords(float n_x, float n_y, int rotationAngle) {
        switch (rotationAngle) {
            case 0:
                return new PointF(n_x, n_y);
            case 90:
                return new PointF(n_y, 1.0f - n_x);
            case 180:
                return new PointF(1.0f - n_x, 1.0f - n_y);
            case 270:
                return new PointF(1.0f - n_y, n_x);
            default:
                return null;
        }
    }

    // This method locks the focus to a particular metering rectangle based on the user's touch point.  The metering rectangle must be expressed
    // in the current camera's active pixel array reference frame.
    private void lockFocusToRegion(MeteringRectangle[] meteringRect) {

        try {

            // Show the mFocusIndicatorView and add an animation to it
            mFocusIndicatorView.setVisibility(View.VISIBLE);
            FocusIndicatorAnimation animation = new FocusIndicatorAnimation(this);
            animation.setDuration(700);
            animation.setRepeatCount(Animation.INFINITE);

            animation.setAnimationListener(new Animation.AnimationListener()
            {

                public void onAnimationStart(Animation anim) {
                    // TODO Auto-generated method stub
                }

                public void onAnimationRepeat(Animation anim) {

                    // If the auto focus is locked then stop the animation from repeating.
                    if (!mManualAutoFocus) {
                        anim.setRepeatCount(0);
                    }
                }

                public void onAnimationEnd(Animation anim) {
                }
            });
            mFocusIndicatorView.startAnimation(animation);

            mPreviewRequestBuilder.setTag(2);

            // Set the control mode to auto so that the auto focus and auto exposure modes will take effect
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Set the auto focus regions
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRect);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, meteringRect);

            // Set the auto focus mode to auto
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

            // Start the auto focus
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Start the capture sequence and set the state to STATE_WAITING_LOCK
            mState = STATE_WAITING_LOCK;
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);

            // Call update preview to start the preview again
            updatePreview();
        }
        catch (CameraAccessException e) {
            // error handling
        }
    }

    static class FocusIndicatorAnimation extends Animation {

        private WeakReference<Camera2View> _view;

        public FocusIndicatorAnimation(Camera2View view) {
            this._view = new WeakReference<Camera2View>(view);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {

            _view.get().mFocusIndicatorView.radius = interpolatedTime;
            _view.get().mFocusIndicatorView.requestLayout();
        }
    }

    // This method returns the distance between the first two touch points of the given MotionEvent.
    private double getFingerSpacing(MotionEvent event) {

        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    // This method sets the resolution images for the buttons and recreates the camera view with the new preview size
    private void setResolution(String resolutionMode) {

        if (mCamera != null) {

            // Set the resolution images
            setResolutionImage(resolutionMode);

            // Initialize the camera again
            //mPreviewLayout.removeView(mPreview);
            createPreview();
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
        SharedPreferences preferences = getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_CAMERA_MODE, cameraMode);
        editor.apply();
    }

    // This method initiates the mJPEGReader object with the given Size object
    private void setJPEGImageReader(final Size jpegSize) {

        // Initiate the ImageReader object
        Log.d(TAG, "ImageReader getting sized with width = " + jpegSize.getWidth() + " and height = " + jpegSize.getHeight());
        mJPEGReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);

        // Set the listener for the mJPEGReader
        mJPEGReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "onImageAvailable");
                processPhoto(reader.acquireLatestImage());
            }
        }, null);

        mJpegCaptureSurface = mJPEGReader.getSurface();
    }

    private void capturePhoto() {

        Log.d(TAG, "capturePhoto");

        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Turn off autofocus while capturing the image
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

            // TODO: set options here that the user has changed

            // Set the flash mode
            setFlashModeForRequest(builder);

            // Set the orientation of the JPEG based on the current camera
            setJpegOrientationForRequest(builder, getResources().getConfiguration().orientation);

            // Set the zoom level for the request
            Rect zoomRect = getCurrentZoomRect();
            builder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);

            builder.addTarget(mJpegCaptureSurface);
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 100); // TODO: use the user set quality

            mCaptureSession.capture(builder.build(), new CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long framNumber) {

                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {

                    // Play the shutter click sound effect as long as the device ringer is turned on
                    AudioManager mgr = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    switch (mgr.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            MediaActionSound sound = new MediaActionSound();
                            sound.play(MediaActionSound.SHUTTER_CLICK);
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Image capture failed.");
                }
            }, null);


        } catch (CameraAccessException e) {
            Log.e(TAG, "Image capture failed.", e);
        }
    }

    // This method initializes all of the touch listeners and click listeners for the interface elements.
    private void setupListeners() {

        // Set a touch listener for the preview layout to handle zooming and tap-to-focus
        mPreviewLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {

                // If the camera is null, then return
                if (mCamera == null) {
                    LogUtil.e(TAG, "The camera instance was null when the screen was touched.");
                    return false;
                }

                // Handle the touch event as long as the camera isn't null
                if (mCamera != null) {

                    final int action = event.getAction();

                    // Dismiss the resolution layout if it's showing
                    if (mResolutionLayoutVisible) {
                        hideResolutionLayout();
                    }

                    // Handle multi-touch events
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {

                    }
                    if (action == MotionEvent.ACTION_DOWN) {

                    }
                    else if (action == MotionEvent.ACTION_MOVE) {

                        // Check if this is a multi-touch event
                        if (event.getPointerCount() > 1) {

                            // Set the mMultiTouchDetected flag
                            mMultiTouchDetected = true;

                            // Record the initial finger spacing and zoom level if necessary
                            if (mStartingFingerSpacing == -1.0) {
                                mStartingFingerSpacing = getFingerSpacing(event);
                                mStartingZoomLevel = mCurrentZoomLevel;
                            }

                            // Handle the zoom event and update the preview
                            handleZoom(event);
                        }
                    }

                    else if (action == MotionEvent.ACTION_UP) {

                        // Trigger the tap-to-autofocus if this was a single tap
                        if (event.getPointerCount() == 1 && !mMultiTouchDetected) {
                            handleFocus(event);
                        }
                        else {

                            // Reset the mMultiTouchDetected flag
                            mMultiTouchDetected = false;
                        }

                        // Reset the mStartingFingerSpacing
                        mStartingFingerSpacing = -1.0;

                        // Dismiss the resolution layout if it's showing
                        if (mResolutionLayoutVisible) {
                            hideResolutionLayout();
                        }
                    }
                }
                else {
                    LogUtil.e(TAG, "Unable to retrieve camera instance in the touch listener.");
                    return false;
                }

                return true;
            }
        });

        // Set a listener for the capture button
        mCaptureButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                if (mCamera != null) {
                    try {
                        //capturePhoto();
                        takePicture();
                    } catch (RuntimeException re) {
                        Log.e(TAG, "RuntimeEx takePicture" + re.getMessage());
                    }
                }
            }
        });

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

    // This method opens a new camera
    private void openCamera(SurfaceHolder holder) {

        if (!checkCameraPermissions()) {
            System.err.println("checkCameraPermissions returned false");
            return;
        }

        //setUpCameraOutputs(width, height);
        //configureTransform(width, height);

        CameraManager manager = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);
        try {

            // Initialize the camera parameters
            initCamera(holder);

            // Make sure that a lock on the camera opening can be acquired before trying to open the camera
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            //manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            manager.openCamera(mCameraId, mStateCallback, null);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
        catch (SecurityException e) {
        }
    }

    // This method closes the current camera
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCamera != null) {
                mCamera.close();
                mCamera = null;
            }
            if (mJPEGReader != null) {
                mJPEGReader.close();
                mJPEGReader = null;
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        finally {
            mCameraOpenCloseLock.release();
        }
    }

    // This method starts a background thread and its handler.
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    // This method stops the background thread and its handler.
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////
    // Photo capture methods //
    ///////////////////////////

    // This method initiates a still image capture by calling the lockFocus() method to lock the auto focus.
    private void takePicture() {

        // Set this flag to true to bypass the full AF and AE routine for capturing a photo in basic Camera mode.
        boolean bypassAutoFocus = mFlashMode.equals("off") || mFlashMode.equals("torch");

        // If the camera is in FastCam mode, then capture the photo immediately without going through the auto focus and auto exposure.
        if (mCameraMode.equals("fastcam") || bypassAutoFocus) {
            captureStillPicture();
        }

        // Otherwise, try to lock the focus before capturing the photo
        else {
            lockFocus();
        }
    }

    // This method locks the auto focus as the first step for a still image capture.
    private void lockFocus() {
        try {

            // Set the state in the request builder to initiate the auto focus lock.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            // Update the camera state to indicate that the preview is waiting for the auto focus to lock
            mState = STATE_WAITING_LOCK;

            // Use the mCaptureSession to initiate the auto focus lock
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // This method runs the precapture sequence for capturing a still image. It starts the auto exposure.
    // This method should be called when the mCaptureCallback gets a response from lockFocus().
    private void runPrecaptureSequence() {
        try {

            System.out.println("runPrecaptureSequence was called");

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Set the states in the request builder to initiate the auto exposure.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Add the metering region to the request if the mManualAutoFocus flag is set
            if (mManualAutoFocus) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, mMeteringRect);
            }

            // Update the camera state to indicate that the preview is waiting for the auto exposure to finish
            mState = STATE_WAITING_PRECAPTURE;

            // Use the mCaptureSession to initiate the auto exposure
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // This method captures a still picture. This method should be called when we get a response in the mCaptureCallback from both
    // lockFocus() and runPrecaptureSequence().
    private void captureStillPicture() {
        System.out.println("captureStillPicture was called");

        try {
            if (mCamera == null) {
                return;
            }

            // Create a CaptureRequest.Builder to use to take the photo.
            final CaptureRequest.Builder captureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mJpegCaptureSurface);

            // Use the same auto exposure and auto focus modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Set the flash mode for the request
            setFlashModeForRequest(captureBuilder);

            // Set the zoom level for the request
            Rect zoomRect = getCurrentZoomRect();
            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);

            // Set the orientation for the JPEG
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    // Play the shutter click sound effect as long as the device ringer is turned on
                    AudioManager mgr = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    switch (mgr.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            MediaActionSound sound = new MediaActionSound();
                            sound.play(MediaActionSound.SHUTTER_CLICK);
                            break;
                        default:
                            break;
                    }

                    // Animate the screen flash when the image is captured if the camera is in FastCam mode.
                    //if (mCameraMode.equals("fastcam")) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                animateScreenFlash();
                            }
                        });
                    //}

                    // Unlock the auto focus again after the photo is taken
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        System.out.println("in getOrientation rotation = " + rotation + " and sensorOrientation = " + sensorOrientation);

        // Per the Google example project Camera2Basic, the angle at which the JPEG needs to be rotated is a function of the device orientation
        // and the sensor orientation + 270.  However, implementing this resulted in images being upside down for any sensors that have an
        // orientation of 270.  Simply using the value from the ORIENTATIONS array has resulted in correct captured image orientations on all
        // tested devices so far because the images are rotated manually after the JPEG is captured before it's stored in the file system.
        //return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
        return (ORIENTATIONS.get(rotation));
    }

    // This method briefly flashes the screen as a visual indicator that a photo was captured
    private void animateScreenFlash() {

        // Make sure the mFocusIndicatorView is hidden
        mFocusIndicatorView.setVisibility(View.INVISIBLE);

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

    // This method unlocks the auto focus when the still image capture sequence is finished.
    private void unlockFocus() {
        try {

            System.out.println("unlockFocus called");

            // Make sure this runs on the main thread since it updates the UI.  Sometimes this method gets called from a background thread.
            post(new Runnable() {
                @Override
                public void run() {

                    mFocusIndicatorView.setVisibility(View.INVISIBLE);
                }
            });

            // Reset the auto focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);

            // Reset the auto exposure trigger
            if(android.os.Build.VERSION.SDK_INT >= 23) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            }

            // Start the preview again
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);

            // Set the camera state back to the default preview state
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // —————————————————————————————————————————————————————————————————————————————————————————————
    // Process Photo methods
    // —————————————————————————————————————————————————————————————————————————————————————————————
    private void processPhoto(Image image) {
        Log.d(TAG, "processPhoto()");

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);

        image.close();

        Log.d(TAG, "byte[] data created");

        if (data != null) {
            Bitmap bPhoto = null;
            try {

                BitmapFactory.Options options = new BitmapFactory.Options();

                // Decoding the data with inJustDecodeBounds = true returns a null bitmap, but it decodes the size without having to
                // allocate memory for all the pixels.
                //options.inJustDecodeBounds = true;
                //BitmapFactory.decodeByteArray(data, 0, data.length, options);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;

                // Get the bitmap (options will resize it if set)
                bPhoto = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                Log.d(TAG, "Bitmap bPhoto decoded from data.");

                // The attempt to decode the data can sometimes fail and return null.  If that happens, display a message to the user and restart the camera preview..
                if (bPhoto == null) {

                    new AlertDialog.Builder(getContext())
                            .setTitle("Error")
                            .setMessage("Something went wrong while taking this photo. Try taking a picture with your camera app and uploading it.")
                            .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO: restart the camera somehow
                                    dialog.dismiss();
                                }
                            }).create().show();

                    return;
                }

                int orientation = ((mLastOrientation + 45) / 90) * 90;

                // Reverse device orientation for front-facing cameras
                if (mCameraType == CameraCharacteristics.LENS_FACING_FRONT) {
                    orientation = -orientation;
                }

                // Calculate the rotation angle to apply to the image
                int rotation = 0;
                int cameraOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                rotation = (cameraOrientation + orientation) % 360;

                // Create the transformation matrix for the image
                Matrix matrix = new Matrix();
                Log.d(TAG, "before getWidth()");
                int theW = bPhoto.getWidth();
                Log.d(TAG, "after getWidth()");
                Log.d(TAG, "before getHeight()");
                int theH = bPhoto.getHeight();
                Log.d(TAG, "after getHeight()");

                // Mirror the image for the front-facing camera
                if (mCameraType == CameraCharacteristics.LENS_FACING_FRONT) {
                    matrix.setScale(1, -1);
                    matrix.postTranslate(0, theH);

                    if (theH > theW) {
                        rotation = (rotation - 270) % 360;
                    }
                }
                else if (theH > theW) {
                    rotation = (rotation - 90) % 360;
                }

                // Rotate the image according to the sensor and device orientations
                matrix.postRotate(rotation);

                Log.d(TAG, "calculations done, ready to rotate");
                bPhoto = Bitmap.createBitmap(bPhoto, 0, 0, theW, theH, matrix, false); // This is slow ~3 seconds

                Log.d(TAG, "Before cropping the photo is " + bPhoto.getWidth() + " x " + bPhoto.getHeight());

                // Get the height and width of the screen in portrait coordinates (where height > width)
                //TODO: I guess this should really be the view size and not the screen size?
                double screenWidth = (double) getWidth(); //CompanyCamApplication.getInstance().getScreenPortraitPixelWidth();
                double screenHeight = (double) getHeight(); //CompanyCamApplication.getInstance().getScreenPortraitPixelHeight();

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

                Log.d(TAG, "bPhoto saved to mFile");

                //TODO: better if gotoEdit/uploadFastCam are done *after* exif is set and bPhoto is recycled?
                // Transition to the EditPhotoCaptureActivity as long as the current mode isn't FastCam
                if (!mCameraMode.equals("fastcam")) {
                    gotoEditPhotoCapture(photo.getPath(), imgWidth, imgHeight);
                }

                // If the current mode is FastCam, then upload the photo immediately
                else {
                    uploadFastCamPhoto(photo, imgWidth, imgHeight);
                }

                try {
                    SharedPreferences preferences = getSharedPreferences();
                    String flashMode = preferences.getString(PREFS_FLASH_MODE, "auto");
                    ExifUtils.setAttributes(photo, getExifLocation(), flashMode);
                } catch (IOException e) {
                    LogUtil.e(TAG, e.getLocalizedMessage());
                }

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            } catch (OutOfMemoryError oome) {
                Log.e(TAG, "OutOfMemoryError: " + oome.getMessage());
                finishWithError("Out of memory: " + oome.getMessage());
            } finally {
                if (bPhoto != null) {
                    bPhoto.recycle();
                    bPhoto = null;
                }
            }
        }
    }

    private File getPhotoPath() {
        File dir = appPhotoDirectory;
        dir.mkdirs();
        return (new File(dir, StorageUtility.getNewFileName()));
    }

    // —————————————————————————————————————————————————————————————————————————————————————————————
    // Surface Holder methods
    // —————————————————————————————————————————————————————————————————————————————————————————————
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated");

        if(mCaptureSession != null) {
            mCaptureSession.close();
        }

        // Set the SurfaceCreated flag
        mPreview.mSurfaceCreated = true;

        // The Surface has been created, now initialize the camera

        // Set the flash mode and resolution mode images
        setFlashModeImage(mFlashMode);
        setResolutionImage(mResolutionMode);

        // Initialize the camera
        openCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.d(TAG, "surfaceDestroyed");

        // Set the mSurfaceCreated flag
        mPreview.mSurfaceCreated = false;

        LogUtil.e(TAG, "surfaceDestroyed was called");

        if (mCamera != null) {
            mCameraClosedCallback = "";
            closeCamera();
            LogUtil.e(TAG, "The camera was released");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Log.d(TAG, "surfaceChanged width = " + w + " and height = " + h);

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mPreview.getHolder().getSurface() == null) {

            Log.d(TAG, "the surface was null in surfaceChanged");
            // preview surface does not exist
            return;
        }

        // set preview size and make any resize, rotate or reformatting changes here

        // start preview with new settings
        //createPreview();
    }

    // This method uploads photos taken while in FastCam mode
    private void uploadFastCamPhoto(File photo, int imgWidth, int imgHeight) {

        // If saveToPhone is set, then save the image to the device in addition to sending it to the server.
        SharedPreferences preferences = getSharedPreferences();
        boolean saveToPhone = preferences.getBoolean(PhotoActions.PREF_KEY_SAVE_TO_PHONE, false);
        if (saveToPhone) {

            // Try writing the image to the device. This method will return null if the image can't be saved successfully.
            String imageURL = PhotoActions.writeImageToDevice(getContext(), Uri.fromFile(photo));
        }

        doPhotoAccepted(photo, imgWidth, imgHeight);
    }

    // This is a helper method for logging the auto focus state
    private static String getAFStateString(int afState) {
        switch (afState) {
            case 0:
                return "CONTROL_AF_STATE_INACTIVE";
            case 1:
                return "CONTROL_AF_STATE_PASSIVE_SCAN";
            case 2:
                return "CONTROL_AF_STATE_PASSIVE_FOCUSED";
            case 3:
                return "CONTROL_AF_STATE_ACTIVE_SCAN";
            case 4:
                return "CONTROL_AF_STATE_FOCUSED_LOCKED";
            case 5:
                return "CONTROL_AF_STATE_NOT_FOCUSED_LOCKED";
            case 6:
                return "CONTROL_AF_STATE_PASSIVE_UNFOCUSED";
            default:
                return "Unknown";
        }
    }

    // This is a helper method for logging the auto exposure state
    private static String getAEStateString(int aeState) {
        switch (aeState) {
            case 0:
                return "CONTROL_AE_STATE_INACTIVE";
            case 1:
                return "CONTROL_AE_STATE_SEARCHING";
            case 2:
                return "CONTROL_AE_STATE_CONVERGED";
            case 3:
                return "CONTROL_AE_STATE_LOCKED";
            case 4:
                return "CONTROL_AE_STATE_FLASH_REQUIRED";
            case 5:
                return "CONTROL_AE_STATE_PRECAPTURE";
            default:
                return "Unknown";
        }
    }
}
