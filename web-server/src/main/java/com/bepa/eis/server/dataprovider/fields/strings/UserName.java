package com.bepa.eis.server.dataprovider.fields.strings;

public class UserName extends AbstractString {

    public static String FIELD_NAME = "Name";

    public UserName(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "User Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "User Name";
    }

    @Override
    public Integer getFieldMinLength() {
        return 5;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 25;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
