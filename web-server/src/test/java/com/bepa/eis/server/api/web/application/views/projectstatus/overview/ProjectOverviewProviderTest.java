package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.server.api.DTO.TrlRecord;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/** Standalone regression suite; run its main method without a test framework. */
public final class ProjectOverviewProviderTest {
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 10, 12);
        List<TrlRecord> records = List.of(
                record(1, today.minusDays(1)),
                record(2, today.minusYears(1)),
                record(3, today),
                record(4, today.plusDays(1)),
                record(5, null),
                record(1, today.minusDays(1))
        );

        check(records, today, 2);
        check(List.of(), today, 0);
        check(null, today, 0);
        check(List.of(record(1, today), record(2, null)), today, 0);
        System.out.println("ProjectOverviewProviderTest passed");
    }

    private static TrlRecord record(int entityId, LocalDate deadline) {
        return new TrlRecord(1, 1, entityId, 1,
                deadline == null ? null : Timestamp.valueOf(deadline.atStartOfDay()));
    }

    private static void check(List<TrlRecord> records, LocalDate today, int expected) {
        int actual = ProjectOverviewProvider.countOverduePhysicalStructures(records, today);
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " overdue PSYS, got " + actual);
        }
    }
}
