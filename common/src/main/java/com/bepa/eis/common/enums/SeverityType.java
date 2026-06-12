package com.bepa.eis.common.enums;

public enum SeverityType {

    CRITICAL (1, "Critical"),
    HIGH(2, "High"),
    MEDIUM(3, "Medium"),
    LOW(4, "Low"),
    INVALID_SEVERITY_TYPE(-1, "Severity Type");

    private final int id;
    private final String description;

    // Constructor
    SeverityType(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static SeverityType valueOf(int value) {
        for (SeverityType entityDataElement : SeverityType.values()) {
            if (entityDataElement.id == value) return entityDataElement;
        }
        return INVALID_SEVERITY_TYPE;
    }

}
