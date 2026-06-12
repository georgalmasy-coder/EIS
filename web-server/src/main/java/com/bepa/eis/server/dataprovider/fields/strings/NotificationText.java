package com.bepa.eis.server.dataprovider.fields.strings;

public class NotificationText extends AbstractString {

    public static String FIELD_NAME = "NotificationText";

    public NotificationText(String value) {
        setValue(value);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Notification";
    }

    @Override
    public String getFieldHeaderName() {
        return "Notification";
    }

    @Override
    public Integer getFieldMinLength() {
        return 1;
    }

    @Override
    public Integer getFieldMaxLength() {
        return 100;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
