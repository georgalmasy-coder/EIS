package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeEntryRecord(
        long id,
        long customerId,
        long activityId,
        LocalDate entryDate,
        BigDecimal hours,
        String note,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String activityShortDescription,
        String activityLongDescription
) {
}
