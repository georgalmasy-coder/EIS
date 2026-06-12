package com.bepa.eis.server.dataprovider.fields.integers.ids;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;

abstract public class AbstractId extends AbstractInteger {

    public AbstractId() {
        setFieldNotVisible();
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.HIDDEN;
    }

    @Override
    public String getFieldLabelName() {
        return null;
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

}
