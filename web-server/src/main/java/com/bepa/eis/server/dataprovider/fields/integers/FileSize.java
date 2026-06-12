package com.bepa.eis.server.dataprovider.fields.integers;

public class FileSize extends AbstractInteger {

    public static String FIELD_NAME = "FileSize";

    public FileSize(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "File Size";
    }

    @Override
    public String getFieldHeaderName() {
        return "File Size";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
