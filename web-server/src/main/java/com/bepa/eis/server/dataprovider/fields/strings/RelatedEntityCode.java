package com.bepa.eis.server.dataprovider.fields.strings;

public class RelatedEntityCode extends AbstractString {

    public static String FIELD_NAME = "RelatedEntityCode";

    public RelatedEntityCode() {
        super(FIELD_NAME);
        setFieldNotEditable();
        setFieldVisible();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Code";
    }

    @Override
    public String getFieldHeaderName() {
        return "Code";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
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


    @Override
    public String getSortKey() {
        return getValue();
    }
}
