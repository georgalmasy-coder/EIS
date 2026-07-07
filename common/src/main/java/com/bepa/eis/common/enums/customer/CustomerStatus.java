package com.bepa.eis.common.enums.customer;

public enum CustomerStatus {

    CREATED(
            1,
            "Created",
            "Customer has been created, but onboarding has not yet started."
    ),

    PENDING_CONFIRMATION(
            2,
            "Pending confirmation",
            "Customer must confirm account creation before access is enabled."
    ),

    TRIAL_ACTIVE(
            3,
            "Trial active",
            "Customer is in the trial period and access is allowed."
    ),

    PENDING_SUBSCRIPTION_CONFIRMATION(
            4,
            "Pending subscription confirmation",
            "Customer must confirm continuation of the subscription."
    ),

    PAYMENT_PENDING(
            5,
            "Payment pending",
            "Payment has been requested and the system is waiting for payment confirmation."
    ),

    SUBSCRIPTION_ACTIVE(
            6,
            "Subscription active",
            "Customer has an active subscription and access is allowed."
    ),

    SUBSCRIPTION_EXPIRING(
            7,
            "Subscription expiring",
            "Customer subscription is close to expiry."
    ),

    PAYMENT_OVERDUE(
            8,
            "Payment overdue",
            "Payment is overdue, but customer may still be inside the grace period."
    ),

    SUSPENDED(
            9,
            "Suspended",
            "Customer is suspended and access is not allowed."
    ),

    CANCELLED(
            10,
            "Cancelled",
            "Customer has been cancelled."
    );

    private final int id;
    private final String label;
    private final String description;

    CustomerStatus(
            int id,
            String label,
            String description
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public int getId() {
        return id;
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

    public boolean isLoginAllowedByDefault() {
        return this == TRIAL_ACTIVE
                || this == PENDING_SUBSCRIPTION_CONFIRMATION
                || this == PAYMENT_PENDING
                || this == SUBSCRIPTION_ACTIVE
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isTerminalStatus() {
        return this == CANCELLED;
    }

    public boolean requiresCustomerAction() {
        return this == PENDING_CONFIRMATION
                || this == PENDING_SUBSCRIPTION_CONFIRMATION
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public boolean requiresPaymentAction() {
        return this == PAYMENT_PENDING
                || this == PAYMENT_OVERDUE;
    }

    public boolean isActiveCommercialStatus() {
        return this == TRIAL_ACTIVE
                || this == SUBSCRIPTION_ACTIVE
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public static CustomerStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (CustomerStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static CustomerStatus fromIdOrDefault(
            Integer id,
            CustomerStatus defaultStatus
    ) {
        CustomerStatus status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? CREATED : defaultStatus;
    }

    public static CustomerStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerStatus fromCodeOrDefault(
            String code,
            CustomerStatus defaultStatus
    ) {
        CustomerStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? CREATED : defaultStatus;
    }

    public static String getActiveStatusIds() {
        return PENDING_CONFIRMATION.getId() + ", " +
                TRIAL_ACTIVE.getId() + ", " +
                PENDING_SUBSCRIPTION_CONFIRMATION.getId() + ", " +
                PAYMENT_PENDING.getId() + ", " +
                SUBSCRIPTION_ACTIVE.getId() + ", " +
                SUBSCRIPTION_EXPIRING.getId() + ", " +
                PAYMENT_OVERDUE.getId();
    }
}