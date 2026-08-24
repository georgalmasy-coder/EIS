package com.bepa.eis.common.enums.entity;

import com.bepa.eis.common.enums.customer.CustomerModuleStatus;

public enum SBSCodeTypes {
    FUNCTIONAL (1,  "Function", "=", true),
    LOCATION(2, "Location", "+", true),
    PRODUCT(3, "Product", "-", true),
    TYPE_OR_CLASS(4, "Type or classification", "%", true),
    OTHER(5, "Other", "#", true);

    private final int id;
    private final String code;
    private final String description;
    private final boolean active;
    private final String prefix;

    // Constructor
    SBSCodeTypes(int id, String description, String prefix, boolean active) {
        this.id = id;
        this.code = prefix + " " + description;
        this.description = description;
        this.prefix = prefix;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isActive() {
        return active;
    }

    public static SBSCodeTypes fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        try {
            Integer normalizedCode = Integer.valueOf(code.trim());
            for (SBSCodeTypes type : values()) {
                if (type.getId() == normalizedCode) {
                    return type;
                }
            }
        } catch (Exception e) {
        }


        return FUNCTIONAL;
    }

}
