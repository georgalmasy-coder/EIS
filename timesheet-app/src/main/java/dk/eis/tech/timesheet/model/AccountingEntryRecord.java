package dk.eis.tech.timesheet.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AccountingEntryRecord(
        long id,
        LocalDate entryDate,
        int accountingYear,
        int accountingMonth,
        String entryType,
        String description,
        String referenceType,
        String referenceId,
        Long correctionOfEntryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AccountingLineRecord> lines
) {
}
