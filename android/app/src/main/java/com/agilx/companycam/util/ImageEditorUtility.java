package com.agilx.companycam.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;

import com.agilx.companycam.core.events.OutOfMemoryEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.greenrobot.event.EventBus;

/**
 * Created by keaton on 4/28/15.
 */
public class ImageEditorUtility {

    private static final String TAG = "ImageEditorUtility";
    private static final String OOME_STRING = "Out of memory";

    public static File processPhotoWithEdits(final File imageFile, int rotationDegrees, Bitmap drawBitmap) {
        return processPhotoWithEdits(imageFile, rotationDegrees, drawBitmap, 0);
    }

    private static File processPhotoWithEdits(final File imageFile, int rotationDegrees, Bitmap drawBitmap, int numTries) {
        LogUtil.d(TAG, "processing photo " + imageFile.getName()
                           + ", rotationDegrees = " + rotationDegrees
                           + ", hasDrawing = " + (drawBitmap != null)
                           + ", try # = " + numTries);

        // 200 KB
        int TARGET_IMAGE_SIZE = 200 * 1024;
        // either width or height must be at or below this
        int MAX_SMALLER_IMAGE_DIMENSION = 1280;

        LogUtil.i(TAG, "[1] Check photo size");
        Bitmap imageBitmap = null;
        BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
        bmpOptions.inSampleSize = 1;
        boolean success = false;
        while (!success && bmpOptions.inSampleSize <= 16) {
            try {
                imageBitmap = BitmapFactory.decodeFile(imageFile.getPath(), bmpOptions);
                if (imageBitmap.getWidth() > MAX_SMALLER_IMAGE_DIMENSION && imageBitmap.getHeight() > MAX_SMALLER_IMAGE_DIMENSION) {
                    bmpOptions.inSampleSize *= 2;
                    String logMessage = String.format("Image dimensions (%d x %d) are too large. Downsampling by half (inSampleSize = %d)",
                                                         imageBitmap.getWidth(),
                                                         imageBitmap.getHeight(),
                                                         bmpOptions.inSampleSize);
                    LogUtil.d(TAG, logMessage);
                } else {
                    success = true;
                }
            } catch (OutOfMemoryError e) {
                LogUtil.e(TAG, "Memory error trying to decode file: " + e);
                bmpOptions.inSampleSize *= 2;
                String logMessage = String.format("Downsampling by half (inSampleSize = %d)", bmpOptions.inSampleSize);

                LogUtil.d(TAG, logMessage);
            }
        }

        if (imageBitmap == null) {
            LogUtil.e(TAG, "imageBitmap was not downsized correctly");
            if (numTries < 10) {
                numTries++;
                LogUtil.d("trying again (numTries = " + numTries + ")");
                return processPhotoWithEdits(imageFile, rotationDegrees, drawBitmap, numTries);
            }
            return null;
        }

        LogUtil.i(TAG, "[2] Rotate photo");
        Bitmap imageBitmapRotated;
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            int totalRotationDegrees = (getRotationCorrection(orientation) + rotationDegrees) % 360;
            Matrix matrix = new Matrix();
            matrix.postRotate(totalRotationDegrees);
            imageBitmapRotated = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            LogUtil.e(TAG, String.format("Error rotating photo: %s", e.getMessage()));
            if (numTries < 10) {
                numTries++;
                LogUtil.d("trying again (numTries = " + numTries + ")");
                return processPhotoWithEdits(imageFile, rotationDegrees, drawBitmap, numTries);
            }
            return null;
        } catch (OutOfMemoryError e) {
            LogUtil.e(TAG, "Memory error trying to rotate photo: " + e);
            System.gc();
            return null;
        }

        LogUtil.i(TAG, "[3] Apply drawing if exists and compress");
        final File editedFile = StorageUtility.getOutputMediaFile(StorageUtility.MEDIA_TYPE_IMAGE, "Edits");
        try {
            Bitmap finalBitmap = drawBitmap == null ? imageBitmapRotated : overlayBitmaps(imageBitmapRotated, drawBitmap);
            int compressQuality = 45;
            do {
                // If file is not deleted, compress will add to existing file size.
                editedFile.delete();
                FileOutputStream editedFOut = new FileOutputStream(editedFile);
                LogUtil.d(TAG, "Attempting to compress photo with quality " + compressQuality);
                //finalBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, editedFOut);
                LogUtil.d(TAG, "New file size (KB): " + editedFile.length() / (float) 1024);
                compressQuality -= 5;
                editedFOut.flush();
                editedFOut.close();
            } while (editedFile.length() >= TARGET_IMAGE_SIZE);
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to overlay and compress drawing: " + e);
            if (numTries < 10) {
                numTries++;
                LogUtil.d("trying again (numTries = " + numTries + ")");
                return processPhotoWithEdits(imageFile, rotationDegrees, drawBitmap, numTries);
            }
            return null;
        } catch (OutOfMemoryError e) {
            LogUtil.e(TAG, "Memory error trying Apply drawing if exists and compress: " + e);
            System.gc();
            return null;
        }

        return editedFile;
    }

    public static File processPhotoWithEditsOld(final File imageFile, int rotationDegrees, Bitmap drawBitmap, boolean isNew, Context context) {
        final File editedFile = StorageUtility.getOutputMediaFile(StorageUtility.MEDIA_TYPE_IMAGE, "Edits");

        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            int totalRotationDegrees = (getRotationCorrection(orientation) + rotationDegrees) % 360;

            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = false;
            bmpFactoryOptions.inSampleSize = 2;
            Bitmap bmImage = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmpFactoryOptions);
            Matrix matrix = new Matrix();
            matrix.postRotate(totalRotationDegrees);
            Bitmap bmImageTransformed = Bitmap.createBitmap(bmImage, 0, 0, bmImage.getWidth(), bmImage.getHeight(), matrix, true);
            if (drawBitmap != null) {
                File drawFile = StorageUtility.getTempFile(context);
                if (drawFile != null) {
                    OutputStream drawFOut = new FileOutputStream(drawFile);
                    drawBitmap.compress(Bitmap.CompressFormat.PNG, 100, drawFOut);
                    drawFOut.flush();
                    drawFOut.close();
                    boolean success = false;
                    bmpFactoryOptions.inSampleSize = 1;
                    while (!success && bmpFactoryOptions.inSampleSize <= 16) {
                        try {
                            drawBitmap = BitmapFactory.decodeFile(drawFile.getAbsolutePath(), bmpFactoryOptions);
                            bmImageTransformed = overlayBitmaps(bmImageTransformed, drawBitmap);
                            success = true;
                        } catch (OutOfMemoryError e) {
                            LogUtil.logException(e);
                            bmpFactoryOptions.inSampleSize *= 2;
                        }
                    }
                    drawFile.delete();
                    if (!success) {
                        LogUtil.e(TAG, "Failed to overlay drawing after attempting downscaling");
                    }
                }
            }
            FileOutputStream fOut = new FileOutputStream(editedFile);
            bmImageTransformed.compress(Bitmap.CompressFormat.JPEG, isNew ? 80 : 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            LogUtil.e(TAG, String.format("Error saving edited photo: %s", e.getMessage()));
            return null;
        }

        return editedFile;
    }

    private static int getRotationCorrection(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

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
                EventBus.getDefault()
                        .post(new OutOfMemoryEvent(OOME_STRING));
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
