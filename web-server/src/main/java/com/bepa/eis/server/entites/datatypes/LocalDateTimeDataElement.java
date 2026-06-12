package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.common.enums.entity.EntityElementType;

import java.time.LocalDateTime;

import static com.bepa.eis.common.enums.entity.EntityElementType.LOCAL_DATETIME;

public class LocalDateTimeDataElement extends AbstractDataElement {

    @Override
    public EntityElementType getElementType() {
        return LOCAL_DATETIME;
    }

    public LocalDateTimeDataElement(String name, String value) {
        super(name);

        if (value != null) {
            LocalDateTime datetimeValue = LocalDateTime.parse(value);
            setLocalDateTimeValue(datetimeValue);
        }

    }

    /*
    public void save(PreparedStatement ps, SystemBreakdownRow row) throws Exception {
        LocalDateTime value = row.getDateOfChange();

        if (value != null) {
            ps.setTimestamp(1, Timestamp.valueOf(value));
        } else {
            ps.setTimestamp(1, null);
        }
    }


     */
}
