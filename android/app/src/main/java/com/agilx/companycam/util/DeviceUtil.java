package com.agilx.companycam.util;

import android.content.Context;

/**
 * Created by keaton on 5/27/15.
 */
public class DeviceUtil {

    public static float pxToDp(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float dpToPx(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

}
