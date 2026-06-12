package com.bepa.eis.common.enums.entity;

public enum EntityElementType {

    INTEGER("integer", 1, "IntegerValue"),
    DOUBLE("double", 2, "DoubleValue"),
    CURRENCY("currency", 3, "CurrencyValue"),
    STRING("string", 4, "StringValue"),
    LOCAL_DATE("localDate", 5, "LocalDateValue"),
    LOCAL_DATETIME("localDateTime", 6, "LocalDateTimeValue"),
    BOOLEAN("boolean", 7, "BooleanValue"),
    NOTE("note", 8, "StringValue"),
    //FILE("file", 9),
    NONE("none", -1, "");

    private final String description;
    private final int id;
    private final String valueFieldName;

    // Constructor
    EntityElementType(String description, int id, String valueFieldName) {
        this.description = description;
        this.id = id;
        this.valueFieldName = valueFieldName;
    }

    // Getters
    public String getDescription() {
        return description;
    }
    public int getId() {
        return id;
    }

    public String getValueFieldName() {
        return valueFieldName;
    }

}
