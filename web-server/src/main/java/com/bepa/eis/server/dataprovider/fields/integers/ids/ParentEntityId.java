package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class ParentEntityId extends AbstractId {

    public static String FIELD_NAME = "ParentEntityId";

    public ParentEntityId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent Entity Id";
    }

}
