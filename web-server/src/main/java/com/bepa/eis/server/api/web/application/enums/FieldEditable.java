package com.bepa.eis.server.api.web.application.enums;

public enum FieldEditable {

    FIELD_EDITABLE("true"),
    FIELD_NOT_EDITABLE("false");

    private final String description;

    // Constructor
    FieldEditable(String description) {
        this.description = description;
    }

    // Getters
    public String getDescription() {
        return description;
    }

}