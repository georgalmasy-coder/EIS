package com.bepa.eis.server.dataprovider.fields.booleans;

public class FileDeleted extends AbstractBoolean {

    public static final String FIELD_NAME = "IsDeleted";

    public FileDeleted(Boolean value) {
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
