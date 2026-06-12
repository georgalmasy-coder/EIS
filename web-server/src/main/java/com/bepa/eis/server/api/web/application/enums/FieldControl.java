package com.bepa.eis.server.api.web.application.enums;

public enum FieldControl {

    HIDDEN("hidden"),
    CHECKBOX("checkbox"),
    RADIO("radio"),
    DATE("date"),
    DATETIME("datetime"),
    TIME("time"),
    TEXTAREA("textarea"),
    TEXT("text"),
    NUMBER("number"),
    DECIMAL("decimal"),
    SELECT("select"),
    NONE("none");

    private final String description;

    // Constructor
    FieldControl(String description) {
        this.description = description;
    }

    // Getters
    public String getDescription() {
        return description;
    }

}