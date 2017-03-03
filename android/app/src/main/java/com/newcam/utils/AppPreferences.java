package com.newcam.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by dan on 3/2/17.
 */

public class AppPreferences {

    private static final String APP_PACKAGE ="com.agilx.companycam";

    private static final String PREFS_FORCE_CAM1 = "PREFS_FORCE_CAM1";

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
    }

    public static boolean getForceCamera1(Context context){
        SharedPreferences preferences = AppPreferences.getSharedPreferences(context);
        return preferences.getBoolean(PREFS_FORCE_CAM1, false);
    }

    public static void setForceCamera1(Context context, boolean val){
        SharedPreferences preferences = AppPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREFS_FORCE_CAM1, val);
        editor.apply();
    }
}
