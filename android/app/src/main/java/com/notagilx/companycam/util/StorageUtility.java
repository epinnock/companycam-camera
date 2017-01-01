package com.notagilx.companycam.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.parceler.apache.commons.lang.RandomStringUtils;

/**
 * Created by keaton on 4/18/15.
 */
public class StorageUtility {

    public static String getNewFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return String.format("%s-%s.jpg", timeStamp, RandomStringUtils.randomAlphabetic(16));
    }

}
