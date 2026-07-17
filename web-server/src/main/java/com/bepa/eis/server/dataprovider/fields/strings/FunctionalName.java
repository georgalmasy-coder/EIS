package com.bepa.eis.server.dataprovider.fields.strings;

public class FunctionalName extends AbstractString {

    public static String FIELD_NAME = "FunctionalName";

    public FunctionalName() {
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Functional Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Name";
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

}
