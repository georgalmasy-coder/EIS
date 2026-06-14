package com.bepa.eis.server.api.web.application.admin;

import com.sun.management.OperatingSystemMXBean;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@WebListener
public class AdminDashboardCpuLoadSampler implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardCpuLoadSampler.class);

    private static final int SAMPLE_INTERVAL_SECONDS = 10;
    private static final int HISTORY_HOURS = 8;
    private static final int MAX_POINTS = HISTORY_HOURS * 60 * 60 / SAMPLE_INTERVAL_SECONDS;
    private static final int STARTUP_SEED_POINTS = 12;

    private static final Deque<CpuLoadPoint> CPU_LOAD_HISTORY = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private static volatile ScheduledExecutorService executorService;
    private static volatile double lastKnownCpuLoadPercent = 0.0;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        stop();
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        seedStartupHistory();

        executorService = Executors.newSingleThreadScheduledExecutor(new CpuSamplerThreadFactory());

        executorService.scheduleAtFixedRate(
                AdminDashboardCpuLoadSampler::sampleCpuLoadSafely,
                0,
                SAMPLE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info(
                "Admin dashboard CPU load sampler started. intervalSeconds={}, historyHours={}, maxPoints={}",
                SAMPLE_INTERVAL_SECONDS,
                HISTORY_HOURS,
                MAX_POINTS
        );
    }

    public static void stop() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }

        ScheduledExecutorService currentExecutorService = executorService;

        if (currentExecutorService != null) {
            currentExecutorService.shutdownNow();
            executorService = null;
        }

        CPU_LOAD_HISTORY.clear();
        lastKnownCpuLoadPercent = 0.0;

        log.info("Admin dashboard CPU load sampler stopped.");
    }

    public static List<CpuLoadPoint> getCpuLoadHistory() {
        ensureStarted();
        return new ArrayList<>(CPU_LOAD_HISTORY);
    }

    public static boolean isStarted() {
        return STARTED.get();
    }

    public static int getSampleCount() {
        return CPU_LOAD_HISTORY.size();
    }

    private static void ensureStarted() {
        if (!STARTED.get()) {
            start();
        }
    }

    private static void seedStartupHistory() {
        double initialCpuLoad = readSystemCpuLoadPercent();
        long nowMillis = System.currentTimeMillis();

        for (int index = STARTUP_SEED_POINTS - 1; index >= 0; index--) {
            long sampleMillis = nowMillis - ((long) index * SAMPLE_INTERVAL_SECONDS * 1000L);

            CPU_LOAD_HISTORY.addLast(new CpuLoadPoint(
                    sampleMillis,
                    Instant.ofEpochMilli(sampleMillis).toString(),
                    initialCpuLoad
            ));
        }

        trimHistory();
    }

    private static void sampleCpuLoadSafely() {
        try {
            double cpuLoad = readSystemCpuLoadPercent();
            long nowMillis = System.currentTimeMillis();

            CPU_LOAD_HISTORY.addLast(new CpuLoadPoint(
                    nowMillis,
                    Instant.ofEpochMilli(nowMillis).toString(),
                    cpuLoad
            ));

            trimHistory();
        } catch (Exception e) {
            log.warn("Could not sample system CPU load for admin dashboard.", e);
        }
    }

    private static void trimHistory() {
        while (CPU_LOAD_HISTORY.size() > MAX_POINTS) {
            CPU_LOAD_HISTORY.pollFirst();
        }
    }

    private static double readSystemCpuLoadPercent() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

        if (!(bean instanceof OperatingSystemMXBean operatingSystemMXBean)) {
            return lastKnownCpuLoadPercent;
        }

        double cpuLoad = operatingSystemMXBean.getCpuLoad();

        if (isValidCpuLoad(cpuLoad)) {
            lastKnownCpuLoadPercent = round(cpuLoad * 100.0, 1);
            return lastKnownCpuLoadPercent;
        }

        double processCpuLoad = operatingSystemMXBean.getProcessCpuLoad();

        if (isValidCpuLoad(processCpuLoad)) {
            lastKnownCpuLoadPercent = round(processCpuLoad * 100.0, 1);
            return lastKnownCpuLoadPercent;
        }

        double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();
        int processors = Math.max(operatingSystemMXBean.getAvailableProcessors(), 1);

        if (systemLoadAverage >= 0) {
            lastKnownCpuLoadPercent = round(Math.min((systemLoadAverage / processors) * 100.0, 100.0), 1);
            return lastKnownCpuLoadPercent;
        }

        return lastKnownCpuLoadPercent;
    }

    private static boolean isValidCpuLoad(double cpuLoad) {
        return !Double.isNaN(cpuLoad)
                && !Double.isInfinite(cpuLoad)
                && cpuLoad >= 0.0
                && cpuLoad <= 1.0;
    }

    private static double round(
            double value,
            int decimals
    ) {
        double multiplier = Math.pow(10, decimals);
        return Math.round(value * multiplier) / multiplier;
    }

    public record CpuLoadPoint(
            long epochMillis,
            String time,
            double value
    ) {
    }

    private static class CpuSamplerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                    runnable,
                    "admin-dashboard-cpu-load-sampler"
            );

            thread.setDaemon(true);

            return thread;
        }
    }
}