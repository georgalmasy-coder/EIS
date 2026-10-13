package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.misc.ComponentHealthProvider;
import com.bepa.eis.common.scheduling.CronJobSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.util.List;
import com.bepa.eis.common.scheduling.CleanupJobSettings.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IntegrationServerApplication {

    private static final Logger log = LoggerFactory.getLogger(IntegrationServerApplication.class);

    private static final long SCHEDULER_SLEEP_SECONDS = 1L;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CountDownLatch stopped = new CountDownLatch(1);

    private final MailQueueJob mailQueueJob = new MailQueueJob();
    private final CustomerWorkflowJob customerWorkflowJob = new CustomerWorkflowJob();
    private final IntegrationDatabaseInstaller databaseInstaller = new IntegrationDatabaseInstaller();
    private final LogArchiveCleanupJob logArchiveCleanupJob = new LogArchiveCleanupJob();
    private final List<DataCleanupJob> cleanupJobs = List.of(
            new DataCleanupJob(Type.SESSION), new DataCleanupJob(Type.INCIDENTS), new DataCleanupJob(Type.PERFORMANCE));

    private final ScheduledCronJob<CronJobSettings> scheduledMail = new ScheduledCronJob<>(
            ComponentHealthProvider.MAIL_JOB, "Mail queue job",
            GlobalConfiguration::getMailQueueJobSchedule, settings -> settings, settings -> {
                mailQueueJob.runOnce();
                return "OK";
            });
    private final ScheduledCronJob<CronJobSettings> scheduledWorkflow = new ScheduledCronJob<>(
            ComponentHealthProvider.WORKFLOW_JOB, "Customer workflow job",
            GlobalConfiguration::getCustomerWorkflowJobSchedule, settings -> settings, settings -> {
                customerWorkflowJob.runOnce();
                return customerWorkflowJob.getLastRunHealthStatus();
            });
    public static void main(String[] args) {
        IntegrationServerApplication application = new IntegrationServerApplication();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received. Stopping EIS Integration Server...");
            application.stop();
            application.awaitStopped();
        }, "eis-integration-server-shutdown-hook"));

        application.run();
    }

    private void run() {
        logStartupInformation();
        installDatabaseObjects();

        try {
            while (running.get()) {
                runScheduledJobs();
                sleepUntilNextSchedulerTick();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("EIS Integration Server interrupted.");
        } finally {
            log.info("EIS Integration Server stopped.");
            stopped.countDown();
        }
    }

    private void installDatabaseObjects() {
        try {
            databaseInstaller.installIfAvailable();
        } catch (RuntimeException e) {
            log.error("Database installation failed during startup.", e);
        }
    }

    private void runScheduledJobs() {
        scheduledMail.runIfDue(Instant.now());
        scheduledWorkflow.runIfDue(Instant.now());
        for (DataCleanupJob job : cleanupJobs) job.runIfDue(Instant.now());
        logArchiveCleanupJob.runIfDue(Instant.now());
    }
    private void sleepUntilNextSchedulerTick() throws InterruptedException {
        TimeUnit.SECONDS.sleep(SCHEDULER_SLEEP_SECONDS);
    }

    private void logStartupInformation() {
        File configurationFile = GlobalConfiguration.getConfigurationFile();

        log.info("EIS Integration Server started.");
        log.info("Global configuration file: {}", configurationFile.getAbsolutePath());

        log.info("Mail queue worker id: {}", mailQueueJob.getWorkerId());
        log.info("Mail queue job enabled: {}", GlobalConfiguration.isMailQueueJobEnabled());

        log.info("Mail queue batch size: {}", GlobalConfiguration.getMailQueueBatchSize());

        log.info("Customer workflow worker id: {}", customerWorkflowJob.getWorkerId());
        log.info("Customer workflow job enabled: {}", GlobalConfiguration.isCustomerWorkflowJobEnabled());

        log.info("Customer workflow portal base URL: {}", GlobalConfiguration.getCustomerWorkflowPortalBaseUrl());
    }

    private void stop() {
        running.set(false);
    }

    private void awaitStopped() {
        try {
            if (!stopped.await(30, TimeUnit.SECONDS)) {
                log.warn("EIS Integration Server did not stop within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for EIS Integration Server to stop.");
        }
    }
}
