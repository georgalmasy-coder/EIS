package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;

public record AccountingLineRecord(
        long id,
        long entryId,
        long accountId,
        String accountNumber,
        String accountName,
        String lineText,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String vatCode
) {
}
