package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class EntityAttachmentId extends AbstractId {

    public static String FIELD_NAME = "EntityAttachmentPK";

    public EntityAttachmentId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Entity Attachment Id";
    }

}
