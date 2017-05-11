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

    private Bitmap image;
    private GrayU8 imageU8;
    private DrawingUtil drawutil;

    private byte[] workBuffer;

    public DrawableU8Android(Bitmap image){
        this.image = image;

        Canvas canvas = new Canvas(image);
        drawutil = new DrawingUtilAndroid(canvas);

        workBuffer = ConvertBitmap.declareStorage(image, null);
        imageU8 = ConvertBitmap.bitmapToGray(image, (GrayU8)null, null);
    }

    public void clearBitmap(int color){
        image.eraseColor(color);
    }

    public DrawingUtil getDrawingUtil(){
        return drawutil;
    }

    public GrayU8 getGrayU8(){
        ConvertBitmap.bitmapToGray(image, imageU8, workBuffer);
        return imageU8;
    }
}
