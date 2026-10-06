package com.bepa.eis.server.dataprovider.fields.strings.phone;

public class UserPhone extends AbstractPhone {

    public static String FIELD_NAME = "UserPhone";

    public UserPhone() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Phone";
    }

    @Override
    public String getFieldHeaderName() {
        return "Phone";
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
