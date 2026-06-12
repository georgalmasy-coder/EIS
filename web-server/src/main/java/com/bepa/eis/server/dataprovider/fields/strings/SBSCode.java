package com.bepa.eis.server.dataprovider.fields.strings;

public class SBSCode extends AbstractString {

    public static String FIELD_NAME = "SBSCode";

    private boolean isNew = false;

    public SBSCode() { }

    public SBSCode(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return isNew ? "Temporary SBS Code" : "SBS Code";
    }

    @Override
    public String getFieldHeaderName() {
        return isNew ? "Temporary SBS Code" : "SBS Code";
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
