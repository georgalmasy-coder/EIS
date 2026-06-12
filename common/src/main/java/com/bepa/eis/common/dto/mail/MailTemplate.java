package com.bepa.eis.common.dto.mail;

import com.bepa.eis.common.enums.mail.MailTemplateType;

public class MailTemplate {

    private MailTemplateType templateType;
    private String subject;
    private String body;
    private String contentType;
    private String sourceFileName;

    public MailTemplate() {
        templateType = MailTemplateType.SYSTEM_NOTIFICATION;
        subject = "";
        body = "";
        contentType = "text/html; charset=UTF-8";
        sourceFileName = "";
    }

    public MailTemplate(
            MailTemplateType templateType,
            String subject,
            String body,
            String contentType,
            String sourceFileName
    ) {
        this.templateType = templateType == null ? MailTemplateType.SYSTEM_NOTIFICATION : templateType;
        this.subject = safeText(subject);
        this.body = safeText(body);
        this.contentType = safeText(contentType);

        if (this.contentType.isEmpty()) {
            this.contentType = "text/html; charset=UTF-8";
        }

        this.sourceFileName = safeText(sourceFileName);
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = safeText(subject);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = safeText(body);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = safeText(contentType);

        if (this.contentType.isEmpty()) {
            this.contentType = "text/html; charset=UTF-8";
        }
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = safeText(sourceFileName);
    }

    public boolean isHtml() {
        return contentType != null
                && contentType.toLowerCase().contains("text/html");
    }

    public boolean isText() {
        return contentType != null
                && contentType.toLowerCase().contains("text/plain");
    }

    public boolean hasSubject() {
        return subject != null && !subject.trim().isEmpty();
    }

    public boolean hasBody() {
        return body != null && !body.trim().isEmpty();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "MailTemplate [templateType=" + templateType
                + ", subject=" + subject
                + ", contentType=" + contentType
                + ", sourceFileName=" + sourceFileName
                + "]";
    }
}