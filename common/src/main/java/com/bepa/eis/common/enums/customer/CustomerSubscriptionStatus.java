package com.bepa.eis.common.enums.customer;

public enum CustomerSubscriptionStatus {

    NONE(
            "None",
            "Customer has no active trial or subscription."
    ),

    TRIAL(
            "Trial",
            "Customer is currently in the trial period."
    ),

    TRIAL_EXPIRING(
            "Trial expiring",
            "Customer trial period is close to expiry."
    ),

    TRIAL_EXPIRED(
            "Trial expired",
            "Customer trial period has expired."
    ),

    PENDING_CONFIRMATION(
            "Pending confirmation",
            "Customer must confirm continuation or renewal of the subscription."
    ),

    PAYMENT_PENDING(
            "Payment pending",
            "Subscription payment has been requested and is waiting for confirmation."
    ),

    ACTIVE(
            "Active",
            "Customer subscription is active."
    ),

    EXPIRING(
            "Expiring",
            "Customer subscription is close to expiry."
    ),

    EXPIRED(
            "Expired",
            "Customer subscription period has expired."
    ),

    PAYMENT_OVERDUE(
            "Payment overdue",
            "Payment has not been received before the due date."
    ),

    GRACE_PERIOD(
            "Grace period",
            "Customer subscription has expired, but the payment grace period is still active."
    ),

    SUSPENDED(
            "Suspended",
            "Customer subscription is suspended."
    ),

    CANCELLED(
            "Cancelled",
            "Customer subscription has been cancelled."
    );

    private final String label;
    private final String description;

    CustomerSubscriptionStatus(String label, String description) {
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

    public boolean isTrialStatus() {
        return this == TRIAL
                || this == TRIAL_EXPIRING
                || this == TRIAL_EXPIRED;
    }

    public boolean isActiveStatus() {
        return this == TRIAL
                || this == TRIAL_EXPIRING
                || this == ACTIVE
                || this == EXPIRING
                || this == GRACE_PERIOD;
    }

    public boolean isPaymentRequiredStatus() {
        return this == PAYMENT_PENDING
                || this == PAYMENT_OVERDUE
                || this == GRACE_PERIOD;
    }

    public boolean isSuspendedStatus() {
        return this == SUSPENDED;
    }

    public boolean isTerminalStatus() {
        return this == CANCELLED;
    }

    public boolean isLoginAllowedByDefault() {
        return this == TRIAL
                || this == TRIAL_EXPIRING
                || this == ACTIVE
                || this == EXPIRING
                || this == GRACE_PERIOD;
    }

    public static CustomerSubscriptionStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerSubscriptionStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerSubscriptionStatus fromCodeOrDefault(
            String code,
            CustomerSubscriptionStatus defaultStatus
    ) {
        CustomerSubscriptionStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? NONE : defaultStatus;
    }
}