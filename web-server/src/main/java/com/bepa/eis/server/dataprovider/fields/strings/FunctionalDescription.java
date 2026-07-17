package com.bepa.eis.server.dataprovider.fields.strings;

public class FunctionalDescription extends AbstractString {

    public static String FIELD_NAME = "FunctionalDescription";

    public FunctionalDescription() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Functional Description";
    }

    @Override
    public String getFieldHeaderName() {
        return "Description";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 4000;
    }

    @Override
    public Integer getFieldRow() {
        return 20;
    }

    @Override
    public Integer getFieldCol() {
        return 150;
    }

    @Override
    public String getSortKey() {
        return getValue();
    }
}
