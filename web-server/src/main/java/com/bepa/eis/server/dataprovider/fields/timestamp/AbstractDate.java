package com.bepa.eis.server.dataprovider.fields.timestamp;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

abstract public class AbstractDate extends AbstractField {
    private LocalDate value;

    public static DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("yyy-MM-dd");

    public AbstractDate() {
        setFieldNotRequired();
    }

    public AbstractDate(Timestamp timestamp) {
        LocalDate localDate = (timestamp != null) ? LocalDate.ofInstant(timestamp.toInstant(), ZoneId.systemDefault() ) : null;
        setValue(localDate);
        setFieldNotRequired();
    }

    public AbstractDate(Date date) {
        LocalDate localDate = (date != null) ? date.toLocalDate() : null;
        setValue(localDate);
        setFieldNotRequired();
    }

    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
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

    @Override
    public String toString() {
        return getValue() != null ? getValue().format(FORMATTER_DATE) : "";
    }

    @Override
    public String getFieldRequiredAsString() {
        return super.getFieldRequiredAsString();
    }

}