package com.bepa.eis.server.dataprovider.fields.integers.ids;

import com.bepa.eis.server.api.web.application.enums.FieldControl;

public class CustomerId extends AbstractId {

    public static String FIELD_NAME = "CustomerId";

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
