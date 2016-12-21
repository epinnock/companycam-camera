package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by keaton on 3/30/15.
 */
public class Place {

    private long localId;

    @SerializedName("id")
    @Expose
    private long locationId;

    @SerializedName("place_id")
    @Expose
    private String placeId;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("street_address_1")
    @Expose
    private String streetAddress1;

    @SerializedName("street_address_2")
    @Expose
    private String streetAddress2;

    @SerializedName("city")
    @Expose
    private String city;

    @SerializedName("state")
    @Expose
    private String state;

    @SerializedName("postal_code")
    @Expose
    private String zip;

    @SerializedName("company_id")
    @Expose
    private long companyId;

    @SerializedName("date_created")
    @Expose
    private Date dateCreated;

    @SerializedName("updated_date")
    @Expose
    private Date updatedDate;

    @SerializedName("user_id")
    @Expose
    private long userId;

    @SerializedName("lat")
    @Expose
    private double lat;

    @SerializedName("lon")
    @Expose
    private double lon;

    @SerializedName("active")
    @Expose
    private boolean isActive;

    @SerializedName("feature_photo_url")
    @Expose
    private String featurePhotoUrl;

    @SerializedName("feature_photo_medium_url")
    @Expose
    private String featurePhotoMediumUrl;

    @SerializedName("feature_photo_small_url")
    @Expose
    private String featurePhotoSmallUrl;

    @SerializedName("country")
    @Expose
    private String country;

    @SerializedName("north")
    @Expose
    private double north;

    @SerializedName("south")
    @Expose
    private double south;

    @SerializedName("east")
    @Expose
    private double east;

    @SerializedName("west")
    @Expose
    private double west;

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreetAddress1() {
        return streetAddress1;
    }

    public void setStreetAddress1(String streetAddress1) {
        this.streetAddress1 = streetAddress1;
    }

    public String getStreetAddress2() {
        return streetAddress2;
    }

    public void setStreetAddress2(String streetAddress2) {
        this.streetAddress2 = streetAddress2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getFeaturePhotoUrl() {
        return featurePhotoUrl;
    }

    public void setFeaturePhotoUrl(String featurePhotoUrl) {
        this.featurePhotoUrl = featurePhotoUrl;
    }

    public String getFeaturePhotoMediumUrl() {
        return featurePhotoMediumUrl;
    }

    public void setFeaturePhotoMediumUrl(String featurePhotoMediumUrl) {
        this.featurePhotoMediumUrl = featurePhotoMediumUrl;
    }

    public String getFeaturePhotoSmallUrl() {
        return featurePhotoSmallUrl;
    }

    public void setFeaturePhotoSmallUrl(String featurePhotoSmallUrl) {
        this.featurePhotoSmallUrl = featurePhotoSmallUrl;
    }

    public String getCountry() { return country;}

    public void setCountry(String country) { this.country = country; }

    public double getNorth() { return north; }

    public void setNorth(double north) { this.north = north; }

    public double getSouth() { return south; }

    public void setSouth(double south) { this.south = south; }

    public double getEast() { return east; }

    public void setEast(double east) { this.east = east; }

    public double getWest() { return west; }

    public void setWest(double west) { this.west = west; }

    public Place () {}

    public Place(Place p) {
        localId = p.getLocalId();
        locationId = p.getLocationId();
        placeId = p.getPlaceId();
        name = p.getName();
        streetAddress1 = p.getStreetAddress1();
        streetAddress2 = p.getStreetAddress2();
        city = p.getCity();
        state = p.getState();
        zip = p.getZip();
        companyId = p.getCompanyId();
        dateCreated = p.getDateCreated();
        updatedDate = p.getUpdatedDate();
        userId = p.getUserId();
        lat = p.getLat();
        lon = p.getLon();
        isActive = p.isActive();
        featurePhotoUrl = p.getFeaturePhotoUrl();
        featurePhotoMediumUrl = p.getFeaturePhotoMediumUrl();
        featurePhotoSmallUrl = p.getFeaturePhotoSmallUrl();
        country = p.getCountry();
        north = p.getNorth();
        south = p.getSouth();
        east = p.getEast();
        west = p.getWest();
    }


}
