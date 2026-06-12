package com.bepa.eis.server.api.web.application.enums;

public enum FieldRequired {

    FIELD_REQUIRED("true"),
    FIELD_NOT_REQUIRED("false");

    private final String description;

    // Constructor
    FieldRequired(String description) {
        this.description = description;
    }

    // Getters
    public String getDescription() {
        return description;
    }

}