package com.newcam.utils;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

/**
 * Created by dan on 1/26/18.
 */

public class ReadableMapUtils {

    static boolean getBoolOrFalse(ReadableMap map, String name) {
        if (map.hasKey(name) && map.getType(name) == ReadableType.Boolean) {
            return map.getBoolean(name);
        } else {
            return false;
        }
    }

    static ReadableArray getArrayOrNull(ReadableMap map, String name) {
        if (map.hasKey(name) && map.getType(name) == ReadableType.Array) {
            return map.getArray(name);
        } else {
            return null;
        }
    }
}
