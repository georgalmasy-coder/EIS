package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.security.AdminDashboardSecurityProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.OperatingSystemMXBean;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = {
        "/admin/api/dashboard/overview",
        "/admin/api/dashboard/system-status",
        "/admin/api/dashboard/customers",
        "/admin/api/dashboard/users",
        "/admin/api/dashboard/customer-creation",
        "/admin/api/dashboard/subscriptions-payments",
        "/admin/api/dashboard/alerts",
        "/admin/api/dashboard/integrations",
        "/admin/api/dashboard/modules",
        "/admin/api/dashboard/performance",
        "/admin/api/dashboard/audit-security"
})
public class AdminDashboardServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardServlet.class);

    private static final int MEMORY_HISTORY_LIMIT = 48;
    private static final int ALERT_DASHBOARD_DAYS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<MemoryPoint> memoryHistory = new ArrayDeque<>();
    private final Instant startedAt = Instant.now();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        long requestStartedNanos = System.nanoTime();
        String path = request.getServletPath();

        Object body = switch (path) {
            case "/admin/api/dashboard/overview" -> buildOverviewResponse();
            case "/admin/api/dashboard/system-status" -> buildSystemStatusResponse(requestStartedNanos);
            case "/admin/api/dashboard/customers" -> buildCustomersResponse();
            case "/admin/api/dashboard/users" -> buildUsersResponse();
            case "/admin/api/dashboard/customer-creation" -> buildCustomerCreationResponse();
            case "/admin/api/dashboard/subscriptions-payments" -> buildSubscriptionsPaymentsResponse();
            case "/admin/api/dashboard/alerts" -> buildAlertsResponse();
            case "/admin/api/dashboard/integrations" -> buildIntegrationsResponse();
            case "/admin/api/dashboard/modules" -> buildModulesResponse();
            case "/admin/api/dashboard/performance" -> buildPerformanceResponse();
            case "/admin/api/dashboard/audit-security" -> buildAuditSecurityResponse();
            default -> null;
        };

        try {
            if (body == null) {
                sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Dashboard endpoint was not found.");
                return;
            }

            sendJson(response, HttpServletResponse.SC_OK, body);
        } catch (IOException e) {
            log.error("Failed to process dashboard request: {}", e.getMessage());
        }
    }

    private OverviewResponse buildOverviewResponse() {
        List<EventRow> latestEvents = loadLatestEvents();

        return new OverviewResponse(
                98,
                128,
                3842,
                1,
                3,
                5,
                2,
                0,
                3,
                List.of(40, 44, 48, 52, 49, 55, 62, 68, 65, 72, 78, 83),
                List.of(
                        new ChartItem("Healthy", 92, "#84d64b"),
                        new ChartItem("Warning", 6, "#f7c948"),
                        new ChartItem("Critical", 2, "#ef4444")
                ),
                latestEvents,
                List.of(
                        new StatusRow("API", "OK"),
                        new StatusRow("Database", "OK"),
                        new StatusRow("Queue", "OK"),
                        new StatusRow("Email", "Warning")
                )
        );
    }

    private List<EventRow> loadLatestEvents() {
        try {
            EventProvider eventProvider = new EventProvider(null);

            return eventProvider.getRecentEvent()
                    .stream()
                    .map(event -> new EventRow(
                            event.time(),
                            event.type(),
                            event.description(),
                            event.project(),
                            event.status()
                    ))
                    .toList();
        } catch (SQLException e) {
            log.warn("Could not load latest events from database. Using empty latest events list.", e);
            return List.of();
        }
    }

    private SystemStatusResponse buildSystemStatusResponse(long requestStartedNanos) {
        OperatingSystemMXBean osBean = getOperatingSystemBean();
        MemoryMetrics memoryMetrics = buildMemoryMetrics();
        DiskMetrics diskMetrics = buildDiskMetrics();

        addMemoryHistoryPoint(memoryMetrics);

        double cpu = buildCpu(osBean);
        double responseMs = nanosToMillis(System.nanoTime() - requestStartedNanos);

        return new SystemStatusResponse(
                round(cpu, 1),
                memoryMetrics.memoryPercent(),
                diskMetrics.diskPercent(),
                round(responseMs, 1),
                99.9,
                "OK",
                "OK",
                "OK",
                0,
                new ArrayList<>(memoryHistory)
        );
    }

    private CustomersResponse buildCustomersResponse() {
        return new CustomersResponse(
                128,
                14,
                5,
                2,
                7,
                List.of(82, 88, 91, 97, 102, 108, 113, 119, 123, 128),
                List.of(
                        new ChartItem("Basis", 104, "#2f9cff"),
                        new ChartItem("Pro", 21, "#8b5cf6"),
                        new ChartItem("Master", 3, "#84d64b")
                ),
                List.of(
                        new ChartItem("Healthy", 110, "#84d64b"),
                        new ChartItem("Warning", 13, "#f7c948"),
                        new ChartItem("Critical", 5, "#ef4444")
                ),
                List.of(
                        new CountryCustomerRow("Denmark", 82),
                        new CountryCustomerRow("Sweden", 14),
                        new CountryCustomerRow("Germany", 11),
                        new CountryCustomerRow("United Kingdom", 9),
                        new CountryCustomerRow("United States", 6)
                ),
                List.of(
                        new CustomerProblemRow("Nordic Systems A/S", "Denmark", "Basis", "Warning", "Email confirmation missing"),
                        new CustomerProblemRow("Global Engineering Ltd.", "United Kingdom", "Pro", "Critical", "Payment failed"),
                        new CustomerProblemRow("ACME GmbH", "Germany", "Basis", "Warning", "Admin verification required")
                )
        );
    }

    private UsersResponse buildUsersResponse() {
        return new UsersResponse(
                3842,
                312,
                18,
                3,
                86,
                List.of(44, 58, 73, 91, 120, 144, 168, 201, 244, 312),
                List.of(
                        new ChartItem("Admin", 42, "#ef4444"),
                        new ChartItem("Editor", 620, "#f7c948"),
                        new ChartItem("User", 3180, "#84d64b")
                ),
                List.of(
                        new ChartItem("Successful", 312, "#84d64b"),
                        new ChartItem("Failed", 18, "#ef4444")
                ),
                List.of(
                        new CustomerUsersRow("Nordic Systems A/S", 420),
                        new CustomerUsersRow("ACME GmbH", 315),
                        new CustomerUsersRow("Global Engineering Ltd.", 280),
                        new CustomerUsersRow("Energy Platform ApS", 170),
                        new CustomerUsersRow("Industrial Systems AB", 145)
                ),
                List.of(
                        new InactiveUserRow("anna@example.com", "Nordic Systems A/S", "User", "2026-04-02", "Warning"),
                        new InactiveUserRow("peter@example.com", "ACME GmbH", "Editor", "2026-03-18", "Warning"),
                        new InactiveUserRow("old-admin@example.com", "Global Engineering Ltd.", "Admin", "2026-02-12", "Critical")
                )
        );
    }

    private CustomerCreationResponse buildCustomerCreationResponse() {
        return new CustomerCreationResponse(
                120,
                92,
                74,
                68,
                58,
                5,
                44,
                88,
                91,
                4,
                List.of(4, 8, 6, 10, 12, 9, 14, 18, 16, 22),
                List.of(
                        new ChartItem("Success", 88, "#84d64b"),
                        new ChartItem("Failed", 12, "#ef4444")
                ),
                List.of(
                        new FailedCreationAttemptRow("11:18", "Global Engineering Ltd.", "Payment", "Card validation failed", "Open"),
                        new FailedCreationAttemptRow("10:42", "Nordic Systems A/S", "Email Confirmation", "Confirmation email could not be sent", "Warning"),
                        new FailedCreationAttemptRow("09:55", "Unknown", "CVR Lookup", "CVR lookup returned no company", "Handled")
                )
        );
    }

    private SubscriptionsPaymentsResponse buildSubscriptionsPaymentsResponse() {
        return new SubscriptionsPaymentsResponse(
                184500,
                2214000,
                6,
                9,
                1.8,
                List.of(130000, 138000, 145000, 151000, 160000, 166000, 172000, 178000, 181000, 184500),
                List.of(
                        new ChartItem("Basis", 114000, "#2f9cff"),
                        new ChartItem("Pro", 52000, "#8b5cf6"),
                        new ChartItem("Master", 18500, "#84d64b")
                ),
                List.of(
                        new ChartItem("Active", 118, "#84d64b"),
                        new ChartItem("Trial", 8, "#2f9cff"),
                        new ChartItem("Suspended", 2, "#f7c948"),
                        new ChartItem("Cancelled", 1, "#ef4444")
                ),
                List.of(
                        new ChartItem("Successful", 96, "#84d64b"),
                        new ChartItem("Failed", 4, "#ef4444")
                ),
                List.of(
                        new FailedPaymentRow("11:21", "Global Engineering Ltd.", "4.900 kr.", "Card declined", "Open"),
                        new FailedPaymentRow("10:44", "ACME GmbH", "1.900 kr.", "Insufficient funds", "Retrying"),
                        new FailedPaymentRow("09:18", "Nordic Systems A/S", "990 kr.", "Expired card", "Customer contacted")
                )
        );
    }

    private AlertsResponse buildAlertsResponse() {
        List<OpenIncidentRow> todaysIncidents = loadTodaysIncidents();
        List<OpenIncidentRow> openIncidents = loadOpenIncidentsLastDays();
        List<ServiceStatusRow> affectedServices = loadAffectedServicesLastDays();
        List<Integer> alertsTrend = loadAlertsTrend();

        int critical = Math.toIntExact(todaysIncidents.stream()
                .filter(row -> "Critical".equalsIgnoreCase(row.severityType()))
                .count());

        int high = Math.toIntExact(todaysIncidents.stream()
                .filter(row -> "High".equalsIgnoreCase(row.severityType()))
                .count());

        int medium = Math.toIntExact(todaysIncidents.stream()
                .filter(row -> "Medium".equalsIgnoreCase(row.severityType()))
                .count());

        return new AlertsResponse(
                critical,
                high,
                medium,
                0,
                0,
                alertsTrend,
                List.of(
                        new ChartItem("Critical", critical, "#ef4444"),
                        new ChartItem("High", high, "#fb923c"),
                        new ChartItem("Medium", medium, "#f7c948")
                ),
                List.of(
                        new ChartItem("Open today", todaysIncidents.size(), "#ef4444"),
                        new ChartItem("Resolved today", 0, "#84d64b")
                ),
                affectedServices,
                openIncidents
        );
    }

    private List<OpenIncidentRow> loadTodaysIncidents() {
        try {
            IncidentProvider incidentProvider = new IncidentProvider(null);

            return incidentProvider.getRecentIncidentsToday()
                    .stream()
                    .map(this::toOpenIncidentRow)
                    .toList();
        } catch (SQLException e) {
            log.warn("Could not load today's incidents from database. Using empty incidents list.", e);
            return List.of();
        }
    }

    private List<OpenIncidentRow> loadOpenIncidentsLastDays() {
        try {
            IncidentProvider incidentProvider = new IncidentProvider(null);

            return incidentProvider.getRecentIncidentsLastDays(ALERT_DASHBOARD_DAYS)
                    .stream()
                    .map(this::toOpenIncidentRow)
                    .toList();
        } catch (SQLException e) {
            log.warn("Could not load incidents from database. Using empty incidents list.", e);
            return List.of();
        }
    }

    private OpenIncidentRow toOpenIncidentRow(IncidentProvider.RecentIncident incident) {
        return new OpenIncidentRow(
                incident.logCreated(),
                incident.customer(),
                incident.project(),
                incident.user(),
                incident.serviceType(),
                incident.severityType(),
                incident.module(),
                incident.message()
        );
    }

    private List<Integer> loadAlertsTrend() {
        try {
            IncidentProvider incidentProvider = new IncidentProvider(null);

            return incidentProvider.getIncidentTrendLastDays(ALERT_DASHBOARD_DAYS)
                    .stream()
                    .map(IncidentProvider.IncidentTrendPoint::count)
                    .toList();
        } catch (SQLException e) {
            log.warn("Could not load alert trend from database. Using empty alert trend.", e);
            return emptyIntegerList(ALERT_DASHBOARD_DAYS);
        }
    }

    private List<ServiceStatusRow> loadAffectedServicesLastDays() {
        try {
            IncidentProvider incidentProvider = new IncidentProvider(null);

            return incidentProvider.getIncidentCountsByServiceLastDays(ALERT_DASHBOARD_DAYS)
                    .stream()
                    .map(serviceIncidentCount -> new ServiceStatusRow(
                            serviceIncidentCount.service(),
                            serviceIncidentCount.count()
                    ))
                    .toList();
        } catch (SQLException e) {
            log.warn("Could not load affected services from database. Using empty affected services list.", e);
            return List.of();
        }
    }

    private List<Integer> emptyIntegerList(int size) {
        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            values.add(0);
        }

        return values;
    }

    private IntegrationsResponse buildIntegrationsResponse() {
        return new IntegrationsResponse(
                "OK",
                "OK",
                "Warning",
                "OK",
                "OK",
                98,
                99,
                86,
                99,
                96,
                List.of(110, 125, 118, 140, 135, 155, 149, 162, 151, 170),
                List.of(
                        new ChartItem("Success", 96, "#84d64b"),
                        new ChartItem("Failure", 4, "#ef4444")
                ),
                List.of(
                        new IntegrationStatusRow("Virk.dk / CVR", "OK"),
                        new IntegrationStatusRow("Email Provider", "OK"),
                        new IntegrationStatusRow("Payment Provider", "Warning"),
                        new IntegrationStatusRow("SSO Provider", "OK"),
                        new IntegrationStatusRow("External API Gateway", "OK")
                ),
                List.of(
                        new IntegrationCallVolumeRow("Virk.dk / CVR", 44),
                        new IntegrationCallVolumeRow("Email Provider", 318),
                        new IntegrationCallVolumeRow("Payment Provider", 86),
                        new IntegrationCallVolumeRow("SSO Provider", 210),
                        new IntegrationCallVolumeRow("External API Gateway", 128)
                ),
                List.of(
                        new IntegrationEventRow("11:48", "Virk.dk / CVR", "Company lookup", "142 ms", "OK"),
                        new IntegrationEventRow("11:42", "Payment Provider", "Payment validation", "618 ms", "Warning"),
                        new IntegrationEventRow("11:35", "Email Provider", "Send confirmation email", "233 ms", "OK")
                )
        );
    }

    private ModulesResponse buildModulesResponse() {
        return new ModulesResponse(
                104,
                21,
                3,
                6,
                1,
                List.of(82, 88, 91, 96, 102, 108, 114, 119, 124, 128),
                List.of(
                        new ChartItem("Basis", 104, "#2f9cff"),
                        new ChartItem("Pro", 21, "#8b5cf6"),
                        new ChartItem("Master", 3, "#84d64b")
                ),
                List.of(
                        new FeatureUsageRow("Requirements Management", 118),
                        new FeatureUsageRow("System Breakdown", 94),
                        new FeatureUsageRow("Attachments", 72),
                        new FeatureUsageRow("Notes", 63),
                        new FeatureUsageRow("Export", 41)
                ),
                List.of(
                        new ModuleStatusRow("Basis", "Available"),
                        new ModuleStatusRow("Pro", "Coming Soon"),
                        new ModuleStatusRow("Master", "Coming Soon")
                ),
                List.of(
                        new ModuleChangeRow("11:32", "Nordic Systems A/S", "Upgrade requested", "Basis", "Pro", "Pending"),
                        new ModuleChangeRow("10:18", "ACME GmbH", "Module enabled", "None", "Basis", "OK"),
                        new ModuleChangeRow("09:44", "Global Engineering Ltd.", "Downgrade requested", "Pro", "Basis", "Review")
                )
        );
    }

    private PerformanceResponse buildPerformanceResponse() {
        try {
            PerformanceProvider performanceProvider = new PerformanceProvider(null);

            PerformanceProvider.PerformanceKpis performanceKpis =
                    performanceProvider.getPerformanceKpisLast7Days();

            List<PerformanceModuleRow> modulePerformance =
                    performanceProvider.getModulePerformanceLast7Days()
                            .stream()
                            .map(row -> new PerformanceModuleRow(
                                    row.module(),
                                    row.avgDurationMs(),
                                    row.count(),
                                    row.goodPerformanceCount(),
                                    row.acceptablePerformanceCount(),
                                    row.poorPerformanceCount()
                            ))
                            .toList();

            List<ProjectModulePerformanceRow> projectModulePerformance =
                    performanceProvider.getProjectModulePerformanceLast7Days()
                            .stream()
                            .map(row -> new ProjectModulePerformanceRow(
                                    row.project(),
                                    row.module(),
                                    row.avgDurationMs(),
                                    row.count(),
                                    row.goodPerformanceCount(),
                                    row.acceptablePerformanceCount(),
                                    row.poorPerformanceCount()
                            ))
                            .toList();

            List<RecentPerformanceMeasurementRow> recentPerformanceMeasurements =
                    performanceProvider.getRecentPerformanceMeasurements()
                            .stream()
                            .map(row -> new RecentPerformanceMeasurementRow(
                                    row.created(),
                                    row.customer(),
                                    row.project(),
                                    row.module(),
                                    row.durationMs(),
                                    row.performanceInterval()
                            ))
                            .toList();

            return new PerformanceResponse(
                    performanceKpis.goodPerformanceCount(),
                    performanceKpis.acceptablePerformanceCount(),
                    performanceKpis.poorPerformanceCount(),
                    modulePerformance,
                    projectModulePerformance,
                    recentPerformanceMeasurements
            );
        } catch (SQLException e) {
            log.warn("Could not load performance dashboard data from database. Using empty performance dashboard data.", e);

            return new PerformanceResponse(
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }

    private Object buildAuditSecurityResponse() {
        try {
            AdminDashboardSecurityProvider provider = new AdminDashboardSecurityProvider(null);
            return provider.loadSecurityDashboardData();
        } catch (SQLException e) {
            log.warn("Could not load audit and security dashboard data from database. Using fallback data.", e);
            return buildFallbackAuditSecurityResponse();
        }
    }

    private AuditSecurityResponse buildFallbackAuditSecurityResponse() {
        return new AuditSecurityResponse(
                91,
                74,
                3,
                86,
                4,
                List.of(12, 16, 14, 20, 24, 31, 28, 36, 42, 74),
                List.of(
                        new ChartItem("MFA enabled", 86, "#84d64b"),
                        new ChartItem("Missing MFA", 14, "#ef4444")
                ),
                List.of(
                        new ChartItem("Failed login", 74, "#ef4444"),
                        new ChartItem("Permission change", 4, "#f7c948"),
                        new ChartItem("Locked account", 3, "#8b5cf6"),
                        new ChartItem("New admin", 1, "#2f9cff")
                ),
                List.of(
                        new SecurityStatusRow("Password policy", "OK"),
                        new SecurityStatusRow("MFA coverage", "Warning"),
                        new SecurityStatusRow("Admin users", "OK"),
                        new SecurityStatusRow("Suspicious activity", "Warning")
                ),
                List.of(
                        new AuditEventRow("11:52", "admin@bepa.dk", "Changed user role", "user: peter@example.com", "OK"),
                        new AuditEventRow("11:33", "system", "Locked user account", "user: unknown@example.com", "Warning"),
                        new AuditEventRow("10:47", "security@bepa.dk", "Created admin user", "user: new-admin@example.com", "Review")
                )
        );
    }

    private OperatingSystemMXBean getOperatingSystemBean() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

        if (bean instanceof OperatingSystemMXBean operatingSystemMXBean) {
            return operatingSystemMXBean;
        }

        return null;
    }

    private double buildCpu(OperatingSystemMXBean osBean) {
        if (osBean == null) {
            return 0.0;
        }

        double cpuLoad = osBean.getCpuLoad();

        if (cpuLoad < 0) {
            return 0.0;
        }

        return cpuLoad * 100;
    }

    private MemoryMetrics buildMemoryMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
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
        double usedGb = bytesToGb(used);
        double committedGb = bytesToGb(committed);
        double limitGb = bytesToGb(max);
        double availableGb = Math.max(limitGb - usedGb, 0.0);

        return new MemoryMetrics(
                round(memoryPercent, 1),
                round(committedGb, 2),
                round(limitGb, 2),
                round(availableGb, 2),
                round(usedGb, 2)
        );
    }

    private DiskMetrics buildDiskMetrics() {
        File[] roots = File.listRoots();

        if (roots == null || roots.length == 0) {
            return new DiskMetrics(0.0);
        }

        long total = 0;
        long free = 0;

        for (File root : roots) {
            total += Math.max(root.getTotalSpace(), 0);
            free += Math.max(root.getFreeSpace(), 0);
        }

        long used = Math.max(total - free, 0);
        double diskPercent = total > 0 ? ((double) used / total) * 100 : 0.0;

        return new DiskMetrics(round(diskPercent, 1));
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

    private void sendJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        sendJson(response, status, new ErrorResponse(message));
    }

    private record ErrorResponse(String message) {
    }

    private record MemoryMetrics(
            double memoryPercent,
            double committedGb,
            double limitGb,
            double availableGb,
            double usedGb
    ) {
    }

    private record DiskMetrics(double diskPercent) {
    }

    public record MemoryPoint(
            double committed,
            double limit,
            double available,
            double used
    ) {
    }

    public record ChartItem(
            String label,
            double value,
            String color
    ) {
    }

    public record StatusRow(
            String label,
            String status
    ) {
    }

    public record EventRow(
            String time,
            String type,
            String description,
            String project,
            String status
    ) {
    }

    public record OverviewResponse(
            int systemHealth,
            int activeCustomers,
            int activeUsers,
            int criticalAlerts,
            int paymentErrors,
            int pendingCustomerConfirmations,
            int failedEmails,
            int failedIntegrations,
            int lockedUsers,
            List<Integer> activityTrend,
            List<ChartItem> healthDistribution,
            List<EventRow> latestEvents,
            List<StatusRow> serviceStatus
    ) {
    }

    public record SystemStatusResponse(
            double cpu,
            double memory,
            double disk,
            double response,
            double availability,
            String apiStatus,
            String databaseStatus,
            String queueStatus,
            int incidentCount,
            List<MemoryPoint> memorySeries
    ) {
    }

    public record CountryCustomerRow(String country, int customers) {
    }

    public record CustomerProblemRow(String customer, String country, String module, String status, String issue) {
    }

    public record CustomersResponse(
            int activeCustomers,
            int newCustomersThisMonth,
            int pendingCustomers,
            int suspendedCustomers,
            int customersWithIssues,
            List<Integer> customerGrowth,
            List<ChartItem> customersByModule,
            List<ChartItem> customerHealth,
            List<CountryCustomerRow> customersByCountry,
            List<CustomerProblemRow> customersWithProblems
    ) {
    }

    public record CustomerUsersRow(String customer, int users) {
    }

    public record InactiveUserRow(String user, String customer, String role, String lastLogin, String status) {
    }

    public record UsersResponse(
            int activeUsers,
            int loginsToday,
            int failedLogins,
            int lockedAccounts,
            int newUsers,
            List<Integer> loginActivity,
            List<ChartItem> usersByRole,
            List<ChartItem> loginHealth,
            List<CustomerUsersRow> usersByCustomer,
            List<InactiveUserRow> inactiveUsers
    ) {
    }

    public record FailedCreationAttemptRow(String time, String customer, String step, String error, String status) {
    }

    public record CustomerCreationResponse(
            int started,
            int customerInfo,
            int payment,
            int confirmed,
            int activated,
            int pendingConfirmations,
            int cvrLookupsToday,
            int cvrSuccessRate,
            int paymentValidationRate,
            int failedCreations,
            List<Integer> creationTrend,
            List<ChartItem> cvrLookup,
            List<FailedCreationAttemptRow> failedCreationAttempts
    ) {
    }

    public record FailedPaymentRow(String time, String customer, String amount, String reason, String status) {
    }

    public record SubscriptionsPaymentsResponse(
            int mrr,
            int arr,
            int paymentErrors,
            int trialsExpiring,
            double churn,
            List<Integer> mrrTrend,
            List<ChartItem> revenueByModule,
            List<ChartItem> subscriptionStatus,
            List<ChartItem> paymentHealth,
            List<FailedPaymentRow> failedPayments
    ) {
    }

    public record ServiceStatusRow(
            String service,
            int count
    ) {
    }

    public record OpenIncidentRow(
            String logCreated,
            String customer,
            String project,
            String user,
            String serviceType,
            String severityType,
            String module,
            String message
    ) {
    }

    public record AlertsResponse(
            int critical,
            int high,
            int medium,
            int failedEmails,
            int failedJobs,
            List<Integer> alertsTrend,
            List<ChartItem> severity,
            List<ChartItem> alertHealth,
            List<ServiceStatusRow> affectedServices,
            List<OpenIncidentRow> openIncidents
    ) {
    }

    public record IntegrationStatusRow(String integration, String status) {
    }

    public record IntegrationCallVolumeRow(String integration, int calls) {
    }

    public record IntegrationEventRow(String time, String integration, String operation, String latency, String status) {
    }

    public record IntegrationsResponse(
            String virkStatus,
            String emailStatus,
            String paymentStatus,
            String ssoStatus,
            String externalApiStatus,
            int virkHealth,
            int emailHealth,
            int paymentHealthScore,
            int ssoHealth,
            int externalApiHealth,
            List<Integer> latency,
            List<ChartItem> successFailure,
            List<IntegrationStatusRow> integrationStatus,
            List<IntegrationCallVolumeRow> callVolume,
            List<IntegrationEventRow> integrationEvents
    ) {
    }

    public record FeatureUsageRow(String feature, int usage) {
    }

    public record ModuleStatusRow(String module, String status) {
    }

    public record ModuleChangeRow(String time, String customer, String change, String from, String to, String status) {
    }

    public record ModulesResponse(
            int basisCustomers,
            int proCustomers,
            int masterCustomers,
            int upgrades,
            int downgrades,
            List<Integer> moduleAdoption,
            List<ChartItem> moduleDistribution,
            List<FeatureUsageRow> featureUsage,
            List<ModuleStatusRow> moduleStatus,
            List<ModuleChangeRow> recentModuleChanges
    ) {
    }

    public record PerformanceModuleRow(
            String module,
            double avgDurationMs,
            int count,
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount
    ) {
    }

    public record ProjectModulePerformanceRow(
            String project,
            String module,
            double avgDurationMs,
            int count,
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount
    ) {
    }

    public record RecentPerformanceMeasurementRow(
            String created,
            String customer,
            String project,
            String module,
            int durationMs,
            String performanceInterval
    ) {
    }

    public record PerformanceResponse(
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount,
            List<PerformanceModuleRow> modulePerformance,
            List<ProjectModulePerformanceRow> projectModulePerformance,
            List<RecentPerformanceMeasurementRow> recentPerformanceMeasurements
    ) {
    }

    public record SecurityStatusRow(String label, String status) {
    }

    public record AuditEventRow(String time, String user, String action, String object, String status) {
    }

    public record AuditSecurityResponse(
            int securityScore,
            int failedLogins,
            int lockedUsers,
            int mfaCoverage,
            int adminChanges,
            List<Integer> failedLoginTrend,
            List<ChartItem> mfaCoverageDistribution,
            List<ChartItem> securityEventTypes,
            List<SecurityStatusRow> securityStatus,
            List<AuditEventRow> auditEvents
    ) {
    }

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {

    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) throws Exception {

    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return null;
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return null;
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return null;
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {

    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return null;
    }
}