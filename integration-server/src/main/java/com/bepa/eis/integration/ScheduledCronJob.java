package com.bepa.eis.integration;

import com.bepa.eis.common.providers.misc.ComponentHealthProvider;
import com.bepa.eis.common.scheduling.CronJobSettings;
import com.bepa.eis.common.scheduling.CronSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** All integration jobs share scheduling, reload, failure handling and health reporting. */
public final class ScheduledCronJob<C> {
    private static final Logger log = LoggerFactory.getLogger(ScheduledCronJob.class);
    @FunctionalInterface public interface Execution<C> { String run(C configuration) throws Exception; }

    private final String id, label;
    private final Supplier<C> configurationSupplier;
    private final Function<C, CronJobSettings> scheduling;
    private final Execution<C> execution;
    private final Clock clock;
    private final BiConsumer<String, String> health;
    private C configuration;
    private CronJobSettings settings;
    private CronSchedule schedule;
    private Instant nextRun;
    private String configurationError;
    private boolean invalidConfiguration;

    public ScheduledCronJob(String id, String label, Supplier<C> configurationSupplier,
                            Function<C, CronJobSettings> scheduling, Execution<C> execution) {
        this(id, label, configurationSupplier, scheduling, execution, Clock.systemUTC(), ComponentHealthProvider::record);
    }

    ScheduledCronJob(String id, String label, Supplier<C> configurationSupplier,
                     Function<C, CronJobSettings> scheduling, Execution<C> execution, Clock clock,
                     BiConsumer<String, String> health) {
        this.id = id; this.label = label; this.configurationSupplier = configurationSupplier;
        this.scheduling = scheduling; this.execution = execution; this.clock = clock; this.health = health;
    }

    public synchronized void runIfDue(Instant now) {
        try {
            C configured = configurationSupplier.get();
            if (configuration == null || !configured.equals(configuration)) {
                CronJobSettings configuredSettings = scheduling.apply(configured);
                CronSchedule configuredSchedule = configuredSettings.schedule();
                Instant next = configuredSchedule.nextAfter(now);
                configuration = configured;
                settings = configuredSettings;
                schedule = configuredSchedule;
                nextRun = next;
                log.info("{} configured: enabled={}, cron={}, timezone={}, nextRun={}",
                        label, settings.enabled(), settings.cron(), settings.zone(), nextRun);
            }
            invalidConfiguration = false;
            configurationError = null;
        } catch (RuntimeException e) {
            if (!invalidConfiguration || !Objects.equals(configurationError, e.getMessage())) {
                log.error("Invalid configuration for {}. Job will not run.", label, e);
                health.accept(id, "Critical");
            }
            invalidConfiguration = true;
            configurationError = e.getMessage();
            configuration = null;
            return;
        }
        if (!settings.enabled() || now.isBefore(nextRun)) return;
        try {
            health.accept(id, execution.run(configuration));
        } catch (Exception e) {
            health.accept(id, "Critical");
            log.error("{} failed.", label, e);
        } finally {
            // Skip missed slots after downtime/long executions; no immediate retries.
            Instant completed = clock.instant();
            nextRun = schedule.nextAfter(completed.isBefore(now) ? now : completed);
        }
    }
}
