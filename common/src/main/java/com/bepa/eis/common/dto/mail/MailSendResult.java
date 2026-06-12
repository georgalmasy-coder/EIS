package com.bepa.eis.common.dto.mail;

public class MailSendResult {

    private boolean success;
    private String smtpMessageId;
    private String errorMessage;
    private Exception exception;

    public MailSendResult() {
        success = false;
        smtpMessageId = "";
        errorMessage = "";
        exception = null;
    }

    private MailSendResult(
            boolean success,
            String smtpMessageId,
            String errorMessage,
            Exception exception
    ) {
        this.success = success;
        this.smtpMessageId = safeText(smtpMessageId);
        this.errorMessage = safeText(errorMessage);
        this.exception = exception;
    }

    public static MailSendResult success() {
        return new MailSendResult(
                true,
                "",
                "",
                null
        );
    }

    public static MailSendResult success(String smtpMessageId) {
        return new MailSendResult(
                true,
                smtpMessageId,
                "",
                null
        );
    }

    public static MailSendResult failed(String errorMessage) {
        return new MailSendResult(
                false,
                "",
                errorMessage,
                null
        );
    }

    public static MailSendResult failed(String errorMessage, Exception exception) {
        return new MailSendResult(
                false,
                "",
                errorMessage,
                exception
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSmtpMessageId() {
        return smtpMessageId;
    }

    public void setSmtpMessageId(String smtpMessageId) {
        this.smtpMessageId = safeText(smtpMessageId);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = safeText(errorMessage);
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public boolean hasSmtpMessageId() {
        return smtpMessageId != null && !smtpMessageId.trim().isEmpty();
    }

    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public boolean hasException() {
        return exception != null;
    }

    public String getCombinedErrorMessage() {
        if (hasErrorMessage()) {
            return errorMessage;
        }

        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return exception.getMessage().trim();
        }

        if (exception != null) {
            return exception.getClass().getName();
        }

        return "";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "MailSendResult [success=" + success
                + ", smtpMessageId=" + smtpMessageId
                + ", errorMessage=" + errorMessage
                + ", exception=" + (exception == null ? "" : exception.getClass().getName())
                + "]";
    }
}