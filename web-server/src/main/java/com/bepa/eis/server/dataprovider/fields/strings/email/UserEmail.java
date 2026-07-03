package com.bepa.eis.server.dataprovider.fields.strings.email;

public class UserEmail extends AbstractEmail {

    public static String FIELD_NAME = "UserEmail";

    public UserEmail() { }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Email (login)";
    }

    @Override
    public String getFieldHeaderName() {
        return "Email";
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
