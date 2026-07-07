package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryUpsertRequest(
        Long customerId,
        Long activityId,
        LocalDate entryDate,
        BigDecimal hours,
        String note
) {
}
