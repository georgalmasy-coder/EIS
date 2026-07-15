package com.bepa.eis.server.dataprovider.fields.strings.email;

public class ContactEmail extends AbstractEmail {

    public static String FIELD_NAME = "ContactEmail";

    public ContactEmail() {
        setFieldEditable();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Contact Email";
    }

    @Override
    public String getFieldHeaderName() {
        return "Contact Email";
    }

    @Override
    public String getSortKey() {
        return getValue();
    }
}
