package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.customer.CustomerWorkflowMaintenanceProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowMaintenanceProvider.CustomerWorkflowMaintenanceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomerWorkflowJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowJob.class);

    private final String workerId;
    private final AtomicBoolean running;
    private final CustomerWorkflowProcessor customerWorkflowProcessor;
    private final CustomerWorkflowMaintenanceProvider maintenanceProvider;

    public CustomerWorkflowJob() {
        this(buildDefaultWorkerId());
    }

    public CustomerWorkflowJob(String workerId) {
        this.workerId = normalizeWorkerId(workerId);
        this.running = new AtomicBoolean(false);
        this.customerWorkflowProcessor = new CustomerWorkflowProcessor(this.workerId);
        this.maintenanceProvider = new CustomerWorkflowMaintenanceProvider(null);
    }

    public CustomerWorkflowJob(
            String workerId,
            CustomerWorkflowProcessor customerWorkflowProcessor
    ) {
        this.workerId = normalizeWorkerId(workerId);
        this.running = new AtomicBoolean(false);
        this.customerWorkflowProcessor = customerWorkflowProcessor == null
                ? new CustomerWorkflowProcessor(this.workerId)
                : customerWorkflowProcessor;
        this.maintenanceProvider = new CustomerWorkflowMaintenanceProvider(null);
    }

    public CustomerWorkflowJob(
            String workerId,
            CustomerWorkflowProcessor customerWorkflowProcessor,
            CustomerWorkflowMaintenanceProvider maintenanceProvider
    ) {
        this.workerId = normalizeWorkerId(workerId);
        this.running = new AtomicBoolean(false);
        this.customerWorkflowProcessor = customerWorkflowProcessor == null
                ? new CustomerWorkflowProcessor(this.workerId)
                : customerWorkflowProcessor;
        this.maintenanceProvider = maintenanceProvider == null
                ? new CustomerWorkflowMaintenanceProvider(null)
                : maintenanceProvider;
    }

    @Override
    public void run() {
        runOnce();
    }

    public void runOnce() {
        if (!GlobalConfiguration.isCustomerWorkflowJobEnabled()) {
            log.debug("Customer workflow job is disabled.");
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("Customer workflow job is already running. Skipping this execution.");
            return;
        }

        try {
            executeJob();
        } catch (RuntimeException e) {
            log.error("Unhandled error while running customer workflow job.", e);
            throw e;
        } finally {
            running.set(false);
        }
    }

    private void executeJob() {
        Instant startedAt = Instant.now();

        log.info("Customer workflow job started. workerId={}, startedAt={}", workerId, startedAt);

        CustomerWorkflowMaintenanceResult maintenanceResult = maintenanceProvider.runMaintenance();

        if (maintenanceResult.hasChanges()) {
            log.info("Customer workflow maintenance completed. result={}", maintenanceResult);
        }

        int processedCount = customerWorkflowProcessor.processDueWorkflows();

        Instant finishedAt = Instant.now();

        log.info(
                "Customer workflow job finished. workerId={}, processedCount={}, maintenanceAffectedRows={}, finishedAt={}",
                workerId,
                processedCount,
                maintenanceResult.getTotalAffectedRows(),
                finishedAt
        );
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getWorkerId() {
        return workerId;
    }

    private static String buildDefaultWorkerId() {
        return "integration-customer-workflow-worker-"
                + getHostName()
                + "-"
                + UUID.randomUUID();
    }

    private static String getHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();

            if (hostName != null && !hostName.trim().isEmpty()) {
                return hostName.trim();
            }
        } catch (UnknownHostException ignored) {
            // Fallback below.
        }

        return "unknown-host";
    }

    private static String normalizeWorkerId(String workerId) {
        if (workerId == null || workerId.trim().isEmpty()) {
            return buildDefaultWorkerId();
        }

        return workerId.trim();
    }
}