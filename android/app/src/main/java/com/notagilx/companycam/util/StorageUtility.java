package com.notagilx.companycam.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by keaton on 4/18/15.
 */
public class StorageUtility {

    public static String getNewFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        //return String.format("%s-%s.jpg", timeStamp, RandomStringUtils.randomAlphabetic(16));
        return String.format("%s-%s.jpg", timeStamp, "RUEIWORUERUOW"); //TODO
    }

}
