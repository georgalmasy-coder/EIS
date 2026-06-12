package com.bepa.eis.server.dataprovider.fields.bigdecimals;

import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.api.web.application.enums.FieldRequired;
import com.bepa.eis.server.dataprovider.fields.AbstractField;

import java.math.BigDecimal;

abstract public class AbstractBigDecimals extends AbstractField {

    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.DECIMAL;
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
        return 15;
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
