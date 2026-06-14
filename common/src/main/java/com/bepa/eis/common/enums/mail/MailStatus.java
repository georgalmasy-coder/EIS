package com.bepa.eis.common.enums.mail;

public enum MailStatus {

    QUEUED("Queued"),
    SENDING("Sending"),
    SENT("Sent"),
    FAILED("Failed"),
    UNDELIVERED("Undelivered"),
    CANCELLED("Cancelled");

    private final String description;

    MailStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static MailStatus mapToStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return QUEUED;
        }

        String normalizedValue = value.trim();

        for (MailStatus status : MailStatus.values()) {
            if (status.name().equalsIgnoreCase(normalizedValue)
                    || status.description.equalsIgnoreCase(normalizedValue)) {
                return status;
            }
        }

        return QUEUED;
    }

    public boolean isFinalStatus() {
        return this == SENT
                || this == UNDELIVERED
                || this == CANCELLED;
    }

    public boolean canBeSent() {
        return this == QUEUED
                || this == FAILED;
    }

    public boolean canBeRetried() {
        return this == FAILED;
    }
}