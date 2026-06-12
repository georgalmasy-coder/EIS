package com.bepa.eis.common.enums.customer;

public enum CustomerModuleStatus {

    ACTIVE(
            1,
            "Active",
            "Customer module is active."
    ),

    TRIAL(
            2,
            "Trial",
            "Customer module is active during the trial period."
    ),

    SUSPENDED(
            3,
            "Suspended",
            "Customer module has been suspended."
    ),

    CANCELLED(
            4,
            "Cancelled",
            "Customer module has been cancelled."
    ),

    EXPIRED(
            5,
            "Expired",
            "Customer module has expired."
    );

    private final int id;
    private final String label;
    private final String description;

    CustomerModuleStatus(
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

    public boolean isTrial() {
        return this == TRIAL;
    }

    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isAccessAllowedByDefault() {
        return this == ACTIVE
                || this == TRIAL;
    }

    public boolean isTerminalStatus() {
        return this == CANCELLED
                || this == EXPIRED;
    }

    public static CustomerModuleStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (CustomerModuleStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        return null;
    }

    public static CustomerModuleStatus fromIdOrDefault(
            Integer id,
            CustomerModuleStatus defaultStatus
    ) {
        CustomerModuleStatus status = fromId(id);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? ACTIVE : defaultStatus;
    }

    public static CustomerModuleStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerModuleStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerModuleStatus fromCodeOrDefault(
            String code,
            CustomerModuleStatus defaultStatus
    ) {
        CustomerModuleStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? ACTIVE : defaultStatus;
    }
}