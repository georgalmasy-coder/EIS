package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceMaterialRow(
        long id,
        LocalDate entryDate,
        BigDecimal quantity,
        String unit,
        String shortDescription,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
