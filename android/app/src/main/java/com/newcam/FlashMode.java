package com.newcam;

/**
 * Created by dan on 12/18/17.
 */

public enum FlashMode {
    OFF(0, "off"),
    ON(1, "on"),
    AUTO(2, "auto"),
    TORCH(3, "torch");

    private final int intValue;
    private final String stringValue;

    FlashMode(int intValue, String stringValue) {
        this.intValue = intValue;
        this.stringValue = stringValue;
    }

    public int toInt() { return intValue; }

    @Override
    public String toString() { return stringValue; }

    public static FlashMode fromInt(int intValue) {
        for(FlashMode mode : FlashMode.values()) {
            if (mode.toInt() == intValue) { return mode; }
        }
        return FlashMode.OFF;
    }

    public static FlashMode fromString(String stringValue) {
        for(FlashMode mode : FlashMode.values()) {
            if (mode.toString().equals(stringValue)) { return mode; }
        }
        return FlashMode.OFF;
    }
}
