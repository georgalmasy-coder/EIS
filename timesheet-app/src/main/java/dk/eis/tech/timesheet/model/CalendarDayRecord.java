package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalendarDayRecord(
        LocalDate date,
        int dayOfMonth,
        BigDecimal hours,
        List<TimeEntryRecord> entries
) {
}
