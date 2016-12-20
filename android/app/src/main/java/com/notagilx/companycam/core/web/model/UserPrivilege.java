package com.notagilx.companycam.core.web.model;

import io.realm.RealmObject;

/**
 * Created by keaton on 7/29/15.
 */
public class UserPrivilege extends RealmObject {

    private int privilege;

    public int getPrivilege() {
        return privilege;
    }

    public void setPrivilege(int privilege) {
        this.privilege = privilege;
    }

}
