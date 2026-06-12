package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;

public class DeadlineFinalized extends AbstractDate {

    public static String FIELD_NAME = "DeadlineFinalized";

    public DeadlineFinalized() {
        super ();
    }

    public DeadlineFinalized(Timestamp timestamp) {
        super (timestamp);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Deadline Finalized";
    }

    @Override
    public String getFieldHeaderName() {
        return "Deadline Finalized";
    }

}
