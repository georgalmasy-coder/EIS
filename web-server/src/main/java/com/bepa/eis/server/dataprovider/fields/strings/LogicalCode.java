package com.bepa.eis.server.dataprovider.fields.strings;

public class LogicalCode extends AbstractString {

    public static String FIELD_NAME = "LogicalCode";

    private boolean isNew = false;

    public LogicalCode() {
        setFieldRequired();
    }

    public LogicalCode(boolean isNew) {
        this.isNew = isNew;
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return isNew ? "Temporary Logical Code" : "Logical Code";
    }

    @Override
    public String getFieldHeaderName() {
        return isNew ? "Temporary Logical Code" : "Logical Code";
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
