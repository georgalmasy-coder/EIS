package dk.eis.tech.timesheet.model;

public record ActivityUpsertRequest(
        String shortDescription,
        String longDescription,
        boolean inactive
) {
}
