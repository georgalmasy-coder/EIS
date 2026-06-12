package com.bepa.eis.common.enums;

public enum EventType {

    CREATE_CUSTOMER_EVENT (1, "Create Customer"),
    ENTITY_MODIFIED_EVENT(2, "Entity Created or Modified"),
    INVALID_EVENT_TYPE(-1, "Invalid Event Type");

    private final int id;
    private final String description;

    // Constructor
    EventType(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static EventType valueOf(int value) {
        for (EventType entityDataElement : EventType.values()) {
            if (entityDataElement.id == value) return entityDataElement;
        }
        return INVALID_EVENT_TYPE;
    }

}
