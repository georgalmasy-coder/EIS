package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class CreatedDateTime extends AbstractDateTime {

    public static String FIELD_NAME = "CreatedTime";

    public CreatedDateTime(Timestamp timestamp) {
        super (timestamp);
        setFieldEditable();
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Created";
    }

    @Override
    public String getFieldHeaderName() {
        return "Created";
    }

}
