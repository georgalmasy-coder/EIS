package com.bepa.eis.server.dataprovider.fields.integers;

import com.bepa.eis.server.api.web.application.enums.FieldEditable;
import com.bepa.eis.server.api.web.application.enums.FieldRequired;

public class BudgetInDays extends AbstractInteger {

    public static String FIELD_NAME = "BudgetInDays";

    public BudgetInDays(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Budget Days";
    }

    @Override
    public String getFieldHeaderName() {
        return "Budget Days";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
