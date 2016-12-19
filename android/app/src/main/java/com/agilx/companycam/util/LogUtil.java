package com.agilx.companycam.util;

import android.util.Log;

/**
 * Created by keaton on 5/11/15.
 */
public class LogUtil {

    private static final String TAG = "CompanyCamLog";

    private static boolean sLogToCrashlytics = false;

    public static void setLogToCrashlytics(boolean logToCrashlytics) {
        sLogToCrashlytics = logToCrashlytics;
    }

    public static void v(String tag, String message) {
        logInternal(Log.VERBOSE, tag, message);
    }

    public static void v(String message) {
        logInternal(Log.VERBOSE, message);
    }

    public static void d(String tag, String message) {
        logInternal(Log.DEBUG, tag, message);
    }

    public static void d(String message) {
        logInternal(Log.DEBUG, message);
    }

    public static void i(String tag, String message) {
        logInternal(Log.INFO, tag, message);
    }

    public static void i(String message) {
        logInternal(Log.INFO, message);
    }

    public static void w(String tag, String message) {
        logInternal(Log.WARN, tag, message);
    }

    public static void w(String message) {
        logInternal(Log.WARN, message);
    }

    public static void e(String tag, String message) {
        logInternal(Log.ERROR, tag, message);
    }

    public static void e(String message) {
        logInternal(Log.ERROR, message);
    }

    public static void logException(Throwable throwable) {
        if (sLogToCrashlytics) {
            //Crashlytics.logException(throwable); //TODO
        } else {
            e(throwable.getMessage());
        }
    }

    private static void logInternal(int priority, String message) {
        logInternal(priority, TAG, message);
    }

    private static void logInternal(int priority, String tag, String message) {
        if (sLogToCrashlytics) {
            //Crashlytics.log(priority, tag, message); //TODO
        } else {
            Log.println(priority, tag, message);
        }
    }

}
