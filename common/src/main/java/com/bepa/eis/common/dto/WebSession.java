package com.bepa.eis.common.dto;

import java.util.Date;

public class WebSession {

    private Integer id;
    private String sessionId;
    private Integer customerId;
    private Integer projectId;
    private Integer userId;
    private Date created;
    private Date lastAccessed;

    private String ipAddress;
    private String userAgent;
    private String countryCode;
    private String countryName;
    private String regionName;
    private String city;
    private Double latitude;
    private Double longitude;
    private Date loginAt;
    private Date logoutAt;
    private Date expiredAt;
    private String endedReason;

    public WebSession() {
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setLastAccessed(Date lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLoginAt(Date loginAt) {
        this.loginAt = loginAt;
    }

    public void setLogoutAt(Date logoutAt) {
        this.logoutAt = logoutAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void setEndedReason(String endedReason) {
        this.endedReason = endedReason;
    }

    public int getId() {
        return id == null ? 0 : id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastAccessed() {
        return lastAccessed;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getCity() {
        return city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Date getLoginAt() {
        return loginAt;
    }

    public Date getLogoutAt() {
        return logoutAt;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public String getEndedReason() {
        return endedReason;
    }

    public boolean isSessionValid() {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        return userId != null && userId > 0;
    }

    @Override
    public String toString() {
        return "Web session: "
                + sessionId
                + " for customer: "
                + customerId
                + " for project: "
                + projectId
                + " for user: "
                + userId;
    }
}