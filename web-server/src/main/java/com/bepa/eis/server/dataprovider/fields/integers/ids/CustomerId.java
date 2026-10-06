package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class CustomerId extends AbstractId {

    public static String FIELD_NAME = "CustomerId";

    public CustomerId() {
    }

    public CustomerId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Customer ID";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
