package com.bepa.eis.server.dataprovider.fields.booleans;

public class Latest extends AbstractBoolean {

    public static final String FIELD_NAME = "Latest";

    public Latest(Boolean value) {
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
        return getValue().toString();
    }

}
