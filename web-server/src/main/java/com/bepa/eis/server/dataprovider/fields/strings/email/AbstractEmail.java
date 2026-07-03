package com.bepa.eis.server.dataprovider.fields.strings.email;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;

abstract public class AbstractEmail extends AbstractString {
    private String value;

    public AbstractEmail() {
    }

    public AbstractEmail(WebSession webSession) {
        super();
    }

    public AbstractEmail(String value) {
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
        return FieldControl.EMAIL;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 50;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 50;
    }

    @Override
    public String toString() {
        return getValue() != null  ? getValue() : "";
    }
}