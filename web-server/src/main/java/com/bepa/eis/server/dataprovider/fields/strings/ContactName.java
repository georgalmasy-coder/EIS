package com.bepa.eis.server.dataprovider.fields.strings;

public class ContactName extends AbstractString {

    public static String FIELD_NAME = "ContactName";

    public ContactName() {
        setFieldEditable();
    }

    public ContactName(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Contact Name";
    }

    @Override
    public String getFieldHeaderName() {
        return "Contact Name";
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

}
