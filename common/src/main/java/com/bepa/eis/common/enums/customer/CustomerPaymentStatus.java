package com.bepa.eis.common.enums.customer;

public enum CustomerPaymentStatus {

    NONE(
            "None",
            "No payment has been created."
    ),

    CREATED(
            "Created",
            "Payment record has been created, but payment processing has not yet started."
    ),

    REQUESTED(
            "Requested",
            "Payment has been requested from the payment provider."
    ),

    PENDING(
            "Pending",
            "Payment is pending and awaiting confirmation from the payment provider."
    ),

    AUTHORIZED(
            "Authorized",
            "Payment has been authorized but not yet captured."
    ),

    CAPTURED(
            "Captured",
            "Payment has been captured successfully."
    ),

    SUCCEEDED(
            "Succeeded",
            "Payment has been completed successfully."
    ),

    FAILED(
            "Failed",
            "Payment has failed."
    ),

    CANCELLED(
            "Cancelled",
            "Payment has been cancelled."
    ),

    REJECTED(
            "Rejected",
            "Payment has been rejected by the payment provider or issuer."
    ),

    EXPIRED(
            "Expired",
            "Payment request has expired before completion."
    ),

    TIMED_OUT(
            "Timed out",
            "Payment did not complete before the configured timeout."
    ),

    REFUNDED(
            "Refunded",
            "Payment has been refunded."
    ),

    PARTIALLY_REFUNDED(
            "Partially refunded",
            "Payment has been partially refunded."
    ),

    DISPUTED(
            "Disputed",
            "Payment has been disputed."
    ),

    OVERDUE(
            "Overdue",
            "Payment has not been received before the due date."
    ),

    MANUAL_REVIEW(
            "Manual review",
            "Payment requires manual review."
    );

    private final String label;
    private final String description;

    CustomerPaymentStatus(
            String label,
            String description
    ) {
        this.label = label;
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccessfulStatus() {
        return this == CAPTURED
                || this == SUCCEEDED;
    }

    public boolean isPendingStatus() {
        return this == CREATED
                || this == REQUESTED
                || this == PENDING
                || this == AUTHORIZED;
    }

    public boolean isFailedStatus() {
        return this == FAILED
                || this == CANCELLED
                || this == REJECTED
                || this == EXPIRED
                || this == TIMED_OUT
                || this == OVERDUE;
    }

    public boolean isRefundStatus() {
        return this == REFUNDED
                || this == PARTIALLY_REFUNDED;
    }

    public boolean requiresManualAttention() {
        return this == DISPUTED
                || this == MANUAL_REVIEW;
    }

    public boolean isTerminalStatus() {
        return isSuccessfulStatus()
                || isFailedStatus()
                || isRefundStatus();
    }

    public boolean canBeRetried() {
        return this == FAILED
                || this == REJECTED
                || this == EXPIRED
                || this == TIMED_OUT
                || this == OVERDUE;
    }

    public boolean isCancellationStatus() {
        return this == CANCELLED;
    }

    public boolean isProviderFinalStatus() {
        return this == SUCCEEDED
                || this == CAPTURED
                || this == FAILED
                || this == CANCELLED
                || this == REJECTED
                || this == EXPIRED
                || this == TIMED_OUT
                || this == REFUNDED
                || this == PARTIALLY_REFUNDED;
    }

    public static CustomerPaymentStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerPaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerPaymentStatus fromCodeOrDefault(
            String code,
            CustomerPaymentStatus defaultStatus
    ) {
        CustomerPaymentStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? NONE : defaultStatus;
    }
}