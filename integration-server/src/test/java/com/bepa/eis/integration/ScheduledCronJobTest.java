package com.bepa.eis.integration;

import com.bepa.eis.common.scheduling.CronJobSettings;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledCronJobTest {
    public static void main(String[] args) {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-09T09:39:30Z"));
        var configuration = new AtomicReference<>(settings(true, "* * * * *"));
        AtomicInteger runs = new AtomicInteger();
        AtomicReference<String> result = new AtomicReference<>("OK");
        List<String> observations = new ArrayList<>();
        var job = new ScheduledCronJob<>("test", "Test", configuration::get, value -> value, value -> {
            runs.incrementAndGet();
            if ("fail".equals(result.get())) throw new IllegalStateException("Simulated job failure");
            if ("long".equals(result.get())) { clock.time = clock.time.plusSeconds(180); return "OK"; }
            return result.get();
        }, clock, (id, status) -> observations.add(status));

        job.runIfDue(clock.instant());
        check(runs.get() == 0, "No immediate startup execution");
        tick(job, clock, "2026-10-09T09:40:00Z");
        tick(job, clock, "2026-10-09T09:40:00Z");
        check(runs.get() == 1, "Cron slot executes once");
        result.set("fail");
        tick(job, clock, "2026-10-09T09:41:00Z");
        tick(job, clock, "2026-10-09T09:41:10Z");
        check(runs.get() == 2 && observations.getLast().equals("Critical"), "Failure reported without immediate retry");
        result.set("OK");
        tick(job, clock, "2026-10-09T09:45:00Z");
        tick(job, clock, "2026-10-09T09:45:00Z");
        check(runs.get() == 3, "Late tick does not replay missed slots");
        result.set("long");
        tick(job, clock, "2026-10-09T09:46:00Z");
        job.runIfDue(clock.instant());
        check(runs.get() == 4, "Long execution schedules after completion");
        configuration.set(settings(true, "invalid"));
        tick(job, clock, "2026-10-09T09:50:00Z");
        int errors = observations.size();
        tick(job, clock, "2026-10-09T09:50:10Z");
        check(observations.size() == errors && observations.getLast().equals("Critical"), "Invalid configuration reported once");
        result.set("OK");
        configuration.set(settings(true, "* * * * *"));
        tick(job, clock, "2026-10-09T09:50:20Z");
        check(runs.get() == 4, "Recovery waits for next slot");
        tick(job, clock, "2026-10-09T09:51:00Z");
        check(runs.get() == 5, "Recovered job runs");
        configuration.set(settings(false, "* * * * *"));
        tick(job, clock, "2026-10-09T09:52:00Z");
        tick(job, clock, "2026-10-09T09:53:00Z");
        check(runs.get() == 5, "Disabled job never executes");
        configuration.set(settings(true, "*/10 * * * *"));
        result.set("Warning");
        tick(job, clock, "2026-10-09T09:54:00Z");
        tick(job, clock, "2026-10-09T09:59:00Z");
        check(runs.get() == 5, "Changed cron follows new schedule");
        tick(job, clock, "2026-10-09T10:00:00Z");
        check(runs.get() == 6 && observations.getLast().equals("Warning"), "Workflow warnings preserved");
        System.out.println("Shared cron scheduler tests passed.");
    }

    private static CronJobSettings settings(boolean enabled, String cron) {
        return new CronJobSettings(enabled, cron, ZoneId.of("Europe/Copenhagen"));
    }
    private static void tick(ScheduledCronJob<CronJobSettings> job, MutableClock clock, String instant) {
        clock.time = Instant.parse(instant); job.runIfDue(clock.instant());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static class MutableClock extends Clock {
        Instant time;
        MutableClock(Instant time) { this.time = time; }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return time; }
    }
}
