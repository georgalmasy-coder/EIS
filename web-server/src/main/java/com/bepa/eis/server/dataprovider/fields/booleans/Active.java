package com.bepa.eis.server.dataprovider.fields.booleans;

public class Active extends AbstractBoolean {

    public static final String FIELD_NAME = "Active";

    public Active() {
    }

    public Active(Boolean value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return FIELD_NAME;
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue().toString() : "";
    }

}
