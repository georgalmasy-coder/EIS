package com.bepa.eis.common.scheduling;

import java.time.*;

public class CronScheduleTest {
    public static void main(String[] args) {
        ZoneId zone = ZoneId.of("Europe/Copenhagen");
        check("0 2 * * *", "2026-10-09T00:00:00Z", "2026-10-10T00:00:00Z", zone);
        check("0 2 * * *", "2026-10-08T23:59:59Z", "2026-10-09T00:00:00Z", zone);
        check("*/15 8-10 * * 1-5", "2026-10-09T06:16:00Z", "2026-10-09T06:30:00Z", zone);
        check("0 2 * * 0", "2026-10-09T00:00:00Z", "2026-10-11T00:00:00Z", zone);
        check("0 2 * * 7", "2026-10-09T00:00:00Z", "2026-10-11T00:00:00Z", zone);
        check("0 2 * * *", "2026-03-28T01:00:00Z", "2026-03-30T00:00:00Z", zone);
        check("0 2 * * *", "2026-10-25T00:00:00Z", "2026-10-26T01:00:00Z", zone);
        check("* * * * *", "2026-10-25T00:59:00Z", "2026-10-25T01:00:00Z", zone);
        check("*/10 * * * *", "2026-10-25T00:59:00Z", "2026-10-25T01:00:00Z", zone);
        check("0 2 29 2 *", "2026-10-09T00:00:00Z", "2028-02-29T01:00:00Z", zone);
        check("0 2 1 * 1", "2026-10-09T00:00:00Z", "2026-10-12T00:00:00Z", zone);
        check("10,40 2 * * *", "2026-10-09T00:11:00Z", "2026-10-09T00:40:00Z", zone);
        for (String invalid : new String[]{"0 0 2 * * *", "60 2 * * *", "*/0 2 * * *", "0 25 * * *", "0 2 * * 8", "0 2 31 2 *"}) {
            try {
                new CronSchedule(invalid, zone).nextAfter(Instant.parse("2026-10-09T00:00:00Z"));
                throw new AssertionError("Accepted invalid cron: " + invalid);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("Cron schedule tests passed.");
    }

    private static void check(String cron, String after, String expected, ZoneId zone) {
        Instant actual = new CronSchedule(cron, zone).nextAfter(Instant.parse(after));
        if (!actual.equals(Instant.parse(expected))) throw new AssertionError(cron + ": " + actual + " != " + expected);
    }
}
