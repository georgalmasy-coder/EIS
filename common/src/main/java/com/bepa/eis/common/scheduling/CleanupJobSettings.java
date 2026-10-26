package com.bepa.eis.common.scheduling;

import com.bepa.eis.common.GlobalConfiguration;
import java.time.ZoneId;

public record CleanupJobSettings(boolean enabled, String cron, ZoneId zone, int retentionDays) {
    public enum Type {
        SESSION("session", "SESSION", "LastAccessed", 1),
        INCIDENTS("incidents", "INCIDENTS", "LogCreated", 120),
        PERFORMANCE("performance", "PERFORMANCE", "Created", 120);

        public final String key, table, dateColumn;
        public final int defaultDays;
        Type(String key, String table, String dateColumn, int defaultDays) {
            this.key = key; this.table = table; this.dateColumn = dateColumn; this.defaultDays = defaultDays;
        }
        public String componentId() { return "job.cleanup." + key; }
        public String label() { return table + " cleanup job"; }
    }

    public static CleanupJobSettings load(Type type) {
        return load(type.key, type.defaultDays);
    }

    public static CleanupJobSettings load(String key, int defaultDays) {
        String prefix = "cleanup." + key + ".job.";
        CronJobSettings job = CronJobSettings.load("cleanup." + key + ".job",
                "session".equals(key) ? "*/15 * * * *" : "0 2 * * *");
        if (!job.enabled()) {
            return new CleanupJobSettings(false, job.cron(), job.zone(), defaultDays);
        }
        int days = "session".equals(key) ? defaultDays
                : Integer.parseInt(GlobalConfiguration.getString(prefix + "retention.days", String.valueOf(defaultDays)));
        if (days < 1 || days > 36500) throw new IllegalArgumentException(prefix + "retention.days must be between 1 and 36500");
        return new CleanupJobSettings(true, job.cron(), job.zone(), days);
    }

    public CronSchedule schedule() { return new CronSchedule(cron, zone); }
    public CronJobSettings cronJob() { return new CronJobSettings(enabled, cron, zone); }
}
