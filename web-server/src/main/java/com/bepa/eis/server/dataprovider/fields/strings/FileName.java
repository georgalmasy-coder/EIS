package com.bepa.eis.server.dataprovider.fields.strings;

public class FileName extends AbstractString {

    public static String FIELD_NAME = "FileName";

    public FileName(String value) {
        super(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "File Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "File Name";
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
