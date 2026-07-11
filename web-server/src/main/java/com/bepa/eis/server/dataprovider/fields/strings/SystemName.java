package com.bepa.eis.server.dataprovider.fields.strings;

public class SystemName extends AbstractString {

    public static String FIELD_NAME = "SystemName";

    public SystemName() {
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "System Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "System Name";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 80;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 10;
    }

}
