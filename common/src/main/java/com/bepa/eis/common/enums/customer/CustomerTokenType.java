package com.bepa.eis.common.enums.customer;

public enum CustomerTokenType {

    EMAIL_CONFIRMATION(
            "Email confirmation",
            "Token used when the customer must confirm account creation."
    ),

    SUBSCRIPTION_CONTINUATION(
            "Subscription continuation",
            "Token used when the customer must confirm continuation after the trial period."
    ),

    SUBSCRIPTION_RENEWAL(
            "Subscription renewal",
            "Token used when the customer must confirm subscription renewal."
    ),

    PAYMENT_UPDATE(
            "Payment update",
            "Token used when the customer must update or retry payment."
    ),

    PAYMENT_CONFIRMATION(
            "Payment confirmation",
            "Token used when the customer must confirm or complete payment."
    ),

    REACTIVATION(
            "Reactivation",
            "Token used when the customer must reactivate a suspended account."
    ),

    ADMIN_REACTIVATION(
            "Admin reactivation",
            "Token used for administrator-initiated reactivation flows."
    );

    private final String label;
    private final String description;

    CustomerTokenType(String label, String description) {
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

    public boolean isCustomerConfirmationToken() {
        return this == EMAIL_CONFIRMATION;
    }

    public boolean isSubscriptionToken() {
        return this == SUBSCRIPTION_CONTINUATION
                || this == SUBSCRIPTION_RENEWAL;
    }

    public boolean isPaymentToken() {
        return this == PAYMENT_UPDATE
                || this == PAYMENT_CONFIRMATION;
    }

    public boolean isReactivationToken() {
        return this == REACTIVATION
                || this == ADMIN_REACTIVATION;
    }

    public static CustomerTokenType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerTokenType tokenType : values()) {
            if (tokenType.name().equalsIgnoreCase(normalizedCode)) {
                return tokenType;
            }
        }

        return null;
    }

    public static CustomerTokenType fromCodeOrDefault(
            String code,
            CustomerTokenType defaultTokenType
    ) {
        CustomerTokenType tokenType = fromCode(code);

        if (tokenType != null) {
            return tokenType;
        }

        return defaultTokenType == null ? EMAIL_CONFIRMATION : defaultTokenType;
    }
}