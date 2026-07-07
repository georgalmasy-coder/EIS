package com.bepa.eis.server.dataprovider.fields.strings;

public class Country extends AbstractString {

    public static String FIELD_NAME = "Country";

    public Country(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Country";
    }

    @Override
    public String getFieldHeaderName() {
        return "Country";
    }

    @Override
    public Integer getFieldMinLength() {
        return 2;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 50;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
