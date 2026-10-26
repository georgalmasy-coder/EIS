package com.bepa.eis.integration;

import com.bepa.eis.common.providers.misc.DataCleanupProvider;
import com.bepa.eis.common.scheduling.CleanupJobSettings;
import com.bepa.eis.common.scheduling.CleanupJobSettings.Type;
import org.slf4j.LoggerFactory;
import java.time.Instant;

public class DataCleanupJob {
    private final ScheduledCronJob<CleanupJobSettings> scheduled;

    public DataCleanupJob(Type type) {
        scheduled = new ScheduledCronJob<>(type.componentId(), type.label(),
                () -> CleanupJobSettings.load(type), CleanupJobSettings::cronJob, settings -> {
                    long deleted = new DataCleanupProvider().deleteExpired(type, settings.retentionDays());
                    LoggerFactory.getLogger(DataCleanupJob.class).info("{} completed: deletedRows={}, {}={}",
                            type.label(), deleted, type == Type.SESSION ? "inactivityMinutes" : "retentionDays",
                            type == Type.SESSION ? com.bepa.eis.common.GlobalConfiguration.getSessionTimeoutMinutes()
                                    : settings.retentionDays());
                    return "OK";
                });
    }

    public void runIfDue(Instant now) { scheduled.runIfDue(now); }
}
