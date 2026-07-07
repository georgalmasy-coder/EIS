package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityLinkId extends AbstractId {

    public static String FIELD_NAME = "EntityLinkPK";

    public EntityLinkId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Link Id";
    }

}
