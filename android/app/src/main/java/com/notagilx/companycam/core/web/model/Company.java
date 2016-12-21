package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by keaton on 3/28/15.
 */
public class Company implements Serializable {

    @SerializedName("id")
    @Expose
    private long companyId;

    @SerializedName("Name")
    @Expose
    private String name;

    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("api_key")
    @Expose
    private String apiKey;

    @SerializedName("active")
    @Expose
    private boolean isActive;

    @SerializedName("ModifiedSince")
    @Expose
    private String modifiedSince;

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getModifiedSince() {
        return modifiedSince;
    }

    public void setModifiedSince(String modifiedSince) {
        this.modifiedSince = modifiedSince;
    }

    public Company() {}

    public Company(Company c) {
        companyId = c.getCompanyId();
        name = c.getName();
        email = c.getEmail();
        apiKey = c.getApiKey();
        isActive = c.isActive();
        modifiedSince = c.getModifiedSince();
    }
}
