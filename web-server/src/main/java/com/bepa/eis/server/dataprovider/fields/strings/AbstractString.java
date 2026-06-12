package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

abstract public class AbstractString extends AbstractField {
    private String value;

    public AbstractString() {
    }

    public AbstractString(WebSession webSession) {
        super();
    }

    public AbstractString(String value) {
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value.trim() : null;
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.TEXT;
    }

    @Override
    public Integer getFieldMinLength() {
        return 0;
    }

    @Override
    public Integer getFieldMaxLength() {
        return null;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 35;
    }

    @Override
    public Integer getFieldRow() {
        return null;
    }

    @Override
    public Integer getFieldCol() {
        return null;
    }

    @Override
    public String toString() {
        return getValue() != null  ? getValue() : "";
    }
}