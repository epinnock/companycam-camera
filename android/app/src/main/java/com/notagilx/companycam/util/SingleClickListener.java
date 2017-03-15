package com.notagilx.companycam.util;

import android.view.View;

/**
 * Created by mattboyd on 12/1/16.
 */

// This is a convenience class that can be used as a standard click listener that will ignore rapid clicks in succession and simply
// return one onSingleClick callback
public abstract class SingleClickListener implements View.OnClickListener {

    // The minClickInterval is the minimum amount of time, in milliseconds, that must elapse before the click on the view should be recognized.
    public long minClickInterval;

    // The lastClickTime is the last time that a click event was received.
    public long lastClickTime = 0;

    public SingleClickListener(long minClickInterval) {
        this.minClickInterval = minClickInterval;
    }

    // This method will be overridden to provide the true click action
    public abstract void onSingleClick(View v);

    @Override
    public void onClick(View v) {

        // Get the current time
        long lastClick = lastClickTime;
        long now = System.currentTimeMillis();

        // Check if enough time has elapsed since the last click was received
        if (lastClick == 0 || (now - lastClick > minClickInterval)) {

            // If enough time has elapsed then this click can actually be processed.
            lastClickTime = now;
            onSingleClick(v);
        }
        else {
            // If not enough time has elapsed since the last touch, then do nothing.
        }
    }
}
