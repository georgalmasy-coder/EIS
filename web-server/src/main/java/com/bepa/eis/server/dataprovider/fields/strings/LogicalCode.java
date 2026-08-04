package com.bepa.eis.server.dataprovider.fields.strings;

import com.bepa.eis.common.enums.entity.EntityType;

public class LogicalCode extends AbstractString {

    public static String FIELD_NAME = "LogicalCode";

    private boolean isNew = false;

    public LogicalCode() {
        setFieldRequired();
    }

    public LogicalCode(boolean isNew) {
        this.isNew = isNew;
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return isNew ? "Temporary ID" : "ID";
    }

    @Override
    public String getFieldHeaderName() {
        return isNew ? "Temporary ID" : "ID";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 25;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @Override
    public String getSortKey() {
        return getValue();
    }

    @Override
    public void setValue(String value) {
        if (value != null) {
            if (!value.startsWith(EntityType.LOGICAL_STRUCTURE.getIdPrefix())) {
                value = EntityType.LOGICAL_STRUCTURE.getIdPrefix() + value;
            }
            super.setValue(value);
        }
    }
}
