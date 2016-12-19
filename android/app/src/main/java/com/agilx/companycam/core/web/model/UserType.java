package com.agilx.companycam.core.web.model;

import io.realm.RealmObject;

/**
 * Created by keaton on 7/29/15.
 */
public class UserType extends RealmObject {

    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
