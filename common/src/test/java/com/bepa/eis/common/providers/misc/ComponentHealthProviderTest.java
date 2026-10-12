package com.bepa.eis.common.providers.misc;

import java.time.LocalDateTime;
import java.util.List;

public class ComponentHealthProviderTest {
    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 9, 12, 0);
        var job = new ComponentHealthProvider.Definition("job", "Job", true, 300);
        var integration = new ComponentHealthProvider.Definition("integration", "Integration", false, 86400);
        check("OK".equals(ComponentHealthProvider.evaluate(job, "OK", now.minusSeconds(300), now).status()), "Tolerance boundary");
        var stopped = ComponentHealthProvider.evaluate(job, "OK", now.minusSeconds(301), now);
        check("Critical".equals(stopped.status()), "Stopped job is critical, not healthy or unknown");
        var oldIntegration = ComponentHealthProvider.evaluate(integration, "Critical", now.minusDays(2), now);
        check("Unknown".equals(oldIntegration.status()), "Old integration results expire");
        var healthy = ComponentHealthProvider.evaluate(job, "OK", now, now);
        var warning = ComponentHealthProvider.evaluate(job, "Warning", now, now);
        check(ComponentHealthProvider.score(List.of(healthy, warning, stopped)) == 50, "Equal weighting: 100 + 50 + 0 / 3");
        check(ComponentHealthProvider.score(List.of(healthy, oldIntegration)) == 100, "Unknown excluded from score");
        check(ComponentHealthProvider.score(List.of(oldIntegration)) == null, "Only unknown must not become 0 percent");
        check(ComponentHealthProvider.score(List.of()) == null, "No observations must not become 100 percent");
        check(ComponentHealthProvider.score(List.of(stopped)) == 0, "Only critical is a real 0 percent");
        check(ComponentHealthProvider.score(List.of(healthy, healthy, stopped)) == 67, "Rounded average");
        var cleanup = new ComponentHealthProvider.Definition("cleanup", "Cleanup", true, 0,
                "0 2 * * *", java.time.ZoneId.of("Europe/Copenhagen"));
        LocalDateTime lastCleanup = LocalDateTime.of(2026, 10, 8, 2, 1);
        check("OK".equals(ComponentHealthProvider.evaluate(cleanup, "OK", lastCleanup,
                LocalDateTime.of(2026, 10, 9, 2, 4), java.time.Instant.parse("2026-10-09T00:04:00Z")).status()), "Cleanup cron grace period");
        check("Critical".equals(ComponentHealthProvider.evaluate(cleanup, "OK", lastCleanup,
                LocalDateTime.of(2026, 10, 9, 2, 6), java.time.Instant.parse("2026-10-09T00:06:00Z")).status()), "Missed daily cleanup becomes critical");
        System.out.println("Component health tests passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
