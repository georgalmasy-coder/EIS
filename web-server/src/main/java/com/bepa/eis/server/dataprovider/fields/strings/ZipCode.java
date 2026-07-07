package com.bepa.eis.server.dataprovider.fields.strings;

public class ZipCode extends AbstractString {

    public static String FIELD_NAME = "ZipCode";

    public ZipCode(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Zip Code";
    }

    @Override
    public String getFieldHeaderName() {
        return "Zip Code";
    }

    @Override
    public Integer getFieldMinLength() {
        return 3;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 10;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
