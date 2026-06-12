package com.bepa.eis.common.enums;

public enum ServiceType {

    WEBSERVICE (1, "Web service"),
    PROVIDER_SERVICE (2, "Provider Service"),
    ENTITY_PROCESSING(3, "Entity processing"),
    INVALID_SERVICE_TYPE(-1, "Invalid Service Type");

    private final int id;
    private final String description;

    // Constructor
    ServiceType(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static ServiceType valueOf(int value) {
        for (ServiceType entityDataElement : ServiceType.values()) {
            if (entityDataElement.id == value) return entityDataElement;
        }
        return INVALID_SERVICE_TYPE;
    }

}
