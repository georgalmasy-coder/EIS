package com.bepa.eis.server.dataprovider.fields.integers.ids;

public class NotificationId extends AbstractId {

    public static String FIELD_NAME = "NotificationId";

    public NotificationId(Integer value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldHeaderName() {
        return "Notification ID";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

}
