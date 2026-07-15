package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityId extends AbstractId {

    public static String FIELD_NAME = "EntityId";

    public EntityId() {
    }

    public EntityId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Id";
    }

    public boolean isBlankOrEmpty() {
        return getValue() == null || getValue() == 0;
    }

}
