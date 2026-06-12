package com.bepa.eis.server.dataprovider.fields.integers;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

abstract public class AbstractInteger extends AbstractField {
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.NUMBER;
    }

    @Override
    public Integer getFieldMinLength() {
        return 0;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 15;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 15;
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
        return getValue() != null  ? getValue().toString() : "";
    }
}