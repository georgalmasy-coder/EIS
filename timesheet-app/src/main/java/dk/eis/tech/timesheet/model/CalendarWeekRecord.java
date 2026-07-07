package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;

public record CalendarWeekRecord(
        int weekNumber,
        BigDecimal hours
) {
}
