package dk.eis.tech.timesheet.model;

import java.time.LocalDateTime;

public record ActivityRecord(
        long id,
        long customerId,
        String shortDescription,
        String longDescription,
        boolean inactive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
