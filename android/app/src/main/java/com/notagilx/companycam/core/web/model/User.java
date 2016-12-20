package com.notagilx.companycam.core.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by keaton on 3/28/15.
 */
public class User extends RealmObject {

    @PrimaryKey
    @SerializedName("id")
    @Expose
    private long userId;

    @SerializedName("api_key")
    @Expose
    private String apiKey;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("email_address")
    @Expose
    private String emailAddress;

    @SerializedName("first_name")
    @Expose
    private String firstName;

    @SerializedName("last_name")
    @Expose
    private String lastName;

    @SerializedName("IsActive")
    @Expose
    private boolean isActive;

    @SerializedName("company")
    @Expose
    private Company company;

    @Expose(serialize = false, deserialize = false)
    private boolean isLoggedIn;

    @SerializedName("FullSizeProfileUrl")
    @Expose
    private String fullSizeProfileUrl;

    @SerializedName("WebSizeProfileUrl")
    @Expose
    private String webSizeProfileUrl;

    @SerializedName("profile_url_mobile")
    @Expose
    private String mobileSizeProfileUrl;

    @SerializedName("Roles")
    @Expose
    @Ignore
    private int[] roleValues;

    private RealmList<UserPrivilege> roles;

    @SerializedName("user_type")
    @Expose
    private int userTypeValue;

    private UserType userType;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public String getFullSizeProfileUrl() {
        return fullSizeProfileUrl;
    }

    public void setFullSizeProfileUrl(String fullSizeProfileUrl) {
        this.fullSizeProfileUrl = fullSizeProfileUrl;
    }

    public String getWebSizeProfileUrl() {
        return webSizeProfileUrl;
    }

    public void setWebSizeProfileUrl(String webSizeProfileUrl) {
        this.webSizeProfileUrl = webSizeProfileUrl;
    }

    public String getMobileSizeProfileUrl() {
        return mobileSizeProfileUrl;
    }

    public void setMobileSizeProfileUrl(String mobileSizeProfileUrl) {
        this.mobileSizeProfileUrl = mobileSizeProfileUrl;
    }

    public RealmList<UserPrivilege> getRoles() {
        return roles;
    }

    public void setRoles(RealmList<UserPrivilege> roles) {
        this.roles = roles;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public int[] getRoleValues() {
        return roleValues;
    }

    public void setRoleValues(int[] roleValues) {
        this.roleValues = roleValues;
    }

    public int getUserTypeValue() {
        return userTypeValue;
    }

    public void setUserTypeValue(int userTypeValue) {
        this.userTypeValue = userTypeValue;
    }


}
