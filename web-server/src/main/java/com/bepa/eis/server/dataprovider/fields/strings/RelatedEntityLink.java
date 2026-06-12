package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.server.api.web.application.enums.FieldControl;

public class RelatedEntityLink extends AbstractString {

    public static String FIELD_NAME = "Link";

    public RelatedEntityLink() {
        super(FIELD_NAME);
        setFieldNotVisible();
        setFieldNotEditable();
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
    public FieldControl getFieldControl() {
        return FieldControl.HIDDEN;
    }

    @Override
    public String getSortKey() {
        return getValue();
    }
}
