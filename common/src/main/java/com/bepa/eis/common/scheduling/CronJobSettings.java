package com.bepa.eis.common.scheduling;

import com.bepa.eis.common.GlobalConfiguration;
import java.time.ZoneId;

public record CronJobSettings(boolean enabled, String cron, ZoneId zone) {
    public static CronJobSettings load(String prefix, String defaultCron) {
        String enabled = GlobalConfiguration.getString(prefix + ".enabled", "true");
        if (!"true".equalsIgnoreCase(enabled) && !"false".equalsIgnoreCase(enabled)) {
            throw new IllegalArgumentException(prefix + ".enabled must be true or false");
        }
        if ("false".equalsIgnoreCase(enabled)) {
            return new CronJobSettings(false, defaultCron, ZoneId.of("Europe/Copenhagen"));
        }
        String cron = GlobalConfiguration.getString(prefix + ".cron", defaultCron);
        ZoneId zone = ZoneId.of(GlobalConfiguration.getString(prefix + ".timezone", "Europe/Copenhagen"));
        new CronSchedule(cron, zone).nextAfter(java.time.Instant.now());
        return new CronJobSettings(true, cron, zone);
    }

    public CronSchedule schedule() { return new CronSchedule(cron, zone); }
}
