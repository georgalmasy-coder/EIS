package com.bepa.eis.server.dataprovider.entities.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EntityElementRecord {
    private Integer entityDataElementType;
    private Integer integerValue;
    private Double doubleValue;
    private BigDecimal currencyValue;
    private String stringValue;
    private LocalDate localDateValue;
    private Timestamp localDateTimeValue;
    private Boolean booleanValue;

    public EntityElementRecord(Integer entityDataElementTyp,
                               Integer integerValue,
                               Double doubleValue,
                               BigDecimal currencyValue,
                               String stringValue,
                               LocalDate localDateValue,
                               Timestamp localDateTimeValue,
                               Boolean booleanValue) {
        this.entityDataElementType = entityDataElementTyp;
        this.integerValue = integerValue;
        this.doubleValue = doubleValue;
        this.currencyValue = currencyValue;
        this.stringValue = stringValue;
        this.localDateValue = localDateValue;
        this.localDateTimeValue = localDateTimeValue;
        this.booleanValue = booleanValue;
    }

    public Integer getEntityDataElementType() {
        return entityDataElementType;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public BigDecimal getCurrencyValue() {
        return currencyValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public LocalDate getLocalDateValue() {
        return localDateValue;
    }

    public LocalDateTime getLocalDateTimeValue() {
        return localDateTimeValue != null ? localDateTimeValue.toLocalDateTime() : null;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

}


