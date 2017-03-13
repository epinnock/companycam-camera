package com.newcam.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.newcam.CCCameraManager;
import com.newcam.CCCameraView;
import com.newcam.R;
import com.newcam.cameras.CCCamera;
import com.newcam.utils.CCCameraLayoutInterface;
import com.notagilx.companycam.util.SingleClickListener;
import com.notagilx.companycam.util.views.FocusIndicatorView;

import java.lang.ref.WeakReference;

/**
 * Created by mattboyd on 1/31/17.
 */

public class CCCameraLayout extends RelativeLayout implements CCCameraLayoutInterface {

    private static String TAG = CCCameraLayout.class.getSimpleName();

    public Context mContext;

    // The CCCamera object maintains a reference to the camera and implements the necessary camera API methods
    public CCCamera mCamera;

    // The mOrientationListener is used to update the UI elements after major device orientation changes
    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    // The useTabletLayout flag describes whether the tablet layout is being used for this device
    public boolean useTabletLayout = false;

    // The mPhonePosition is used to track the device interface orientation
    public static final int PORTRAIT_TOP_UP = 1;
    public static final int PORTRAIT_TOP_DOWN = 2;
    public static final int LANDSCAPE_TOP_LEFT = 3;
    public static final int LANDSCAPE_TOP_RIGHT = 4;
    private int mPhonePosition;

    // The mTopLayout contains the place labels, close button, and resolution button
    private LinearLayout mTopLayout;
    public TextView mPlaceName;
    public TextView mPlaceAddress;
    private ImageButton mCloseButton;
    private ImageButton mToggleResolution;

    // The mLabelTouchTarget and mLabelTouchTargetLand are used to dismiss the view when the place labels are touched
    private LinearLayout mLabelTouchTarget;
    private LinearLayout mLabelTouchTargetLand;

    // The mBottomLayout contains the camera buttons and camera mode labels
    private LinearLayout mBottomLayout;

    // The mToggleCamera button allows the user to switch between rear- and forward-facing cameras
    private ImageButton mToggleCamera;

    // The mCaptureButton allows the user to capture a photo
    private ImageButton mCaptureButton;

    // The mToggleFlash button allows the user to turn the flash on or off
    private ImageButton mToggleFlash;

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
    public boolean mResolutionLayoutVisible = false;

    // The mTabletButtonView is the layout that holds all the buttons for tablets
    private TabletButtonView mTabletButtonView;
    private TabletButtonView mTabletButtonViewLand;

    // The mFocusIndicatorView is used to indicate that the camera is in the process of focusing
    public FocusIndicatorView mFocusIndicatorView;

    // The mScreenFlashView is used to provide a flash effect when the user snaps a photo
    private FrameLayout mScreenFlashView;

    // The CLICK_REJECTION_INTERVAL is an amount of time in milliseconds that must elapse before a button click will be processed.
    // This is used to reject multiple clicks in quick succession.
    private static int CLICK_REJECTION_INTERVAL = 1500;

    public CCCameraLayout(Context context) {
        super(context);
        init(context);
    }

    public CCCameraLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {

        mContext = context;

        // Inflate the layout resource for this view
        inflate(context, R.layout.layout_cccamera, this);

        // Get references to the subviews
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
            mTabletButtonView = new TabletButtonView(mContext);
            tabletButtonLayout.addView(mTabletButtonView, tabletParams);
            mTabletButtonViewLand = new TabletButtonView(mContext);
            tableButtonLayoutLand.addView(mTabletButtonViewLand, tabletParams);

            // Set the layout resources for the two tablet button views
            mTabletButtonView.layoutResourceID = R.layout.view_tablet_button;
            mTabletButtonViewLand.layoutResourceID = R.layout.view_tablet_button_land;

            // Initialize the tablet button views
            mTabletButtonView.initView(mContext);
            mTabletButtonViewLand.initView(mContext);

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

            // Show the mTabletButtonView by default
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

        // Set the default button orientations
        int rotationValue = 0;
        mCloseButton.setRotation(rotationValue);
        mToggleResolution.setRotation(rotationValue);
        mToggleFlash.setRotation(rotationValue);
        mToggleCamera.setRotation(rotationValue);

        // Initialize the orientation listener
        initOrientationListener();
    }

    // This method sets up the interface elements after the CCCamera object has been set
    public void setCamera(CCCamera camera) {

        mCamera = camera;

        // Set the visibility of the flash button
        setFlashButtonVisibility();

        // Set the flash mode image
        setFlashModeImage(mCamera.mFlashMode);

        // Set the resolution mode image
        setResolutionImage(mCamera.mResolutionMode);

        // Set the visibility of the camera button
        setCameraButtonVisibility();

        // Set the camera layout state
        setCameraMode(mCamera.mCameraMode);

        // Set the button listeners
        setupListeners();
    }

    // This method initializes an orientation listener to update the UI after major orientation changes
    private void initOrientationListener() {
        mOrientationListener =
                new OrientationEventListener(getContext()) {

                    public void onOrientationChanged(int orientation) {
                        // We keep the last known orientation. So if the user
                        // first orient the camera then point the camera to
                        // floor/sky, we still have the correct orientation.
                        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {

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
                                //else {

                                    // Set the icons for the mToggleResolution and mCloseButton
                                    setResolutionImage(mCamera.mResolutionMode);
                                    mCloseButton.setImageResource(R.drawable.close_icon);
                                //}
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
                                    setResolutionImage(mCamera.mResolutionMode);
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

    // This method initializes all of the touch listeners and click listeners for the interface elements.
    private void setupListeners() {

        // Set a listener for the capture button
        mCaptureButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {
                if (mCamera != null) {
                    try {
                        // Capture a photo
                        mCamera.takePicture();
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

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("normal");
                mCamera.setResolution("normal");
            }
        });

        // Set a listener for the mHighButton
        mHighButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("high");
                mCamera.setResolution("high");
            }
        });

        // Set a listener for the mSuperButton
        mSuperButton.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("super");
                mCamera.setResolution("super");
            }
        });

        // Set a listener for the mNormalButtonLand
        mNormalButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("normal");
                mCamera.setResolution("normal");
            }
        });

        // Set a listener for the mHighButtonLand
        mHighButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("high");
                mCamera.setResolution("high");
            }
        });

        // Set a listener for the mSuperButtonLand
        mSuperButtonLand.setOnClickListener(new SingleClickListener(CLICK_REJECTION_INTERVAL) {
            @Override
            public void onSingleClick(View v) {

                // Set the resolution image and set the resolution of the camera
                setResolutionImage("super");
                mCamera.setResolution("super");
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

        // Set a listener for the mToggleResolution button
        mToggleResolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleResolution();
            }
        });

        // Set a listener for the mToggleFlash button
        mToggleFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Set the flash mode for the camera and set the flash image
                mCamera.toggleFlash();
                setFlashModeImage(mCamera.mFlashMode);
            }
        });

        // Set a listener for the mCloseButton
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeButtonClick();
            }
        });

        // Set a listener for the mLabelTouchTarget layout
        mLabelTouchTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelTouch();
            }
        });

        // Set a listener fro the mToggleCamera button
        mToggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.toggleCamera();
            }
        });
    }

    /////////////////////////
    // mResolution layouts //
    /////////////////////////

    // This method sets the proper button orientation for the mResolutionLayout
    private void setupResolutionLayout() {
        mNormalButtonLand.setRotation(90);
        mHighButtonLand.setRotation(90);
        mSuperButtonLand.setRotation(90);
        mResolutionDismissButtonLand.setRotation(180);
    }

    // This method handles the touch action on the layout with the place name and place address labels
    private void labelTouch() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // Otherwise, let the CCCameraView handle the touch action
        CCCameraManager.getLatestView().labelTouch();
    }

    // This method either shows the resolution layout or dismisses the view depending on the current orientation of the device
    private void toggleResolution() {

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
            // Dismiss the view
            CCCameraManager.getLatestView().finishWithResult("close");
        }
    }

    // This method either shows the resolution layout or dismisses the view depending on the current orientation of the device
    protected void closeButtonClick() {

        // If the resolution layout is displayed, this button click shouldn't have any action, so simply return
        if (mResolutionLayoutVisible) {
            return;
        }

        // The action for the close button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape in the phone layout
        if (mPhonePosition == PORTRAIT_TOP_UP || useTabletLayout) {

            // This is the close button action
            // Dismiss the view
            CCCameraManager.getLatestView().finishWithResult("close");
        }
        else {

            // This is the resolution button action
            showResolutionLayout();
        }
    }

    /////////////////////////////////////
    // CCCameraLayoutInterface methods //
    /////////////////////////////////////

    // This method sets the visibility of the mToggleFlash button
    @Override
    public void setFlashButtonVisibility() {

        // Make sure this runs on the main thread since it updates the UI.  Sometimes this method gets called from a background thread.
        post(new Runnable() {
            @Override
            public void run() {

                // Hide the flash button if the selected camera doesn't support flash
                if (mCamera.hasFlash()) {
                    mToggleFlash.setVisibility(View.VISIBLE);
                }
                else {
                    mToggleFlash.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    // This method sets the flash mode and updates the flash button appropriately
    @Override
    public void setFlashModeImage(String flashMode) {
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
        mCamera.persistFlashMode(flashMode);
    }

    // This method sets the appropriate images resources for all the buttons in the resolution layouts and records the resolution mode.
    @Override
    public void setResolutionImage(String resolutionMode) {

        // Determine whether the mToggleResolution button or the mCloseButton is currently controlling the resolution selection based on the
        // device orientation.  If this device is using the tablet layout, then the buttons are never reversed.
        ImageButton resolutionButton = mToggleResolution;
        if (mPhonePosition == LANDSCAPE_TOP_LEFT && !useTabletLayout) {
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
        }

        // Persist resolution mode
        mCamera.persistResoultionMode(resolutionMode);
    }

    // This method sets the visibility of the mToggleCamera button
    @Override
    public void setCameraButtonVisibility() {

        // Make sure this runs on the main thread since it updates the UI.  Sometimes this method gets called from a background thread.
        post(new Runnable() {
            @Override
            public void run() {

                // Show the camera button only if the device has both a rear- and front-facing camera
                if (mCamera.hasRearCamera() && mCamera.hasFrontCamera()) {
                    mToggleCamera.setVisibility(View.VISIBLE);
                }
                else {
                    mToggleCamera.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    // This method sets the camera mode and updates the camera mode labels accordingly
    @Override
    public void setCameraMode(String cameraMode) {

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

        // Persist the camera mode
        mCamera.persistCameraMode(cameraMode);
    }

    // This method animates the presentation of the resolution layout when the resolution button is tapped
    @Override
    public void showResolutionLayout() {

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
    @Override
    public void hideResolutionLayout() {

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

    // This method briefly flashes the screen as a visual indicator that a photo was captured
    @Override
    public void animateScreenFlash() {

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

    @Override
    public void showAutoFocusIndicator(float x, float y, final boolean setRepeating) {

        // Define the size of the FocusIndicatorView as 80dp
        float focusIndicatorSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());

        // Set the position of the mFocusIndicatorView based on the touch point
        mFocusIndicatorView.left = (int)(x - (focusIndicatorSize/2));
        mFocusIndicatorView.right = (int)(x + (focusIndicatorSize/2));
        mFocusIndicatorView.top = (int)(y - (focusIndicatorSize/2));
        mFocusIndicatorView.bottom = (int)(y + (focusIndicatorSize/2));

        mFocusIndicatorView.requestLayout();

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

                // If the setRepeating flag isn't set, then stop the animation from repeating.
                if (!setRepeating) {
                    anim.setRepeatCount(0);
                }
            }

            public void onAnimationEnd(Animation anim) {
            }
        });
        mFocusIndicatorView.startAnimation(animation);
    }

    @Override
    public void hideAutoFocusIndicator() {
        mFocusIndicatorView.clearAnimation();
        mFocusIndicatorView.setVisibility(View.INVISIBLE);
    }

    ///////////////////////////////
    // mTabletButtonView layouts //
    ///////////////////////////////

    // This method shows the mTabletButtonView and hides the mTabletButtonViewLand
    public void showTabletButtonView() {

        // Set the view references
        mPlaceName = (TextView)mLabelTouchTarget.findViewById(R.id.place_name);
        mPlaceAddress = (TextView)mLabelTouchTarget.findViewById(R.id.place_address);
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
        CCCameraView thisCameraView = CCCameraManager.getLatestView();
        if (thisCameraView != null && thisCameraView.placeName != null) {
            mPlaceName.setText(thisCameraView.placeName);
        }
        if (thisCameraView != null && thisCameraView.placeAddress != null) {
            mPlaceAddress.setText(thisCameraView.placeAddress);
        }
        if (mCamera != null && mCamera.mCameraMode != null) {
            setCameraMode(mCamera.mCameraMode);
        }
        if (mCamera != null && mCamera.mResolutionMode != null) {
            setResolutionImage(mCamera.mResolutionMode);
        }
        if (mCamera != null && mCamera.mFlashMode != null) {
            setFlashModeImage(mCamera.mFlashMode);
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
        mPlaceName = (TextView)mLabelTouchTargetLand.findViewById(R.id.place_name_land);
        mPlaceAddress = (TextView)mLabelTouchTargetLand.findViewById(R.id.place_address_land);
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
        CCCameraView thisCameraView = CCCameraManager.getLatestView();
        if (thisCameraView != null && thisCameraView.placeName != null) {
            mPlaceName.setText(thisCameraView.placeName);
        }
        if (thisCameraView != null && thisCameraView.placeAddress != null) {
            mPlaceAddress.setText(thisCameraView.placeAddress);
        }
        if (mCamera != null && mCamera.mCameraMode != null) {
            setCameraMode(mCamera.mCameraMode);
        }
        if (mCamera != null && mCamera.mResolutionMode != null) {
            setResolutionImage(mCamera.mResolutionMode);
        }
        if (mCamera != null && mCamera.mFlashMode != null) {
            setFlashModeImage(mCamera.mFlashMode);
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

    ///////////////////////////
    // Miscellaneous methods //
    ///////////////////////////

    static class FocusIndicatorAnimation extends Animation {

        private WeakReference<CCCameraLayout> _view;

        public FocusIndicatorAnimation(CCCameraLayout view) {
            this._view = new WeakReference<CCCameraLayout>(view);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {

            _view.get().mFocusIndicatorView.radius = interpolatedTime;
            _view.get().mFocusIndicatorView.requestLayout();
        }
    }
}