package com.bepa.eis.server.entites.datatypes;

import com.bepa.eis.common.enums.entity.EntityElementType;

import java.time.LocalDate;

import static com.bepa.eis.common.enums.entity.EntityElementType.LOCAL_DATE;

public class LocalDateDataElement extends AbstractDataElement {

    @Override
    public EntityElementType getElementType() {
        return LOCAL_DATE;
    }

    public LocalDateDataElement(String name, String value) {
        super(name);

        if (value != null && !value.isEmpty()) {
            try {
                String str = value.length() > 10 ? value.substring(0,10) : value;
                LocalDate localDate = LocalDate.parse(str);
                setLocalDateValue(localDate);
            } catch (Exception e) {
                throw new IllegalArgumentException("LocalDate <" + name + ">is invalid : " + value);
            }
        }
    }

    public LocalDateDataElement(String name, LocalDate localDate) {
        super(name);
        setLocalDateValue(localDate);
    }

    /*
    public void save(PreparedStatement ps, MeetingRow row) throws Exception {
        if (row.getDateOfMeeting() != null) {
            ps.setDate(1, Date.valueOf(row.getDateOfMeeting()));
        } else {
            ps.setDate(1, null);
        }
    }

     */
}
