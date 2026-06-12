package com.bepa.eis.server.api.DTO;

import java.util.Date;

public class NotificationTBD {


    private int notificationId;
    private String notificationText;
    private String notificationType;
    private String notificationUrl;
    private Boolean acknowledged;
    private Date notificationDate;
    private Date acknowledgedDate;
    private int notifyer;
    private int receiver;
    private int projectId;

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public Boolean getAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public Date getNotificationDate() {
        return notificationDate;
    }

    public void setNotificationDate(Date notificationDate) {
        this.notificationDate = notificationDate;
    }

    public Date getAcknowledgedDate() {
        return acknowledgedDate;
    }

    public void setAcknowledgedDate(Date acknowledgedDate) {
        this.acknowledgedDate = acknowledgedDate;
    }

    public int getNotifyer() {
        return notifyer;
    }

    public void setNotifyer(int notifyer) {
        this.notifyer = notifyer;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}
