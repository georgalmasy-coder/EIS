package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;

public class RequirementCaptureDate extends AbstractDate {

    public static String FIELD_NAME = "RequirementCaptureDate";

    public RequirementCaptureDate() {
        super ();
    }

    public RequirementCaptureDate(Timestamp timestamp) {
        super (timestamp);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Requirement Capture Date";
    }

    @Override
    public String getFieldHeaderName() {
        return "Captured";
    }

}
