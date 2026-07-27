package com.bepa.eis.common.enums.customer;

import java.util.Locale;

public enum Subscription {

    BASIS("BASIS-MODULE", "Basis"),
    PRO("PRO-MODULE", "Pro"),
    MASTER("MASTER-MODULE", "Master");

    private final String moduleCode;
    private final String label;

    Subscription(
            String moduleCode,
            String label
    ) {
        this.moduleCode = moduleCode;
        this.label = label;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getLabel() {
        return label;
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
}
