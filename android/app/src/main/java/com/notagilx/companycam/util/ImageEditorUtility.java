package com.notagilx.companycam.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by keaton on 4/28/15.
 */
public class ImageEditorUtility {

    private static final String TAG = "ImageEditorUtility";
    private static final String OOME_STRING = "Out of memory";

    private static Bitmap overlayBitmaps(Bitmap b1, Bitmap b2) {
        Bitmap overlay = Bitmap.createBitmap(b1.getWidth(), b1.getHeight(), b1.getConfig());
        Canvas canvas = new Canvas(overlay);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(b1, 0, 0, paint);
        Bitmap b2Scaled = Bitmap.createScaledBitmap(b2, b1.getWidth(), b1.getHeight(), true);
        canvas.drawBitmap(b2Scaled, 0, 0, paint);
        return overlay;
    }

    public static File processImageWithEdit(File image, int totalRotationDegrees, Bitmap edits) {

        if (edits != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap imageBitmap = BitmapFactory.decodeFile(image.getPath(), options);

            Bitmap scaledEdits = null;
            Bitmap finalImage = null;

            try {
                //rotate original image if necessary
                Bitmap imageBitmapRotated = imageBitmap;
                if(totalRotationDegrees != 0){
                    Matrix matrix = new Matrix();
                    matrix.postRotate(totalRotationDegrees);
                    imageBitmapRotated = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
                }

                //scale edits to match image taken with camera
                //-----------------------------------------------
                //NOTE: assuming inkview and rotated image are centered on screen, and both fit so
                //their widths fill with the screen (i.e., fails if one fills vertically instead)

                //if 'edits' is scaled to match the size of 'imageBitmapRotated', how much excess height?
                float scale = (float)imageBitmapRotated.getWidth() / (float)edits.getWidth();
                int editsRescaleH = (int)( scale * (float)edits.getHeight() );
                int excessH = editsRescaleH - imageBitmapRotated.getHeight();

                //crop the excess height (cropY can be determined with algebra)
                Bitmap croppedEdits = edits;
                if(excessH > 0){
                    int cropY = (int)(0.5f * (float)excessH / scale);
                    croppedEdits = Bitmap.createBitmap(edits, 0, cropY, edits.getWidth(), edits.getHeight() - 2*cropY);
                }

                //finally, scale edits to match size of rotated image
                scaledEdits = getResizedBitmap(croppedEdits, imageBitmapRotated.getWidth(), imageBitmapRotated.getHeight());
                //-----------------------------------------------

                finalImage = overlayBitmaps(imageBitmapRotated, scaledEdits);

                FileOutputStream out = new FileOutputStream(image);
                finalImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            } catch (OutOfMemoryError oome) {
                oome.printStackTrace();
                //TODO log this
                //EventBus.getDefault().post(new OutOfMemoryEvent(OOME_STRING));
            } finally {
                if (edits != null) {
                    edits.recycle();
                    edits = null;
                }
                if (imageBitmap != null) {
                    imageBitmap.recycle();
                    imageBitmap = null;
                }
                if (scaledEdits != null) {
                    scaledEdits.recycle();
                    scaledEdits = null;
                }
                if (finalImage != null) {
                    finalImage.recycle();
                    finalImage = null;
                }
            }
        }
        return image;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    //read photo, resize according to MAX_WIDTH/MAX_HEIGHT, rotate according to EXIF data
    public static Bitmap resizeAndRotatePhoto(String photoPath){

        int MAX_WIDTH = 1920;
        int MAX_HEIGHT = 1080;

        //get orientation from EXIF (will use later to rotate when re-encoding)
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        int totalRotationDegrees = 0;
        try{
            ExifInterface exif = new ExifInterface(photoPath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  totalRotationDegrees = 90;  break;
                case ExifInterface.ORIENTATION_ROTATE_180: totalRotationDegrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: totalRotationDegrees = 270; break;
            }
        }
        catch(IOException ioe){
            LogUtil.e(TAG, "Failed on ExifInterface");
            return null;
        }

        //get photo size only; swap max dims if in portrait
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bitmapOptions);

        int bitmapRawWidth = bitmapOptions.outWidth;
        int bitmapRawHeight = bitmapOptions.outHeight;

        boolean swapMaxDims = (bitmapRawHeight > bitmapRawWidth);
        if(swapMaxDims){
          int temp = MAX_WIDTH;
          MAX_WIDTH = MAX_HEIGHT;
          MAX_HEIGHT = temp;
        }

        //see if we can use inSampleSize
        float rawScaleH = (float)MAX_HEIGHT / (float)bitmapRawHeight;
        float rawScaleW = (float)MAX_WIDTH / (float)bitmapRawWidth;
        float rawScale = (rawScaleH < rawScaleW) ? rawScaleH : rawScaleW;

        int sampleSize = 1;
        if(rawScale <= 0.5){ sampleSize = 2; }
        if(rawScale <= 0.25){ sampleSize = 4; }
        if(rawScale <= 0.125){ sampleSize = 8; }

        LogUtil.i(TAG, "Full size: " + bitmapRawWidth + "x" + bitmapRawHeight + "; sample size: " + sampleSize);

        //load into memory
        bitmapOptions.inJustDecodeBounds = false;
        bitmapOptions.inSampleSize = sampleSize;
        Bitmap bPhoto = BitmapFactory.decodeFile(photoPath, bitmapOptions);

        if(bPhoto == null){
            LogUtil.e(TAG, "Failed on BitmapFactory.decodeFile");
            return null;
        }

        int bitmapWidth = bitmapOptions.outWidth;
        int bitmapHeight = bitmapOptions.outHeight;

        LogUtil.i(TAG, "Memory size: " + bitmapWidth + "x" + bitmapHeight);

        //compute new dimensions
        int bitmapNewWidth = bitmapWidth;
        int bitmapNewHeight = bitmapHeight;

        if((bitmapWidth > MAX_WIDTH) || (bitmapHeight > MAX_HEIGHT)){
            float scaleH = (float)MAX_HEIGHT / (float)bitmapHeight;
            float scaleW = (float)MAX_WIDTH / (float)bitmapWidth;
            float scale = (scaleH < scaleW) ? scaleH : scaleW;

            if(scale < 1.0){ //should always be true
                bitmapNewWidth = (int)(scale * (float)bitmapWidth);
                bitmapNewHeight = (int)(scale * (float)bitmapHeight);
            }
        }

        //resize and rotate if EXIF says so
        Bitmap bPhotoFinal = null;
        try{
            Matrix matrix = new Matrix();
            matrix.postRotate(totalRotationDegrees);

            Bitmap bPhotoResized = Bitmap.createScaledBitmap(bPhoto, bitmapNewWidth, bitmapNewHeight, true);
            Bitmap bPhotoRotated = Bitmap.createBitmap(bPhotoResized, 0, 0, bitmapNewWidth, bitmapNewHeight, matrix, true);

            bPhotoFinal = bPhotoRotated;
        }
        catch(OutOfMemoryError oome){
            LogUtil.e(TAG, "Failed: Out of memory error (resize)");
            return null;
        }

        //all done
        return bPhotoFinal;
    }
}
