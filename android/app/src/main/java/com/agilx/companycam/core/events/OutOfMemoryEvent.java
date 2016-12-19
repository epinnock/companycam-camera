package com.agilx.companycam.core.events;

/**
 * Created by Landon on 12/17/15.
 */
public class OutOfMemoryEvent {

    private String message;

    public OutOfMemoryEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
