package dk.eis.tech.timesheet.model;

import java.time.LocalDateTime;

public record AccountRecord(
        long id,
        String accountNumber,
        String accountName,
        String accountType,
        String systemKey,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
