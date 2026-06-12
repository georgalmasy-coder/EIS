package com.bepa.eis.server.dataprovider.fields.timestamp;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class AcknowledgeDateTime extends AbstractDateTime {

    public static String FIELD_NAME = "AcknowledgeTime";

    public AcknowledgeDateTime(Timestamp timestamp) {
        super (timestamp);
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Acknowledged";
    }

    @Override
    public String getFieldHeaderName() {
        return "Acknowledged";
    }

}
