package com.bepa.eis.server.dataprovider.fields.strings;

public class City extends AbstractString {

    public static String FIELD_NAME = "City";

    public City(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "City";
    }

    @Override
    public String getFieldHeaderName() {
        return "City";
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
