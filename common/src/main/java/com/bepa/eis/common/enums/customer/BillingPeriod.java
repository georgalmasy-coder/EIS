package com.bepa.eis.common.enums.customer;

import java.util.Locale;

public enum BillingPeriod {

    MONTHLY("MONTHLY", "Monthly", "Monthly billing period", 1, 4, true),
    QUARTERLY("QUARTERLY", "Quarterly", "Quarterly billing period", 3, 3, true),
    SEMI_ANNUAL("SEMI_ANNUAL", "Semi-annual", "Semi-annual billing period", 6, 2, true),
    ANNUAL("ANNUAL", "Annually", "Annual billing period", 12, 1, true);

    private final String code;
    private final String label;
    private final String description;
    private final int months;
    private final int displayOrder;
    private final boolean active;

    BillingPeriod(
            String code,
            String label,
            String description,
            int months,
            int displayOrder,
            boolean active
    ) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.months = months;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getMonths() {
        return months;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public static BillingPeriod fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);

        for (BillingPeriod billingPeriod : values()) {
            if (billingPeriod.code.equalsIgnoreCase(normalizedCode)
                    || billingPeriod.name().equalsIgnoreCase(normalizedCode)) {
                return billingPeriod;
            }
        }

        return null;
    }
}
