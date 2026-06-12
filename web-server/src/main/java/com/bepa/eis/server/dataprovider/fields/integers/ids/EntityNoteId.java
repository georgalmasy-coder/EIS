package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityNoteId extends AbstractId {

    public static String FIELD_NAME = "EntityNotePK";

    public EntityNoteId(Integer value) {
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

}
