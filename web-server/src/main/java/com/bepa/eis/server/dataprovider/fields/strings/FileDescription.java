package com.bepa.eis.server.dataprovider.fields.strings;

public class FileDescription extends AbstractString {

    public static String FIELD_NAME = "Description";

    public FileDescription(String value) {
        super(value);
        setFieldVisible();
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
    public Integer getFieldMaxLength() {
        return 80;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 10;
    }

}
