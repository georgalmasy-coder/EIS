package com.bepa.eis.server.dataprovider.fields.booleans;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

abstract public class AbstractBoolean extends AbstractField {
    private Boolean value;

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
        setFieldNotRequired();
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.CHECKBOX;
    }

    @Override
    public Integer getFieldMinLength() {
        return null;
    }

    @Override
    public Integer getFieldMaxLength() {
        return null;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return null;
    }

    @Override
    public Integer getFieldRow() {
        return null;
    }

    @Override
    public Integer getFieldCol() {
        return null;
    }

}
