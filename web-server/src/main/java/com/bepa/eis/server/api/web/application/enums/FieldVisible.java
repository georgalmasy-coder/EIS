package com.bepa.eis.server.api.web.application.enums;

public enum FieldVisible {

    FIELD_VISIBLE("true"),
    FIELD_NOT_VISIBLE("false");

    private final String description;

    // Constructor
    FieldVisible(String description) {
        this.description = description;
    }

    // Getters
    public String getDescription() {
        return description;
    }

}