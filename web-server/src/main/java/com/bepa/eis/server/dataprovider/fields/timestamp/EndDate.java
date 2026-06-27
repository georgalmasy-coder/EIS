package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EndDate extends AbstractDate {

    public static String FIELD_NAME = "EndDate";

    public EndDate(Timestamp timestamp) {
        super (timestamp);
    }

    public EndDate(LocalDate localDate) {
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
        return "End Date";
    }

    @Override
    public String getFieldHeaderName() {
        return "End Date";
    }

}
