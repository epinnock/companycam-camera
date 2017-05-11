package com.newcam.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.newcam.imageprocessing.utils.DrawableU8;
import com.newcam.imageprocessing.utils.DrawingUtil;

import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;

/**
 * Created by dan on 5/10/17.
 */

public class DrawableU8Android implements DrawableU8 {

    private Bitmap bitmap;
    private DrawingUtil drawutil;

    public DrawableU8Android(int width, int height){
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawutil = new DrawingUtilAndroid(bitmap);
    }

    public DrawingUtil getDrawingUtil(){
        return drawutil;
    }

    public GrayU8 getGrayU8(){
        return ConvertBitmap.bitmapToGray(bitmap, (GrayU8)null, null);
    }
}
