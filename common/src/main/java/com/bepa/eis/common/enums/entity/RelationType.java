package com.bepa.eis.common.enums.entity;

public enum RelationType {

    DELETED (0, "Deleted"),
    CONFIRMED(1, "Confirmed"),
    NOT_RELEVANT(2, "Not Relevant"),
    INVALID_RELATION_TYPE(-1, "Relation Type");

    private final int id;
    private final String description;

    // Constructor
    RelationType(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }

    public static RelationType valueOf(int value) {
        for (RelationType entityDataElement : RelationType.values()) {
            if (entityDataElement.id == value) return entityDataElement;
        }
        return INVALID_RELATION_TYPE;
    }

    public static RelationType valueOfDescription(String description) {
        for (RelationType entityDataElement : RelationType.values()) {
            if (entityDataElement.description.equalsIgnoreCase(description) ) return entityDataElement;
        }
        return INVALID_RELATION_TYPE;
    }
}
