package com.bepa.eis.server.dataprovider.fields.strings;

public class Address extends AbstractString {

    public static String FIELD_NAME = "Address";

    public Address(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Address";
    }

    @Override
    public String getFieldHeaderName() {
        return "Address";
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
    public String toString() {
        return getValue();
    }

}
