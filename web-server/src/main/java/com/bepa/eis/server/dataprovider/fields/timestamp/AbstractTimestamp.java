package com.bepa.eis.server.dataprovider.fields.timestamp;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

abstract public class AbstractTimestamp extends AbstractField {
    private LocalDateTime value;

    public AbstractTimestamp(Timestamp timestamp) {
        LocalDateTime localDateTime = (timestamp == null) ? null : timestamp.toLocalDateTime();
        setValue(localDateTime);
    }

    public LocalDateTime getValue() {
        return value;
    }

    public void setValue(LocalDateTime value) {
        this.value = value;
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.DATE;
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
        return 25;
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