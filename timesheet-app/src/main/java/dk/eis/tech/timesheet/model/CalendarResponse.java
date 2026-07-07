package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.util.List;

public record CalendarResponse(
        int year,
        int month,
        String monthLabel,
        BigDecimal monthHours,
        List<CalendarDayRecord> days,
        List<CalendarWeekRecord> weekTotals,
        List<MaterialEntryRecord> materials
) {
}
