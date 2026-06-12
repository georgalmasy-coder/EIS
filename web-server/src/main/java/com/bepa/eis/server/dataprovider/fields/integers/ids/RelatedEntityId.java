package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class RelatedEntityId extends AbstractId {

    public static String FIELD_NAME = "RelatedEntityId";

    public RelatedEntityId(Integer value) {
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
