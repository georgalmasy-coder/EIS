package com.bepa.eis.server.dataprovider.fields.strings;

public class RelatedEntityTypeName extends AbstractString {

    public static String FIELD_NAME = "RelatedEntityTypeName";

    public RelatedEntityTypeName(String value) {
        super(value);
        setFieldVisible();
        setFieldNotEditable();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Entity Type";
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Type";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 80;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 10;
    }


}
