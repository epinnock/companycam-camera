package com.newcam.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.newcam.R;

/**
 * Created by mattboyd on 1/1/17.
 */

public class TabletButtonView extends LinearLayout {

    public Context mContext;
    public int layoutResourceID = R.layout.view_tablet_button;

    public ImageButton mCaptureButton;
    public ImageButton mToggleResolution;
    public ImageButton mToggleFlash;
    public ImageButton mCloseButton;
    public ImageButton mToggleCamera;

    // These views and text labels are for the camera options labels
    public LinearLayout mFastCamLayout;
    public ImageView mFastCamIndicator;
    public TextView mFastCamLabel;

    public LinearLayout mCameraLayout;
    public ImageView mCameraIndicator;
    public TextView mCameraLabel;

    public LinearLayout mScannerLayout;
    public ImageView mScannerIndicator;
    public TextView mScannerLabel;

    public LinearLayout mAuxLayout;
    public ImageView mAuxIndicator;
    public TextView mAuxLabel;

    public TabletButtonView(Context context) {
        super(context);
        mContext = context;
        initView(context);
    }

    public TabletButtonView(Context context, AttributeSet attrs) {
        super(context);
        mContext = context;
        initView(context);
    }

    // This method inflates the appropriate layout for this view
    public void initView(Context context) {

        if (this.layoutResourceID != -1) {

            // Remove any previous child views that have been added
            this.removeAllViews();

            // Inflate the appropriate layout
            View thisView = View.inflate(context, this.layoutResourceID, this);

            // Get references to the subviews
            mCaptureButton = (ImageButton) findViewById(R.id.capture);
            mToggleResolution = (ImageButton) findViewById(R.id.toggle_resolution);
            mToggleFlash = (ImageButton) findViewById(R.id.toggle_flash);
            mCloseButton = (ImageButton) findViewById(R.id.close_button);
            mToggleCamera = (ImageButton) findViewById(R.id.toggle_camera);

            mFastCamLayout = (LinearLayout) findViewById(R.id.fastcam_layout);
            mFastCamLabel = (TextView) findViewById(R.id.fastcam_label);
            mFastCamIndicator = (ImageView) findViewById(R.id.fastcam_selected_icon);

            mCameraLayout = (LinearLayout) findViewById(R.id.camera_layout);
            mCameraLabel = (TextView) findViewById(R.id.camera_label);
            mCameraIndicator = (ImageView) findViewById(R.id.camera_selected_icon);

            mScannerLayout = (LinearLayout) findViewById(R.id.scanner_layout);
            mScannerLabel = (TextView) findViewById(R.id.scanner_label);
            mScannerIndicator = (ImageView) findViewById(R.id.scanner_selected_icon);

            mAuxLayout = (LinearLayout) findViewById(R.id.aux_layout);
            mAuxLabel = (TextView) findViewById(R.id.aux_label);
            mAuxIndicator = (ImageView) findViewById(R.id.aux_selected_icon);
        }
    }
}
