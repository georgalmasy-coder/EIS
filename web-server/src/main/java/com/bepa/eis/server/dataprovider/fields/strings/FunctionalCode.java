package com.bepa.eis.server.dataprovider.fields.strings;

public class FunctionalCode extends AbstractString {

    public static String FIELD_NAME = "FunctionalCode";

    private boolean isNew = false;

    public FunctionalCode() {
        setFieldRequired();
    }

    public FunctionalCode(boolean isNew) {
        this.isNew = isNew;
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return isNew ? "Temporary Functional Code" : "Functional Code";
    }

    @Override
    public String getFieldHeaderName() {
        return isNew ? "Temporary Functional Code" : "Functional Code";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 25;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @Override
    public String getSortKey() {
        return getValue();
    }
}
