package com.bepa.eis.common.enums.customer;

public enum CustomerPaymentMethodStatus {

    ACTIVE(
            1,
            "Active",
            "Payment method is active and can be used for payments."
    ),

    EXPIRED(
            2,
            "Expired",
            "Payment method has expired and should not be used."
    ),

    DISABLED(
            3,
            "Disabled",
            "Payment method has been disabled and should not be used."
    ),

    DELETED(
            4,
            "Deleted",
            "Payment method has been deleted or removed from active use."
    );

    private final int id;
    private final String label;
    private final String description;

    CustomerPaymentMethodStatus(
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

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }

    public boolean canBeUsedForPayment() {
        return this == ACTIVE;
    }

    public boolean isFinalStatus() {
        return this == DELETED;
    }

    public static CustomerPaymentMethodStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (CustomerPaymentMethodStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static CustomerPaymentMethodStatus fromIdOrDefault(
            Integer id,
            CustomerPaymentMethodStatus defaultStatus
    ) {
        CustomerPaymentMethodStatus status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? ACTIVE : defaultStatus;
    }

    public static CustomerPaymentMethodStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerPaymentMethodStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerPaymentMethodStatus fromCodeOrDefault(
            String code,
            CustomerPaymentMethodStatus defaultStatus
    ) {
        CustomerPaymentMethodStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? ACTIVE : defaultStatus;
    }
}