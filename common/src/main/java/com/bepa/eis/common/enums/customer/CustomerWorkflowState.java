package com.bepa.eis.common.enums.customer;

public enum CustomerWorkflowState {

    CREATED(
            "Created",
            "Customer has been created, but the workflow has not yet started."
    ),

    PENDING_EMAIL_CONFIRMATION(
            "Pending email confirmation",
            "Customer confirmation email has been sent and the workflow is waiting for the customer to confirm the account."
    ),

    EMAIL_CONFIRMED(
            "Email confirmed",
            "Customer has confirmed the account email address."
    ),

    TRIAL_ACTIVE(
            "Trial active",
            "Customer trial period is active and users are allowed to log in."
    ),

    TRIAL_EXPIRING(
            "Trial expiring",
            "Customer trial period is close to expiry and a continuation confirmation is required."
    ),

    PENDING_SUBSCRIPTION_CONFIRMATION(
            "Pending subscription confirmation",
            "The workflow is waiting for the customer to confirm continuation of the subscription."
    ),

    PAYMENT_PENDING(
            "Payment pending",
            "A payment request has been created and the workflow is waiting for payment confirmation."
    ),

    SUBSCRIPTION_ACTIVE(
            "Subscription active",
            "Customer subscription is active and users are allowed to log in."
    ),

    SUBSCRIPTION_EXPIRING(
            "Subscription expiring",
            "Customer subscription is close to expiry and renewal confirmation is required."
    ),

    PAYMENT_OVERDUE(
            "Payment overdue",
            "Payment has not been received before the due date, but the customer may still be inside the grace period."
    ),

    SUSPENDED(
            "Suspended",
            "Customer has been suspended and users are not allowed to log in."
    ),

    CANCELLED(
            "Cancelled",
            "Customer workflow has been cancelled."
    ),

    WAITING_FOR_MANUAL_ATTENTION(
            "Waiting for manual attention",
            "Customer workflow cannot continue automatically and requires manual handling."
    );

    private final String label;
    private final String description;

    CustomerWorkflowState(String label, String description) {
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

    public boolean isTerminalState() {
        return this == CANCELLED;
    }

    public boolean isSuspendedState() {
        return this == SUSPENDED;
    }

    public boolean isManualAttentionState() {
        return this == WAITING_FOR_MANUAL_ATTENTION;
    }

    public boolean isLoginAllowedByDefault() {
        return this == TRIAL_ACTIVE
                || this == TRIAL_EXPIRING
                || this == PENDING_SUBSCRIPTION_CONFIRMATION
                || this == SUBSCRIPTION_ACTIVE
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public boolean requiresCustomerAction() {
        return this == PENDING_EMAIL_CONFIRMATION
                || this == PENDING_SUBSCRIPTION_CONFIRMATION
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public boolean requiresPaymentAction() {
        return this == PAYMENT_PENDING
                || this == PAYMENT_OVERDUE;
    }

    public static CustomerWorkflowState fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerWorkflowState state : values()) {
            if (state.name().equalsIgnoreCase(normalizedCode)) {
                return state;
            }
        }

        return null;
    }
}