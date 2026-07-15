package com.bepa.eis.server.dataprovider.fields.strings.phone;

public class ContactPhone extends AbstractPhone {

    public static String FIELD_NAME = "ContactPhone";

    public ContactPhone() {
        setFieldEditable();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Contact Phone";
    }

    @Override
    public String getFieldHeaderName() {
        return "Contact Phone";
    }

    @Override
    public String getSortKey() {
        return getValue();
    }
}
