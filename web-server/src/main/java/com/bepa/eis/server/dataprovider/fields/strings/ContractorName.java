package com.bepa.eis.server.dataprovider.fields.strings;

public class ContractorName extends AbstractString {

    public static String FIELD_NAME = "ContractorName";

    public ContractorName() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Supplier";
    }

    @Override
    public String getFieldHeaderName() {
        return "Supplier";
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

    @Override
    public String toString() {
        return getValue();
    }

}
