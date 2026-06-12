package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class NotificationType extends AbstractId {

    public static String FIELD_NAME = "NotificationType";

    public NotificationType(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Notification Type";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
