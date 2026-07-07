package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialEntryUpsertRequest(
        Long customerId,
        LocalDate entryDate,
        BigDecimal quantity,
        String unit,
        String shortDescription,
        BigDecimal unitPrice
) {
}
