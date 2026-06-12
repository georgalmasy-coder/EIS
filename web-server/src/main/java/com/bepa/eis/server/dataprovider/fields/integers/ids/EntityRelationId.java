package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityRelationId extends AbstractId {

    public static String FIELD_NAME = "EntityRelationPK";

    public EntityRelationId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Relation Id";
    }

}
