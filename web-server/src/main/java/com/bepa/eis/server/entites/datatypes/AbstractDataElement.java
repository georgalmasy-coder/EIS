package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.server.entites.configuration.EntityConfiguration;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

abstract public class AbstractDataElement {

    private static final Logger log = LoggerFactory.getLogger(AbstractDataElement.class);

    private String name;
    private EntityElementType elementType;
    private Integer integerValue;
    private String stringValue;
    private Double doubleValue;
    private BigDecimal currencyValue;
    private LocalDate localDateValue;
    private LocalDateTime localDateTimeValue;
    private Boolean booleanValue;
    private EntityDataElement entityDataElement;

    public AbstractDataElement(String name) {
        setName(name);
    }

    public abstract EntityElementType getElementType();

    private void setName(String name) {
        this.name = name;
        this.entityDataElement = EntityConfiguration.getInstance().getEntityDataElementByFieldName(this.name);

        if (this.entityDataElement == null) {
            log.error("EntityDataElement is null for field: {}", this.name);
            throw new RuntimeException("EntityDataElement is null for field: " + this.name);
        }

    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setLocalDateValue(LocalDate localDateValue) {
        this.localDateValue = localDateValue;
    }

    public LocalDate getLocalDateValue() {
        return localDateValue;
    }

    public void setLocalDateTimeValue(LocalDateTime localDateTimeValue) {
        this.localDateTimeValue = localDateTimeValue;
    }

    public LocalDateTime getLocalDateTimeValue() {
        return localDateTimeValue;
    }

    public void setCurrencyValue(BigDecimal currencyValue) {
        this.currencyValue = currencyValue;
    }

    public BigDecimal getCurrencyValue() {
        return currencyValue;
    }


    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setElementType(EntityElementType elementType) {
        this.elementType = elementType;
    }

    public EntityDataElement getEntityDataElement() {
        if (entityDataElement == null) {
            System.out.println("EntityDataElement is null");
        }
        return entityDataElement;
    }
}
