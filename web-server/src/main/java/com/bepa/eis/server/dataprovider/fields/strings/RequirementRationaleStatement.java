package com.bepa.eis.server.dataprovider.fields.strings;

public class RequirementRationaleStatement extends AbstractString {

    public static String FIELD_NAME = "RationaleStatement";

    public RequirementRationaleStatement() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Rationale Statement";
    }

    @Override
    public String getFieldHeaderName() {
        return "Rat. Statement";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 255;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 10;
    }

}
