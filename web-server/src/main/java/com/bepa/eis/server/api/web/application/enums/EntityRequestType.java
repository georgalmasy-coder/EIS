package com.bepa.eis.server.api.web.application.enums;

public enum EntityRequestType {

    LIST_OF_ENTITIES("List of entities"),
    EDIT_ENTITY("Edit single entity"),
    CREATE_ENTITY("Create new entity");

    private final String description;

    // Constructor
    EntityRequestType(String description) {
        this.description = description;
    }

    // Getters
    public String getDescription() {
        return description;
    }

}