package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;

public class ChangedDateTime extends AbstractDateTime {

    public static String FIELD_NAME = "ChangedDateTime";

    public ChangedDateTime(Timestamp timestamp) {
        super (timestamp);
        setFieldEditable();
        setFieldRequired();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Changed";
    }

    @Override
    public String getFieldHeaderName() {
        return "Changed";
    }

}
