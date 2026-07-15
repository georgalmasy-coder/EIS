package com.bepa.eis.server.dataprovider.fields.strings;

public class StakeholderName extends AbstractString {

    public static String FIELD_NAME = "StakeholderName";

    public StakeholderName() {
        setFieldRequired();
        setFieldEditable();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Name";
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
