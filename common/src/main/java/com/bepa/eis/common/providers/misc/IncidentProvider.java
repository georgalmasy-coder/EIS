package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.ServiceType;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IncidentProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(IncidentProvider.class);

    private static final String INSERT_INCIDENT_SQL =
            "INSERT INTO INCIDENTS " +
                    " (CustomerId, ProjectId, UserId, ServiceId, SeverityId, Message, Module, ModuleInfo, Trace, LogCreated) " +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String GET_RECENT_INCIDENTS_SQL = """
            SELECT TOP 100
                I.LogCreated,
                ISNULL(C.CustomerName, '') AS CustomerName,
                ISNULL(P.ProjectName, '') AS ProjectName,
                ISNULL(U.Name, '') AS UserName,
                I.ServiceId,
                I.SeverityId,
                I.Module,
                I.Message
            FROM INCIDENTS I
            LEFT JOIN CUSTOMER C
                ON I.CustomerId = C.CustomerId
               AND C.Latest = 1
            LEFT JOIN PROJECT P
                ON I.ProjectId = P.ProjectId
               AND I.CustomerId = P.CustomerId
               AND P.Latest = 1
            LEFT JOIN USERS U
                ON I.UserId = U.UserId
            ORDER BY I.LogCreated DESC, I.IncidentId DESC
            """;

    private static final String GET_RECENT_INCIDENTS_WITH_TRACE_SQL = """
            SELECT TOP (?)
                I.IncidentId,
                I.LogCreated,
                ISNULL(C.CustomerName, '') AS CustomerName,
                ISNULL(P.ProjectName, '') AS ProjectName,
                ISNULL(U.Name, '') AS UserName,
                I.ServiceId,
                I.SeverityId,
                I.Module,
                I.Message,
                I.Trace
            FROM INCIDENTS I
            LEFT JOIN CUSTOMER C
                ON I.CustomerId = C.CustomerId
               AND C.Latest = 1
            LEFT JOIN PROJECT P
                ON I.ProjectId = P.ProjectId
               AND I.CustomerId = P.CustomerId
               AND P.Latest = 1
            LEFT JOIN USERS U
                ON I.UserId = U.UserId
            ORDER BY I.LogCreated DESC, I.IncidentId DESC
            """;

    private static final String GET_RECENT_INCIDENTS_TODAY_SQL = """
            SELECT TOP 100
                I.LogCreated,
                ISNULL(C.CustomerName, '') AS CustomerName,
                ISNULL(P.ProjectName, '') AS ProjectName,
                ISNULL(U.Name, '') AS UserName,
                I.ServiceId,
                I.SeverityId,
                I.Module,
                I.Message
            FROM INCIDENTS I
            LEFT JOIN CUSTOMER C
                ON I.CustomerId = C.CustomerId
               AND C.Latest = 1
            LEFT JOIN PROJECT P
                ON I.ProjectId = P.ProjectId
               AND I.CustomerId = P.CustomerId
               AND P.Latest = 1
            LEFT JOIN USERS U
                ON I.UserId = U.UserId
            WHERE I.LogCreated >= CAST(GETDATE() AS date)
              AND I.LogCreated < DATEADD(DAY, 1, CAST(GETDATE() AS date))
            ORDER BY I.LogCreated DESC, I.IncidentId DESC
            """;

    private static final String GET_RECENT_INCIDENTS_LAST_DAYS_SQL = """
            SELECT TOP 100
                I.LogCreated,
                ISNULL(C.CustomerName, '') AS CustomerName,
                ISNULL(P.ProjectName, '') AS ProjectName,
                ISNULL(U.Name, '') AS UserName,
                I.ServiceId,
                I.SeverityId,
                I.Module,
                I.Message
            FROM INCIDENTS I
            LEFT JOIN CUSTOMER C
                ON I.CustomerId = C.CustomerId
               AND C.Latest = 1
            LEFT JOIN PROJECT P
                ON I.ProjectId = P.ProjectId
               AND I.CustomerId = P.CustomerId
               AND P.Latest = 1
            LEFT JOIN USERS U
                ON I.UserId = U.UserId
            WHERE I.LogCreated >= DATEADD(DAY, ?, CAST(GETDATE() AS date))
              AND I.LogCreated < DATEADD(DAY, 1, CAST(GETDATE() AS date))
            ORDER BY I.LogCreated DESC, I.IncidentId DESC
            """;

    private static final String GET_INCIDENT_COUNTS_BY_SERVICE_LAST_MONTH_SQL = """
            SELECT
                I.ServiceId,
                COUNT(*) AS IncidentCount
            FROM INCIDENTS I
            WHERE I.LogCreated >= DATEADD(MONTH, -1, GETDATE())
            GROUP BY I.ServiceId
            ORDER BY IncidentCount DESC
            """;

    private static final String GET_INCIDENT_COUNTS_BY_SERVICE_LAST_DAYS_SQL = """
            SELECT
                I.ServiceId,
                COUNT(*) AS IncidentCount
            FROM INCIDENTS I
            WHERE I.LogCreated >= DATEADD(DAY, ?, CAST(GETDATE() AS date))
              AND I.LogCreated < DATEADD(DAY, 1, CAST(GETDATE() AS date))
            GROUP BY I.ServiceId
            ORDER BY IncidentCount DESC
            """;

    private static final String GET_INCIDENT_TREND_LAST_DAYS_SQL = """
            SELECT
                CAST(I.LogCreated AS date) AS IncidentDate,
                COUNT(*) AS IncidentCount
            FROM INCIDENTS I
            WHERE I.LogCreated >= DATEADD(DAY, ?, CAST(GETDATE() AS date))
              AND I.LogCreated < DATEADD(DAY, 1, CAST(GETDATE() AS date))
            GROUP BY CAST(I.LogCreated AS date)
            ORDER BY IncidentDate
            """;

    public IncidentProvider(WebSession webSession) {
        super(webSession);
    }

    public void createWebServiceIncident(SeverityType severityType, String module, Throwable throwable) {
        createIncident(ServiceType.WEBSERVICE, module, severityType, throwable);
    }

    public void createProviderServiceIncident(SeverityType severityType, String module, Throwable throwable) {
        createIncident(ServiceType.PROVIDER_SERVICE, module, severityType, throwable);
    }

    public List<RecentIncident> getRecentIncidents() throws SQLException {
        return getRecentIncidents(GET_RECENT_INCIDENTS_SQL);
    }

    public List<RecentIncidentDetail> getRecentIncidentsWithTrace(int limit) throws SQLException {
        int safeLimit = sanitizeRecentIncidentLimit(limit);

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_RECENT_INCIDENTS_WITH_TRACE_SQL)) {

            ps.setInt(1, safeLimit);

            return getRecentIncidentDetails(ps);
        }
    }

    public List<RecentIncident> getRecentIncidentsToday() throws SQLException {
        return getRecentIncidents(GET_RECENT_INCIDENTS_TODAY_SQL);
    }

    public List<RecentIncident> getRecentIncidentsLastDays(int days) throws SQLException {
        int safeDays = Math.max(days, 1);

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_RECENT_INCIDENTS_LAST_DAYS_SQL)) {

            ps.setInt(1, -(safeDays - 1));

            return getRecentIncidents(ps);
        }
    }

    public List<ServiceIncidentCount> getIncidentCountsByServiceLastMonth() throws SQLException {
        List<ServiceIncidentCount> serviceIncidentCounts = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_INCIDENT_COUNTS_BY_SERVICE_LAST_MONTH_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                serviceIncidentCounts.add(toServiceIncidentCount(rs));
            }
        }

        return serviceIncidentCounts;
    }

    public List<ServiceIncidentCount> getIncidentCountsByServiceLastDays(int days) throws SQLException {
        int safeDays = Math.max(days, 1);
        List<ServiceIncidentCount> serviceIncidentCounts = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_INCIDENT_COUNTS_BY_SERVICE_LAST_DAYS_SQL)) {

            ps.setInt(1, -(safeDays - 1));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    serviceIncidentCounts.add(toServiceIncidentCount(rs));
                }
            }
        }

        return serviceIncidentCounts;
    }

    public List<IncidentTrendPoint> getIncidentTrendLastDays(int days) throws SQLException {
        int safeDays = Math.max(days, 1);
        Map<LocalDate, Integer> trend = createEmptyTrendMap(safeDays);

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_INCIDENT_TREND_LAST_DAYS_SQL)) {

            ps.setInt(1, -(safeDays - 1));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date incidentDate = rs.getDate("IncidentDate");

                    if (incidentDate != null) {
                        trend.put(incidentDate.toLocalDate(), rs.getInt("IncidentCount"));
                    }
                }
            }
        }

        return trend.entrySet()
                .stream()
                .map(entry -> new IncidentTrendPoint(
                        entry.getKey().toString(),
                        entry.getValue()
                ))
                .toList();
    }

    private List<RecentIncident> getRecentIncidents(String sql) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            return getRecentIncidents(ps);
        }
    }

    private List<RecentIncident> getRecentIncidents(PreparedStatement ps) throws SQLException {
        List<RecentIncident> recentIncidents = new ArrayList<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp logCreated = rs.getTimestamp("LogCreated");

                int serviceId = rs.getInt("ServiceId");
                ServiceType serviceType = rs.wasNull()
                        ? ServiceType.INVALID_SERVICE_TYPE
                        : ServiceType.valueOf(serviceId);

                int severityId = rs.getInt("SeverityId");
                SeverityType severityType = rs.wasNull()
                        ? SeverityType.INVALID_SEVERITY_TYPE
                        : SeverityType.valueOf(severityId);

                recentIncidents.add(new RecentIncident(
                        toIsoString(logCreated),
                        rs.getString("CustomerName"),
                        rs.getString("ProjectName"),
                        rs.getString("UserName"),
                        serviceType.getDescription(),
                        severityType.getDescription(),
                        rs.getString("Module"),
                        rs.getString("Message")
                ));
            }
        }

        return recentIncidents;
    }

    private List<RecentIncidentDetail> getRecentIncidentDetails(PreparedStatement ps) throws SQLException {
        List<RecentIncidentDetail> recentIncidents = new ArrayList<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp logCreated = rs.getTimestamp("LogCreated");

                int serviceId = rs.getInt("ServiceId");
                ServiceType serviceType = rs.wasNull()
                        ? ServiceType.INVALID_SERVICE_TYPE
                        : ServiceType.valueOf(serviceId);

                int severityId = rs.getInt("SeverityId");
                SeverityType severityType = rs.wasNull()
                        ? SeverityType.INVALID_SEVERITY_TYPE
                        : SeverityType.valueOf(severityId);

                recentIncidents.add(new RecentIncidentDetail(
                        rs.getInt("IncidentId"),
                        toIsoString(logCreated),
                        rs.getString("CustomerName"),
                        rs.getString("ProjectName"),
                        rs.getString("UserName"),
                        serviceType.getDescription(),
                        severityType.getDescription(),
                        rs.getString("Module"),
                        rs.getString("Message"),
                        rs.getString("Trace")
                ));
            }
        }

        return recentIncidents;
    }

    private ServiceIncidentCount toServiceIncidentCount(ResultSet rs) throws SQLException {
        int serviceId = rs.getInt("ServiceId");
        ServiceType serviceType = rs.wasNull()
                ? ServiceType.INVALID_SERVICE_TYPE
                : ServiceType.valueOf(serviceId);

        return new ServiceIncidentCount(
                serviceType.getDescription(),
                rs.getInt("IncidentCount")
        );
    }

    private Map<LocalDate, Integer> createEmptyTrendMap(int days) {
        Map<LocalDate, Integer> trend = new LinkedHashMap<>();
        LocalDate firstDate = LocalDate.now().minusDays(days - 1);

        for (int i = 0; i < days; i++) {
            trend.put(firstDate.plusDays(i), 0);
        }

        return trend;
    }

    private void createIncident(ServiceType serviceType, String module, SeverityType severityType, Throwable throwable) {

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(INSERT_INCIDENT_SQL)) {


                setNullableInt(ps, 1, getWebSession().getCustomerId());
                setNullableInt(ps, 2, getWebSession().getProjectId());
                setNullableInt(ps, 3, getWebSession().getUserId());
                ps.setInt(4, serviceType.getId());
                ps.setInt(5, severityType.getId());
                ps.setString(6, getErrorMessage(throwable));
                ps.setString(7, module);
                ps.setString(8, getModuleInfo(throwable));
                ps.setString(9, throwableToString(throwable));
                ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert incident failed : " + serviceType.getDescription() + " " + severityType.getDescription() + " " + getErrorMessage(throwable));
                }
            }
        } catch (Exception e) {
            log.error("Failed to incident event", e);
        }
    }

    private String getErrorMessage(Throwable throwable) {
        return throwable != null ? throwable.getMessage() : "";
    }

    private String getModuleInfo(Throwable throwable) {
        String moduleInfo = null;

        if (throwable != null && throwable.getCause() != null) {

            for (StackTraceElement stackTraceElement : throwable.getCause().getStackTrace()) {
                if (moduleInfo == null && stackTraceElement.getClassName().contains("com.bepa.eis")) {
                    moduleInfo = stackTraceElement.getFileName().replace(".java", "") + " - " + stackTraceElement.getMethodName() + "(" + stackTraceElement.getLineNumber() + ")";
                }
            }

            if (moduleInfo == null) {
                for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                    if (moduleInfo == null && stackTraceElement.getClassName().contains("com.bepa.eis")) {
                        moduleInfo = stackTraceElement.getFileName().replace(".java", "") + " - " + stackTraceElement.getMethodName() + "(" + stackTraceElement.getLineNumber() + ")";
                    }
                }
            }
        }

        return moduleInfo != null ? moduleInfo : "Unknown";
    }

    public String throwableToString(Throwable throwable) {
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        }
        return null;
    }

    private String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }

    public record RecentIncident(
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

    public record ServiceIncidentCount(
            String service,
            int count
    ) {
    }

    public record IncidentTrendPoint(
            String period,
            int count
    ) {
    }

    private void setNullableInt(
            PreparedStatement statement,
            int parameterIndex,
            Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.INTEGER);
            return;
        }
        statement.setInt(parameterIndex, value);
    }

    private int sanitizeRecentIncidentLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }

        return Math.min(limit, 1000);
    }

    public record RecentIncidentDetail(
            int incidentId,
            String logCreated,
            String customer,
            String project,
            String user,
            String serviceType,
            String severityType,
            String module,
            String message,
            String trace
    ) {
    }
}
