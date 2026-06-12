package com.bepa.eis.server.dataprovider.fields.timestamp;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

abstract public class AbstractDateTime extends AbstractField {
    private LocalDateTime value;

    private static DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss");

    public AbstractDateTime(Timestamp timestamp) {
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
        return FieldControl.DATETIME;
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

    @Override
    public String toString() {
        return  getValue() != null ? getValue().format(FORMATTER_DATE_TIME) : "";
    }

}