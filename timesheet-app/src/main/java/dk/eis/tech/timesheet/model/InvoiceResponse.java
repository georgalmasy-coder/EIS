package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceResponse(
        int year,
        int month,
        String monthLabel,
        BigDecimal vatRate,
        BigDecimal monthHours,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal total,
        List<InvoiceTimeRow> timeRows,
        List<InvoiceMaterialRow> materialRows
) {
}
