package com.agilx.companycam.util.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.agilx.companycam.core.web.model.Place;
import com.agilx.companycam.util.DeviceUtil;
import com.newcam.views.NewCameraView;

public class CameraPreview extends SurfaceView {

    private static String  TAG =CameraPreview.class.getSimpleName();
    private SurfaceHolder mHolder;
    private Context mContext;
    private final Rect textBounds = new Rect();
    private Rect background = new Rect();
    private Place mPlace;
    private int phonePosition;

    private String placeName;
    private String placeAddress;

    // mSurfaceCreated is a boolean that describes whether the surfaceCreated callback has already executed
    public boolean mSurfaceCreated = false;

    public int mWidth = 0;
    public int mHeight = 0;
    public int mLeft = 0;
    public int mTop = 0;

    int padding;
    int doubleLineOffset;
    final int ALPHA = 64;
    final int PADDING = 18;

    // This initializer accepts the name and address of the place instead of using a Place object
    public CameraPreview(Context context, String name, String address) {
        super(context);
        mContext = context;
        placeName = name;
        placeAddress = address;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        //mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        padding = Math.round(DeviceUtil.dpToPx(context, PADDING));
        doubleLineOffset = Math.round(DeviceUtil.dpToPx(context, 5));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWidth != 0 && mHeight != 0) {
            setMeasuredDimension(mWidth, mHeight);
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void drawOverlay(Canvas canvas, boolean errorOccurred, String errorMsg) {
        String text;
        Paint paint = new Paint();
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String placeInfo = "";

        // The previous implementation used a Place object to get the place information, but in the React Native implementation that info
        // is passed directly from the Javascript app, so there's no need to check if the mPlace is null since it isn't used.
        /*if (mPlace != null) {
            placeInfo = setPlaceInfo(mPlace);
        }*/
        placeInfo = setPlaceInfo(mPlace);

        if (errorOccurred) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
                text = errorMsg;
        } else {
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(ALPHA);
            text = placeInfo;
        }
        //drawTextCentred(canvas, paint, text);
    }

    public void drawTextCentred(Canvas canvas, Paint paint, String text){

        if (phonePosition != NewCameraView.PORTRAIT_TOP_UP) {
            paint.getTextBounds(text, 0, text.length(), textBounds);

            background.set(textBounds);
            background.left = 0; // x1
            background.right = canvas.getWidth();
            background.top = canvas.getHeight() - textBounds.height() - padding - padding ; //Y1
            background.bottom = background.top + textBounds.height() + padding + textBounds.height() + padding;
            canvas.drawRect(background, paint);

            paint.setColor(Color.WHITE);
            //paint.setTextSize(DeviceUtil.dpToPx(CompanyCamApplication.getInstance(), 15));
            paint.setTextSize(DeviceUtil.dpToPx(mContext, 15)); //TODO
            canvas.drawText(text, // Text to display
                    (canvas.getWidth() / 2) - (paint.measureText(text) / 2),
                    canvas.getHeight() - padding / 2,
                    paint
            );
        } else {
            canvas.save();
            canvas.rotate(270, canvas.getWidth() / 2, canvas.getHeight() / 2);

            Matrix matrix = canvas.getMatrix();

            // Create new float[] to hold the rotated coordinates
            float[] pts = new float[2];

            // Initialize the array with origin
            pts[0] = 0;
            pts[1] = 0;

            // Use the Matrix to map the points
            matrix.mapPoints(pts);

            // Now, create a new Point from our new coordinates
            Point newOrigin = new Point((int) pts[0], (int) pts[1]);

            paint.getTextBounds(text, 0, text.length(), textBounds);

            background.set(textBounds);
            background.left = 0; // x1
            background.right = canvas.getWidth();
            background.top = newOrigin.y - textBounds.height() - padding - padding; //Y1
            background.bottom = background.top + textBounds.height() + padding + textBounds.height() + padding;
            canvas.drawRect(background, paint);

            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
            //paint.setTextSize(DeviceUtil.dpToPx(CompanyCamApplication.getInstance(), 15));
            paint.setTextSize(DeviceUtil.dpToPx(mContext, 15)); //TODO

            String[] lines = text.split("\n");

            if (lines.length > 1) {
                canvas.drawText(lines[0],
                        (canvas.getWidth() / 2) - (paint.measureText(lines[0]) / 2),
                        newOrigin.y - padding - doubleLineOffset,
                        paint
                );
                canvas.drawText(lines[1],
                        (canvas.getWidth() / 2) - (paint.measureText(lines[1]) / 2),
                        (newOrigin.y - doubleLineOffset),
                        paint
                );
            } else {
                canvas.drawText(text,
                        (canvas.getWidth() / 2) - (paint.measureText(text) / 2),
                        newOrigin.y - padding,
                        paint
                );
            }

            canvas.restore();
        }
    }

    public void setPhonePosition(int position) {
        phonePosition = position;
    }

    private String setPlaceInfo(Place place) {
        StringBuilder placeDisplay = new StringBuilder();

        // The previous implementation used a Place object from Realm to get the place information for display.  The new React Native
        // implementation passes the name and address of the Place directly from the Javascript app.
        //String placeName = place.getName();
        //String placeAddress = place.getStreetAddress1();

        //quick fix for crash when place is not given a name;
        //should figure out why null and not just empty string in this case
        if(placeName == null){ placeName = ""; }

        if (placeName.equalsIgnoreCase(placeAddress)) {
            placeDisplay.append(placeName);
            placeDisplay.append("\n");
        } else if (placeName.equalsIgnoreCase("Unknown") || placeName.equalsIgnoreCase("Unnamed") ) {
            //don' tdo anything just need city , state, zip
            placeDisplay.append(placeAddress);
            placeDisplay.append("\n");
        } else {
            placeDisplay.append(placeName);
            placeDisplay.append(" ");
            placeDisplay.append("\n");
            placeDisplay.append(placeAddress);
        }

        // The previous implementation also displayed the city, state, and zip from the Place object, but the new React Native implementation
        // doesn't display that information
        /*placeDisplay.append(place.getCity());
        placeDisplay.append(" ");
        placeDisplay.append(place.getState());
        placeDisplay.append(" ");
        placeDisplay.append(place.getZip());*/

        return placeDisplay.toString();
    }
}
