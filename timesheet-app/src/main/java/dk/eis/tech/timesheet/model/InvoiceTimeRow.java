package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;

public record InvoiceTimeRow(
        long activityId,
        String activityShortDescription,
        BigDecimal hours,
        BigDecimal rate,
        BigDecimal amount
) {
}
