package com.bepa.eis.server.dataprovider.fields.strings.phone;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;

abstract public class AbstractPhone extends AbstractString {
    private String value;

    public AbstractPhone() {
    }

    public AbstractPhone(WebSession webSession) {
        super();
    }

    public AbstractPhone(String value) {
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
        return FieldControl.PHONE;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 20;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 20;
    }

    @Override
    public String toString() {
        return getValue() != null  ? getValue() : "";
    }
}