package com.bepa.eis.common.enums.customer;

import java.util.Locale;

public enum Subscription {

    BASIS("BASIS-MODULE", "Basis Module", 1, true),
    PRO("PRO-MODULE", "Pro Module", 2, true),
    MASTER("MASTER-MODULE", "Master Module", 3, true);

    private final String moduleCode;
    private final String label;
    private final int displayOrder;
    private final boolean active;

    Subscription(
            String moduleCode,
            String label,
            int displayOrder,
            boolean active
    ) {
        this.moduleCode = moduleCode;
        this.label = label;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getLabel() {
        return label;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public String getCode() {
        return name();
    }

    public static Subscription fromModuleCode(String moduleCode) {
        if (moduleCode == null || moduleCode.trim().isEmpty()) {
            return null;
        }

        String normalizedModuleCode = moduleCode.trim().toUpperCase(Locale.ROOT);

        for (Subscription subscription : values()) {
            if (subscription.moduleCode.equalsIgnoreCase(normalizedModuleCode)
                    || subscription.name().equalsIgnoreCase(normalizedModuleCode)) {
                return subscription;
            }
        }

        return null;
    }

    public static Subscription fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);

        for (Subscription subscription : values()) {
            if (subscription.name().equalsIgnoreCase(normalizedCode)) {
                return subscription;
            }
        }

        return null;
    }
}
