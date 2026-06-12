package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
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

    private Instant nextMailQueueRunAt = Instant.EPOCH;
    private Instant nextCustomerWorkflowRunAt = Instant.EPOCH;

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
        Instant now = Instant.now();

        runMailQueueJobIfDue(now);
        runCustomerWorkflowJobIfDue(now);
    }

    private void runMailQueueJobIfDue(Instant now) {
        if (now.isBefore(nextMailQueueRunAt)) {
            return;
        }

        try {
            mailQueueJob.runOnce();
        } catch (RuntimeException e) {
            log.error("Unhandled error while running mail queue job.", e);
        } finally {
            nextMailQueueRunAt = Instant.now().plusSeconds(GlobalConfiguration.getMailQueueJobIntervalSeconds());
        }
    }

    private void runCustomerWorkflowJobIfDue(Instant now) {
        if (now.isBefore(nextCustomerWorkflowRunAt)) {
            return;
        }

        try {
            customerWorkflowJob.runOnce();
        } catch (RuntimeException e) {
            log.error("Unhandled error while running customer workflow job.", e);
        } finally {
            nextCustomerWorkflowRunAt = Instant.now().plusSeconds(GlobalConfiguration.getCustomerWorkflowJobIntervalSeconds());
        }
    }

    private void sleepUntilNextSchedulerTick() throws InterruptedException {
        TimeUnit.SECONDS.sleep(SCHEDULER_SLEEP_SECONDS);
    }

    private void logStartupInformation() {
        File configurationFile = GlobalConfiguration.getConfigurationFile();

        log.info("EIS Integration Server started.");
        log.info("Global configuration file: {}", configurationFile.getAbsolutePath());
        log.info("Mail template directory: {}", GlobalConfiguration.getMailTemplateDirectory().getAbsolutePath());

        log.info("Mail queue worker id: {}", mailQueueJob.getWorkerId());
        log.info("Mail queue job enabled: {}", GlobalConfiguration.isMailQueueJobEnabled());
        log.info("Mail queue job interval: {} seconds", GlobalConfiguration.getMailQueueJobIntervalSeconds());
        log.info("Mail queue batch size: {}", GlobalConfiguration.getMailQueueBatchSize());

        log.info("Customer workflow worker id: {}", customerWorkflowJob.getWorkerId());
        log.info("Customer workflow job enabled: {}", GlobalConfiguration.isCustomerWorkflowJobEnabled());
        log.info("Customer workflow job interval: {} seconds", GlobalConfiguration.getCustomerWorkflowJobIntervalSeconds());
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