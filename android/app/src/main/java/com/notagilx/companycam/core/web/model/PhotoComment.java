package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by keaton on 5/7/15.
 */
public class PhotoComment {

    @SerializedName("id")
    @Expose
    private long photoCommentId;

    @SerializedName("image_id")
    @Expose
    private long photoId;

    @SerializedName("comment")
    @Expose
    private String commentText;

    @SerializedName("user")
    @Expose
    private User createdBy;

    @SerializedName("date_created")
    @Expose
    private Date dateCreated;

    @SerializedName("user_id")
    @Expose
    private long userId;

    @SerializedName("active")
    @Expose
    private boolean isActive;

    public long getPhotoCommentId() {
        return photoCommentId;
    }

    public void setPhotoCommentId(long photoCommentId) {
        this.photoCommentId = photoCommentId;
    }

    public long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(long mPhotoId) {
        this.photoId = mPhotoId;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String mCommentText) {
        this.commentText = mCommentText;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long mUserId) {
        this.userId = mUserId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User user) {
        createdBy = user;
    }
}
