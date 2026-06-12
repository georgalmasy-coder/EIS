package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.server.api.web.application.enums.FieldControl;

public class Notes extends AbstractString {

    public static String FIELD_NAME = "Notes";

    public Notes(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Notes";
    }

    @Override
    public String getFieldHeaderName() {
        return "Notes";
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.TEXTAREA;
    }

    @Override
    public Integer getFieldRow() {
        return 3;
    }

    @Override
    public Integer getFieldCol() {
        return 35;
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue() : "";
    }

}
