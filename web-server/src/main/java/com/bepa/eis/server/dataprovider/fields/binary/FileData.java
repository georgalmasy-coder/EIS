package com.bepa.eis.server.dataprovider.fields.binary;

public class FileData extends AbstractBinary {

    public static final String FIELD_NAME = "FileData";

    public FileData(byte[] value) {
        setBinaryValue(value);
        setFieldNotVisible();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return FIELD_NAME;
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue() :"";
    }

}
