package com.bepa.eis.common.dto.mail;

import com.bepa.eis.common.enums.mail.MailStatus;
import com.bepa.eis.common.enums.mail.MailTemplateType;

import java.sql.Timestamp;

public class MailQueueItem {

    private Integer mailId;
    private MailTemplateType templateType;

    private String fromName;
    private String fromEmail;

    private String toName;
    private String toEmail;

    private String ccEmails;
    private String bccEmails;

    private String subject;
    private String bodyText;
    private String bodyHtml;

    private String parametersJson;

    private MailStatus status;

    private Integer attemptCount;
    private Integer maxAttempts;

    private Timestamp nextAttemptAt;
    private Timestamp createdAt;
    private Integer createdByUserId;
    private Timestamp lastAttemptAt;
    private Timestamp sentAt;

    private String lastError;
    private String smtpMessageId;

    private Timestamp lockedAt;
    private String lockedBy;

    public MailQueueItem() {
        mailId = null;
        templateType = MailTemplateType.SYSTEM_NOTIFICATION;

        fromName = "";
        fromEmail = "";

        toName = "";
        toEmail = "";

        ccEmails = "";
        bccEmails = "";

        subject = "";
        bodyText = "";
        bodyHtml = "";

        parametersJson = "";

        status = MailStatus.QUEUED;

        attemptCount = 0;
        maxAttempts = 5;

        nextAttemptAt = null;
        createdAt = null;
        createdByUserId = null;
        lastAttemptAt = null;
        sentAt = null;

        lastError = "";
        smtpMessageId = "";

        lockedAt = null;
        lockedBy = "";
    }

    public Integer getMailId() {
        return mailId;
    }

    public void setMailId(Integer mailId) {
        this.mailId = mailId;
    }

    public MailTemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(MailTemplateType templateType) {
        this.templateType = templateType == null ? MailTemplateType.SYSTEM_NOTIFICATION : templateType;
    }

    public String getTemplateTypeName() {
        return templateType == null ? "" : templateType.name();
    }

    public void setTemplateTypeName(String templateTypeName) {
        this.templateType = MailTemplateType.mapToType(templateTypeName);
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = safeText(fromName);
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = safeText(fromEmail);
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = safeText(toName);
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = safeText(toEmail);
    }

    public String getCcEmails() {
        return ccEmails;
    }

    public void setCcEmails(String ccEmails) {
        this.ccEmails = safeText(ccEmails);
    }

    public String getBccEmails() {
        return bccEmails;
    }

    public void setBccEmails(String bccEmails) {
        this.bccEmails = safeText(bccEmails);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = safeText(subject);
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = safeText(bodyText);
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = safeText(bodyHtml);
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = safeText(parametersJson);
    }

    public MailStatus getStatus() {
        return status;
    }

    public void setStatus(MailStatus status) {
        this.status = status == null ? MailStatus.QUEUED : status;
    }

    public String getStatusName() {
        return status == null ? "" : status.name();
    }

    public void setStatusName(String statusName) {
        this.status = MailStatus.mapToStatus(statusName);
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount == null ? 0 : attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts == null ? 5 : maxAttempts;
    }

    public Timestamp getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Timestamp nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Timestamp getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Timestamp lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = safeText(lastError);
    }

    public String getSmtpMessageId() {
        return smtpMessageId;
    }

    public void setSmtpMessageId(String smtpMessageId) {
        this.smtpMessageId = safeText(smtpMessageId);
    }

    public Timestamp getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Timestamp lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = safeText(lockedBy);
    }

    public boolean isFinalStatus() {
        return status != null && status.isFinalStatus();
    }

    public boolean canBeSent() {
        return status != null && status.canBeSent();
    }

    public boolean canBeRetried() {
        return status != null && status.canBeRetried();
    }

    public boolean hasHtmlBody() {
        return bodyHtml != null && !bodyHtml.trim().isEmpty();
    }

    public boolean hasTextBody() {
        return bodyText != null && !bodyText.trim().isEmpty();
    }

    public boolean hasRecipient() {
        return toEmail != null && !toEmail.trim().isEmpty();
    }

    public boolean hasSender() {
        return fromEmail != null && !fromEmail.trim().isEmpty();
    }

    public boolean hasAttemptsLeft() {
        return attemptCount == null
                || maxAttempts == null
                || attemptCount < maxAttempts;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "MailQueueItem [mailId=" + mailId
                + ", templateType=" + templateType
                + ", fromEmail=" + fromEmail
                + ", toEmail=" + toEmail
                + ", subject=" + subject
                + ", status=" + status
                + ", attemptCount=" + attemptCount
                + ", maxAttempts=" + maxAttempts
                + ", nextAttemptAt=" + nextAttemptAt
                + "]";
    }
}