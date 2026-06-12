package com.bepa.eis.common.enums.customer;

public enum CustomerWorkflowStatus {

    ACTIVE(
            "Active",
            "The customer workflow is active and can be processed automatically."
    ),

    COMPLETED(
            "Completed",
            "The customer workflow has completed successfully."
    ),

    CANCELLED(
            "Cancelled",
            "The customer workflow has been cancelled."
    ),

    SUSPENDED(
            "Suspended",
            "The customer workflow is suspended and will not continue automatically until reactivated."
    ),

    WAITING_FOR_MANUAL_ATTENTION(
            "Waiting for manual attention",
            "The customer workflow requires manual attention before it can continue."
    );

    private final String label;
    private final String description;

    CustomerWorkflowStatus(String label, String description) {
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

    public boolean isActiveStatus() {
        return this == ACTIVE;
    }

    public boolean isCompletedStatus() {
        return this == COMPLETED;
    }

    public boolean isCancelledStatus() {
        return this == CANCELLED;
    }

    public boolean isSuspendedStatus() {
        return this == SUSPENDED;
    }

    public boolean isManualAttentionStatus() {
        return this == WAITING_FOR_MANUAL_ATTENTION;
    }

    public boolean canBeProcessedAutomatically() {
        return this == ACTIVE;
    }

    public boolean isTerminalStatus() {
        return this == COMPLETED
                || this == CANCELLED;
    }

    public static CustomerWorkflowStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerWorkflowStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalizedCode)) {
                return status;
            }
        }

        return null;
    }

    public static CustomerWorkflowStatus fromCodeOrDefault(
            String code,
            CustomerWorkflowStatus defaultStatus
    ) {
        CustomerWorkflowStatus status = fromCode(code);

        if (status != null) {
            return status;
        }

        return defaultStatus == null ? ACTIVE : defaultStatus;
    }
}