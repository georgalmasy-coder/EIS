package com.bepa.eis.server.dataprovider.fields.strings;

public class UserInitials extends AbstractString {

    public static String FIELD_NAME = "Initials";

    public UserInitials(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Initials";
    }

    @Override
    public String getFieldHeaderName() {
        return "Initials";
    }

    @Override
    public Integer getFieldMinLength() {
        return 0;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 5;
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
