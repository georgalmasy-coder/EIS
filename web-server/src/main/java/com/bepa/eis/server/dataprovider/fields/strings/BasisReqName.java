package com.bepa.eis.server.dataprovider.fields.strings;

public class BasisReqName extends AbstractString {

    public static String FIELD_NAME = "SthReqName";

    public BasisReqName() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Requirement Name";
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
