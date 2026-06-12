package com.bepa.eis.server.dataprovider.fields.strings;

public class ContentType extends AbstractString {

    public static String FIELD_NAME = "ContentType";

    public ContentType(String value) {
        super(value);
        setFieldNotVisible();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Content Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Content Type";
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
