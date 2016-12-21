package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Image {

    private long localId;

    @SerializedName("id")
    @Expose
    private long imageId;

    @SerializedName("filename")
    @Expose
    private String filename;

    @SerializedName("active")
    @Expose
    private boolean isActive;

    @SerializedName("user_id")
    @Expose
    private long uploadedById;

    @SerializedName("creator")
    @Expose
    private String creator;

    @SerializedName("location_id")
    @Expose
    private long locationId;

    @SerializedName("date_uploaded")
    @Expose
    private Date dateUploaded;

    @SerializedName("lat")
    @Expose
    private float lat;

    @SerializedName("lon")
    @Expose
    private float lon;

    @SerializedName("company_id")
    @Expose
    private long companyId;

    @SerializedName("update_ticks")
    @Expose
    private long updateTicks;

    @SerializedName("image")
    @Expose
    private String image;

    @SerializedName("url_small")
    @Expose
    private String urlSmall;

    @SerializedName("url_medium")
    @Expose
    private String urlMedium;

    @SerializedName("url_large")
    @Expose
    private String urlLarge;

    @SerializedName("tag_ids")
    @Expose
    private String imageTagsAttributes;

    @Expose(serialize = false, deserialize = false)
    public boolean isChecked;

    @SerializedName("placeLocalId")
    @Expose
    private long placeLocalId;

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public long getImageId() {
        return imageId;
    }

    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public Date getDateUploaded() {
        return dateUploaded;
    }

    public void setDateUploaded(Date dateUploaded) {
        this.dateUploaded = dateUploaded;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public long getUpdateTicks() {
        return updateTicks;
    }

    public void setUpdateTicks(long updateTicks) {
        this.updateTicks = updateTicks;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUrlSmall() {
        return urlSmall;
    }

    public void setUrlSmall(String urlSmall) {
        this.urlSmall = urlSmall;
    }

    public String getUrlMedium() {
        return urlMedium;
    }

    public void setUrlMedium(String urlMedium) {
        this.urlMedium = urlMedium;
    }

    public String getUrlLarge() {
        return urlLarge;
    }

    public void setUrlLarge(String urlLarge) {
        this.urlLarge = urlLarge;
    }

    public String getImageTagsAttributes() { return imageTagsAttributes; }

    public void setImageTagsAttributes(String imageTagsAttributes) {
        this.imageTagsAttributes = imageTagsAttributes;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public long getPlaceLocalId() { return placeLocalId; }

    public void setPlaceLocalId(long placeLocalId) {this.placeLocalId = placeLocalId; }


    public static class Comment {
        @SerializedName("comment")
        @Expose
        private String comment;

        @SerializedName("date_created")
        @Expose
        private Date dateCreated;

        @SerializedName("user_id")
        @Expose
        private int userId;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(Date dateCreated) {
            this.dateCreated = dateCreated;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }

    public Image() {}

    public Image(Image i) {
        this.localId = i.getLocalId();
        this.imageId = i.getImageId();
        this.filename = i.getFilename();
        this.isActive = i.isActive();
        this.uploadedById = i.getUploadedById();
        this.creator = i.getCreator();
        this.locationId = i.getLocationId();
        this.dateUploaded = i.getDateUploaded();
        this.lat = i.getLat();
        this.lon = i.getLon();
        this.companyId = i.getCompanyId();
        this.updateTicks = i.getUpdateTicks();
        this.image = i.getImage();
        this.urlLarge = i.getUrlLarge();
        this.urlMedium = i.getUrlMedium();
        this.urlSmall = i.getUrlSmall();
        this.imageTagsAttributes = i.getImageTagsAttributes();
        this.placeLocalId = i.getPlaceLocalId();
    }
}
