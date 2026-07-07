package dk.eis.tech.timesheet.model;

import java.math.BigDecimal;

public record CustomerUpsertRequest(
        String companyName,
        String contactName,
        String contactEmail,
        String phoneNumber,
        String addressLine,
        String postalCode,
        String city,
        BigDecimal hourlyRate,
        boolean inactive
) {
}
