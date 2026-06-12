package com.bepa.eis.server.dataprovider.fields.strings;

public class SupplierName extends AbstractString {

    public static String FIELD_NAME = "SupplierName";

    public SupplierName() { }

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
