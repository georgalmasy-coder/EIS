package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class UserId extends AbstractId {

    public static String FIELD_NAME = "UserId";

    public UserId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "User ID";
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue().toString() : "";
    }

}
