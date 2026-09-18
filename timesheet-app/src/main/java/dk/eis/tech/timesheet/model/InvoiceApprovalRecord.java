package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceApprovalRecord(
        long id,
        long customerId,
        int invoiceYear,
        int invoiceMonth,
        String invoiceNumber,
        LocalDate invoiceDate,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal total,
        long accountingEntryId,
        LocalDateTime createdAt
) {
}
