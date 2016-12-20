package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * Created by dsveen on 3/28/16.
 */
public class Geofence extends RealmObject {

    @SerializedName("lat")
    @Expose
    private double geofenceLat;

    @SerializedName("lon")
    @Expose
    private double geofenceLon;

    @SerializedName("order")
    @Expose
    private long geofenceOrder;

    public double getGeofenceLat() { return geofenceLat; }

    public void setGeofenceLat(double geofenceLat) { this.geofenceLat = geofenceLat; }

    public double getGeofenceLon() { return geofenceLon; }

    public void setGeofenceLon(double geofenceLon) { this.geofenceLon = geofenceLon; }

    public long getGeofenceOrder() { return geofenceOrder; }

    public void setGeofenceOrder(long geofenceOrder) { this.geofenceOrder = geofenceOrder; }

}
