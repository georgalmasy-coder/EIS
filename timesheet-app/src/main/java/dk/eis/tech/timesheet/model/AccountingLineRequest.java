package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;

public record AccountingLineRequest(
        Long accountId,
        String lineText,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String vatCode
) {
}
