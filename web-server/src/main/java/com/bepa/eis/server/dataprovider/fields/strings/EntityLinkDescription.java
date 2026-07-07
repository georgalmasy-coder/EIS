package com.bepa.eis.server.dataprovider.fields.strings;

public class EntityLinkDescription extends AbstractString {

    public static String FIELD_NAME = "Description";

    public EntityLinkDescription(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Description";
    }

    @Override
    public String getFieldHeaderName() {
        return "Description";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
