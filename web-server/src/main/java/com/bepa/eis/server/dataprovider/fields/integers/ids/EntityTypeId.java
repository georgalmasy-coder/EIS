package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityTypeId extends AbstractId {

    public static String FIELD_NAME = "EntityType";

    public EntityTypeId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Type";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
