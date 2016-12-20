package com.agilx.companycam.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//============================================================================================
// NB: The directories returned by getOutputMediaFile and getPhotoDirectory
// must match 'RNFS.DocumentDirectoryPath' as found in actions/photos uploadRemoteImage
// at time of writing (12/5/16): this.getReactApplicationContext().getFilesDir().getAbsolutePath()
// cf: https://github.com/johanneslumpe/react-native-fs/blob/master/android/src/main/java/com/rnfs/RNFSManager.java
//============================================================================================

/**
 * Created by keaton on 4/18/15.
 */
public class StorageUtility {

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type, null));
    }

    public static File getOutputMediaFile(int type) {
        return getOutputMediaFile(type, null);
    }

    public static File getOutputMediaFile(int type, String directory) {

        //TODO: this is wrong
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        //String fullDirectory = "CompanyCam";
        //if (directory != null && !directory.isEmpty()) {
        //    fullDirectory += File.separator + directory;
        //}
        //File mediaStorageDir = new File(storageDirectory, fullDirectory);

        File mediaStorageDir = new File(storageDirectory, directory);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath(), getNewFileName());
        } else {
            return null;
        }

        System.err.println("[PATHS] getOutputMediaFile output: " + mediaFile.getAbsolutePath());
        return mediaFile;
    }

    public static File getPhotoDirectory() {

        //TODO: this is wrong
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        return new File(dcim, "CompanyCam");
    }

    public static File getTempFile(Context context) {
        File outputDir = context.getCacheDir();
        try {
            //String name = String.format("draw-%s", RandomStringUtils.randomAlphabetic(8));
            String name = String.format("draw-%s", "JFDSJFLJFDS"); //TODO
            return File.createTempFile(name, "png", outputDir);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getNewFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        //return String.format("%s-%s.jpg", timeStamp, RandomStringUtils.randomAlphabetic(16));
        return String.format("%s-%s.jpg", timeStamp, "RUEIWORUERUOW"); //TODO
    }

}
