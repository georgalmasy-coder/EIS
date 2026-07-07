package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MaterialEntryRecord(
        long id,
        long customerId,
        LocalDate entryDate,
        BigDecimal quantity,
        String unit,
        String shortDescription,
        BigDecimal unitPrice,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
