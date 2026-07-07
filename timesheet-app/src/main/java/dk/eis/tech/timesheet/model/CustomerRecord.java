package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerRecord(
        long id,
        String companyName,
        String contactName,
        String contactEmail,
        String phoneNumber,
        String addressLine,
        String postalCode,
        String city,
        BigDecimal hourlyRate,
        BigDecimal vatRate,
        boolean inactive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
