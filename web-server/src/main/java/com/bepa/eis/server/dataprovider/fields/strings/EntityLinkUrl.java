package com.bepa.eis.server.dataprovider.fields.strings;

public class EntityLinkUrl extends AbstractString {

    public static String FIELD_NAME = "LinkUrl";

    public EntityLinkUrl(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Link Url";
    }

    @Override
    public String getFieldHeaderName() {
        return "Link Url";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
