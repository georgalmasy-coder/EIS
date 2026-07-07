package com.bepa.eis.server.dataprovider.fields.strings;

public class CvrNumber extends AbstractString {

    public static String FIELD_NAME = "CvrNumber";

    public CvrNumber(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "CVR";
    }

    @Override
    public String getFieldHeaderName() {
        return "CVR";
    }

    @Override
    public Integer getFieldMaxLength() {
        return 9;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 9;
    }

}
