package com.bepa.eis.server.dataprovider.fields.binary;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

import java.util.Base64;

abstract public class AbstractBinary extends AbstractField {
    private byte[] value;

    public byte[] getBinaryValue() {
        return value;
    }

    public void setBinaryValue(byte[] value) {
        this.value = value;
    }

    public String getValue() {
        return value != null ? Base64.getEncoder().encodeToString(value) : "";
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.HIDDEN;
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
