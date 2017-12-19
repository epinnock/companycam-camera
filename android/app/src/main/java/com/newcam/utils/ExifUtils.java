package com.newcam.utils;

import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;

import com.newcam.enums.FlashMode;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by dan on 1/5/17.
 */

public class ExifUtils {

    public static void setAttributes(File imageFile, Location exifLoc, FlashMode flashMode) throws IOException {

        ExifInterface exif = new ExifInterface(imageFile.getPath());

        //TODO: GPS location+timetamp: Maybe use Location.convert instead?
        //------------------------------------------------
        double latitude = Math.abs(exifLoc.getLatitude());
        double longitude = Math.abs(exifLoc.getLongitude());

        int num1Lat = (int)Math.floor(latitude);
        int num2Lat = (int)Math.floor((latitude - num1Lat) * 60);
        int num3Lat = (int)( (latitude - ((double)num1Lat+((double)num2Lat/60))) * 3600000 );

        int num1Lon = (int)Math.floor(longitude);
        int num2Lon = (int)Math.floor((longitude - num1Lon) * 60);
        int num3Lon = (int)( (longitude - ((double)num1Lon+((double)num2Lon/60))) * 3600000 );

        String exifLatStr = num1Lat+"/1,"+num2Lat+"/1,"+num3Lat+"/1000";
        String exifLatRef = (exifLoc.getLatitude() > 0) ? "N" : "S";
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifLatStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifLatRef);

        String exifLonStr = num1Lon+"/1,"+num2Lon+"/1,"+num3Lon+"/1000";
        String exifLonRef = (exifLoc.getLongitude() > 0) ? "E" : "W";
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifLonStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifLonRef);

        System.out.println("Set EXIF location coords: [" + exifLatStr + "][" + exifLatRef + "] [" + exifLonStr + "][" + exifLonRef + "]");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(exifLoc.getTime());
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        String exifGPSTimestamp = hourOfDay + "/1," + minutes + "/1," + seconds + "/1";
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exifGPSTimestamp);

        System.out.println("Set EXIF location timestamp: [" + exifGPSTimestamp + "]");
        //------------------------------------------------

        exif.setAttribute(ExifInterface.TAG_FLASH, flashMode.toString());
        exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL);
        exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER);

        exif.saveAttributes();
    }
}
