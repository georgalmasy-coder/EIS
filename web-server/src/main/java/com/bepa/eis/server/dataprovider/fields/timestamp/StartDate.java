package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

public class StartDate extends AbstractDate {

    public static String FIELD_NAME = "StartDate";

    public StartDate(Timestamp timestamp) {
        super (timestamp);
        setFieldEditable();
    }

    public StartDate(LocalDate localDate) {
        super ();
        if (localDate != null) {
            setValue(localDate);
        }
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Start Date";
    }

    @Override
    public String getFieldHeaderName() {
        return "Start Date";
    }


}
