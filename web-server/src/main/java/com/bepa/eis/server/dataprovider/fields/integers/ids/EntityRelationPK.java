package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityRelationPK extends AbstractId {

    public static String FIELD_NAME = "EntityRelationPK";

    public EntityRelationPK(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Relation PK";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
