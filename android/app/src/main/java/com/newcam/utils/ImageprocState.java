package com.newcam.utils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.LinkedList;

/**
 * Created by dan on 1/26/18.
 */

public class ImageprocState {

    public boolean magicColor = false; // Optional
    public boolean fourPointApplied = false; // Optional
    public float[] pRect = new float[8]; // Required iff fourPointApplied

    public void debugPrint(String tag) {
        System.out.println(tag + " ImageprocState:");
        System.out.println(tag + " - magicColor enabled?: " + (magicColor ? "Yes" : "No") + "");
        System.out.println(tag + " - fourPoint enabled?: " + (fourPointApplied ? "Yes" : "No") + "");
        if (fourPointApplied) {
            System.out.println(tag + " - fourPoint locations: " +
                    "(" + pRect[0] + ", " + pRect[1] + "), " +
                    "(" + pRect[2] + ", " + pRect[3] + "), " +
                    "(" + pRect[4] + ", " + pRect[5] + "), " +
                    "(" + pRect[6] + ", " + pRect[7] + ")");
        }
    }

    /** Write fields to a WritableMap */
    public WritableMap toWritableMap() {
        WritableMap data = Arguments.createMap();

        data.putBoolean("magicColor", magicColor);
        data.putBoolean("fourPointApplied", fourPointApplied);
        if (fourPointApplied) {
            WritableArray fourPointLocations = Arguments.createArray();
            for (int k=0; k<8; k++) {
                fourPointLocations.pushDouble(pRect[k]);
            }
        }

        return data;
    }

    /** Set fields from a ReadableMap.  Throws exception if invalid data */
    public void loadReadableMap(ReadableMap data) throws IllegalArgumentException {

        if (data.hasKey("magicColor")) {
            magicColor = data.getBoolean("magicColor");
        } else {
            magicColor = false;
        }

        if (data.hasKey("fourPointApplied")) {
            fourPointApplied = data.getBoolean("fourPointApplied");

            if (data.hasKey("fourPointLocations")) {
                // Points are read in order from 'fourPointLocations', which is required if
                // fourPointApplied == true. Values are in image coordinates, normalized to [0,1]^2
                ReadableArray fourPointLocations = data.getArray("fourPointLocations");
                if (fourPointLocations.size() == 8) {
                    for (int k=0; k<8; k++) {
                        pRect[k] = (float)fourPointLocations.getDouble(k);
                    }
                } else {
                    throw new IllegalArgumentException("Field 'fourPointLocations' must be [x1, y1, ..., x4, y4]");
                }
            } else {
                throw new IllegalArgumentException("Missing field 'fourPointLocations'");
            }
        } else {
            fourPointApplied = false;
        }
    }
}
