package com.bepa.eis.server.dataprovider.fields.strings;

import java.math.BigDecimal;

public class CustomerName extends AbstractString {

    public static String FIELD_NAME = "CustomerName";

    public CustomerName(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Company Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Company Name";
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
    public String toString() {
        return getValue();
    }

}
