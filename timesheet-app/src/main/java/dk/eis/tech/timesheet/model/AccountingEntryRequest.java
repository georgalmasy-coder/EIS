package dk.eis.tech.timesheet.model;

import java.time.LocalDate;
import java.util.List;

public record AccountingEntryRequest(
        LocalDate entryDate,
        String entryType,
        String description,
        String referenceType,
        String referenceId,
        Long correctionOfEntryId,
        List<AccountingLineRequest> lines
) {
}
