package com.bepa.eis.server.dataprovider.fields.integers;

public class Version extends AbstractInteger {

    public static String FIELD_NAME = "Version";

    public Version() {
    }

    public Version(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Version";
    }

    @Override
    public String getFieldHeaderName() {
        return "Version";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
