package com.bepa.eis.server.dataprovider.fields.bigdecimals;

import java.math.BigDecimal;

public class BudgetInValue extends AbstractBigDecimals {

    public static String FIELD_NAME = "BudgetInValue";

    public BudgetInValue(BigDecimal value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Budget Value";
    }

    @Override
    public String getFieldHeaderName() {
        return "Budget Value";
    }

    @Override
    public String toString() {
        return getValue() != null ? getValue().toString() : "";
    }

}
