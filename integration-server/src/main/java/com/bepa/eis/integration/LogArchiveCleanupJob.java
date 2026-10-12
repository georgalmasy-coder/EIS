package com.bepa.eis.integration;

import com.bepa.eis.common.providers.misc.LogArchiveCleanup;
import com.bepa.eis.common.scheduling.LogCleanupSettings;
import org.slf4j.LoggerFactory;
import java.time.Instant;

public final class LogArchiveCleanupJob {
    private final ScheduledCronJob<LogCleanupSettings> scheduled = new ScheduledCronJob<>(
            LogCleanupSettings.COMPONENT_ID, LogCleanupSettings.LABEL, LogCleanupSettings::load,
            configuration -> configuration.job().cronJob(), configuration -> {
                long deleted = LogArchiveCleanup.deleteExpired(configuration.directories(),
                        configuration.job().retentionDays(), Instant.now());
                LoggerFactory.getLogger(LogArchiveCleanupJob.class).info("Log cleanup completed: deletedFiles={}, retentionDays={}, directories={}",
                        deleted, configuration.job().retentionDays(), configuration.directories());
                return "OK";
            });

    public void runIfDue(Instant now) { scheduled.runIfDue(now); }
}