package com.newcam;

/**
 * Created by dan on 12/18/17.
 */

public enum ResolutionMode {
    NORMAL(0, "normal"),
    HIGH(1, "high"),
    SUPER(2, "super");

    private final int intValue;
    private final String stringValue;

    ResolutionMode(int intValue, String stringValue) {
        this.intValue = intValue;
        this.stringValue = stringValue;
    }

    public int toInt() { return intValue; }

    @Override
    public String toString() { return stringValue; }

    public static ResolutionMode fromInt(int intValue) {
        for(ResolutionMode mode : ResolutionMode.values()) {
            if (mode.toInt() == intValue) { return mode; }
        }
        return ResolutionMode.NORMAL;
    }

    public static ResolutionMode fromString(String stringValue) {
        for(ResolutionMode mode : ResolutionMode.values()) {
            if (mode.toString().equals(stringValue)) { return mode; }
        }
        return ResolutionMode.NORMAL;
    }
}
