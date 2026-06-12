package com.bepa.eis.server.dataprovider.fields.strings;

public class FileExtension extends AbstractString {

    public static String FIELD_NAME = "FileExtension";

    public FileExtension(String value) {
        super(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "File Extension";
    }

    @Override
    public String getFieldHeaderName() {
        return "File Extension";
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
