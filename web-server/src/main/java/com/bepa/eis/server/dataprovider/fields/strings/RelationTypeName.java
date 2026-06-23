package com.bepa.eis.server.dataprovider.fields.strings;

public class RelationTypeName extends AbstractString {

    public static String FIELD_NAME = "RelationTypeName";

    public RelationTypeName() {
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
        return "Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Type";
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
