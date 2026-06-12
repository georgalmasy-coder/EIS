package com.bepa.eis.server.dataprovider.fields.strings;

public class RequirementName extends AbstractString {

    public static String FIELD_NAME = "RequirementName";

    public RequirementName() { }

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
        return "Name";
    }

    @Override
    public Integer getFieldMinLength() {
        return 5;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 25;
    }

}
