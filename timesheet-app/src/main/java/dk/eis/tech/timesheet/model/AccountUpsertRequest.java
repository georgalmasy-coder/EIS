package dk.eis.tech.timesheet.model;

public record AccountUpsertRequest(
        String accountNumber,
        String accountName,
        String accountType,
        boolean active
) {
}
