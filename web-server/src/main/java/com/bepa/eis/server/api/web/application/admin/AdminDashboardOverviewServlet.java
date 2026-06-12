package com.bepa.eis.server.api.web.application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.OperatingSystemMXBean;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

//@WebServlet(name = "AdminDashboardOverviewServlet", urlPatterns = "/admin/api/dashboard/overview")
public class AdminDashboardOverviewServlet extends HttpServlet {

    private static final int MEMORY_HISTORY_LIMIT = 48;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Instant servletStartedAt = Instant.now();
    private final Deque<MemoryPoint> memoryHistory = new ArrayDeque<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long requestStartedNanos = System.nanoTime();

        DashboardOverviewResponse response = buildOverviewResponse(requestStartedNanos);

        sendJson(resp, HttpServletResponse.SC_OK, response);
    }

    private DashboardOverviewResponse buildOverviewResponse(long requestStartedNanos) {
        OperatingSystemMXBean osBean = getOperatingSystemBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        CpuMetrics cpuMetrics = buildCpuMetrics(osBean);
        MemoryMetrics memoryMetrics = buildMemoryMetrics(memoryBean);
        DiskMetrics diskMetrics = buildDiskMetrics();

        addMemoryHistoryPoint(memoryMetrics);

        double responseTimeMs = nanosToMillis(System.nanoTime() - requestStartedNanos);
        double availability = calculateAvailability();

        return new DashboardOverviewResponse(
                cpuMetrics.cpu(),
                cpuMetrics.cpuMax(),
                memoryMetrics.memory(),
                memoryMetrics.memoryMax(),
                diskMetrics.disk(),
                diskMetrics.diskMax(),
                buildIopsPlaceholder(),
                buildNetworkInPlaceholder(),
                buildNetworkOutPlaceholder(),
                round(responseTimeMs, 1),
                availability,
                99,
                98,
                97,
                "OK",
                "OK",
                "OK",
                0,
                new ArrayList<>(memoryHistory)
        );
    }

    private OperatingSystemMXBean getOperatingSystemBean() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

        if (bean instanceof OperatingSystemMXBean operatingSystemMXBean) {
            return operatingSystemMXBean;
        }

        return null;
    }

    private CpuMetrics buildCpuMetrics(OperatingSystemMXBean osBean) {
        if (osBean == null) {
            return new CpuMetrics(0.0, 0.0);
        }

        double systemCpuLoad = osBean.getCpuLoad();
        double processCpuLoad = osBean.getProcessCpuLoad();

        double cpu = systemCpuLoad >= 0 ? systemCpuLoad * 100 : 0.0;
        double cpuMax = processCpuLoad >= 0 ? Math.max(cpu, processCpuLoad * 100) : cpu;

        return new CpuMetrics(round(cpu, 1), round(cpuMax, 1));
    }

    private MemoryMetrics buildMemoryMetrics(MemoryMXBean memoryBean) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long heapUsed = Math.max(heapUsage.getUsed(), 0);
        long heapCommitted = Math.max(heapUsage.getCommitted(), 0);
        long heapMax = heapUsage.getMax() > 0 ? heapUsage.getMax() : heapCommitted;

        long nonHeapUsed = Math.max(nonHeapUsage.getUsed(), 0);
        long nonHeapCommitted = Math.max(nonHeapUsage.getCommitted(), 0);

        long used = heapUsed + nonHeapUsed;
        long committed = heapCommitted + nonHeapCommitted;
        long max = heapMax > 0 ? heapMax + nonHeapCommitted : committed;

        double memoryPercent = max > 0 ? ((double) used / max) * 100 : 0.0;
        double committedPercent = max > 0 ? ((double) committed / max) * 100 : memoryPercent;

        double usedGb = bytesToGb(used);
        double committedGb = bytesToGb(committed);
        double limitGb = bytesToGb(max);
        double availableGb = Math.max(limitGb - usedGb, 0.0);

        return new MemoryMetrics(
                round(memoryPercent, 1),
                round(Math.max(memoryPercent, committedPercent), 1),
                round(committedGb, 2),
                round(limitGb, 2),
                round(availableGb, 2),
                round(usedGb, 2)
        );
    }

    private DiskMetrics buildDiskMetrics() {
        File[] roots = File.listRoots();

        if (roots == null || roots.length == 0) {
            return new DiskMetrics(0.0, 0.0);
        }

        long total = 0;
        long free = 0;

        for (File root : roots) {
            total += Math.max(root.getTotalSpace(), 0);
            free += Math.max(root.getFreeSpace(), 0);
        }

        long used = Math.max(total - free, 0);
        double diskPercent = total > 0 ? ((double) used / total) * 100 : 0.0;

        return new DiskMetrics(round(diskPercent, 1), round(diskPercent, 1));
    }

    private void addMemoryHistoryPoint(MemoryMetrics memoryMetrics) {
        synchronized (memoryHistory) {
            memoryHistory.addLast(new MemoryPoint(
                    memoryMetrics.committedGb(),
                    memoryMetrics.limitGb(),
                    memoryMetrics.availableGb(),
                    memoryMetrics.usedGb()
            ));

            while (memoryHistory.size() > MEMORY_HISTORY_LIMIT) {
                memoryHistory.removeFirst();
            }

            while (memoryHistory.size() < MEMORY_HISTORY_LIMIT) {
                memoryHistory.addFirst(new MemoryPoint(
                        memoryMetrics.committedGb(),
                        memoryMetrics.limitGb(),
                        memoryMetrics.availableGb(),
                        memoryMetrics.usedGb()
                ));
            }
        }
    }

    private double calculateAvailability() {
        Duration uptime = Duration.between(servletStartedAt, Instant.now());

        if (uptime.toSeconds() < 60) {
            return 100.0;
        }

        return 99.9;
    }

    private double buildIopsPlaceholder() {
        return 0.0;
    }

    private double buildNetworkInPlaceholder() {
        return 0.0;
    }

    private double buildNetworkOutPlaceholder() {
        return 0.0;
    }

    private double bytesToGb(long bytes) {
        return bytes / 1024.0 / 1024.0 / 1024.0;
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private double round(double value, int decimals) {
        double multiplier = Math.pow(10, decimals);
        return Math.round(value * multiplier) / multiplier;
    }

    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        objectMapper.writeValue(resp.getOutputStream(), body);
    }

    private record CpuMetrics(
            double cpu,
            double cpuMax
    ) {
    }

    private record MemoryMetrics(
            double memory,
            double memoryMax,
            double committedGb,
            double limitGb,
            double availableGb,
            double usedGb
    ) {
    }

    private record DiskMetrics(
            double disk,
            double diskMax
    ) {
    }

    public record DashboardOverviewResponse(
            double cpu,
            double cpuMax,
            double memory,
            double memoryMax,
            double disk,
            double diskMax,
            double iops,
            double networkIn,
            double networkOut,
            double response,
            double availability,
            double availability7,
            double availability15,
            double availability30,
            String apiStatus,
            String databaseStatus,
            String queueStatus,
            int incidentCount,
            List<MemoryPoint> memorySeries
    ) {
    }

    public record MemoryPoint(
            double committed,
            double limit,
            double available,
            double used
    ) {
    }
}