package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;

public class DeadlineNextTRL extends AbstractDate {

    public static String FIELD_NAME = "DeadlineNextTRL";

    public DeadlineNextTRL() {
        super ();
    }

    public DeadlineNextTRL(Timestamp timestamp) {
        super (timestamp);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Deadline Next TRL";
    }

    @Override
    public String getFieldHeaderName() {
        return "Deadline Next TRL";
    }

}
