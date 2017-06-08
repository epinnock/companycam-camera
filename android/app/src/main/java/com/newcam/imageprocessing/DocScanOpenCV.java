package com.newcam.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.View;

import com.newcam.imageprocessing.utils.BoofLogUtil;
import com.newcam.imageprocessing.utils.DocScanUtil;
import com.newcam.imageprocessing.utils.PerspectiveRect;

import java.util.List;

import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F32;

/**
 * Created by dan on 5/1/17.
 */

public class DocScanOpenCV extends View implements CCCameraImageProcessor {

    //----------------------------------------------
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();
    //----------------------------------------------


    protected Context context;

    public DocScanOpenCV(Context context) {
        super(context);
        this.context = context;

        this.setBackgroundColor(Color.argb(0,0,0,0));
    }

    @Override
    public void setListener(ImageProcessorListener listener) {

    }

    @Override
    public void setImageParams(int widthOrig, int heightOrig, int widthContainer, int heightContainer, int MAX_OUTPUT_DIM) {

    }

    @Override
    public boolean setPreviewBytes(byte[] data, int rotation) {
        System.err.println("[CCAM DocScan] " + stringFromJNI());
        return false;
    }

    @Override
    public void clearVisiblePreview() {

    }

    @Override
    public Bitmap getOutputImage() {
        return null;
    }
}
