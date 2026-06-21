package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PerformanceProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(PerformanceProvider.class);

    private static final String INSERT_PERFORMANCE_SQL =
            "INSERT INTO PERFORMANCE " +
                    " (CustomerId, ProjectId, UserId, Module, DurationMs, Created) " +
                    " VALUES (?, ?, ?, ?, ?, ?)";

    private static final String GET_PERFORMANCE_KPIS_LAST_7_DAYS_SQL = """
            SELECT
                SUM(CASE WHEN DurationMs BETWEEN 0 AND 999 THEN 1 ELSE 0 END) AS GoodPerformanceCount,
                SUM(CASE WHEN DurationMs BETWEEN 1000 AND 2999 THEN 1 ELSE 0 END) AS AcceptablePerformanceCount,
                SUM(CASE WHEN DurationMs >= 3000 THEN 1 ELSE 0 END) AS PoorPerformanceCount
            FROM PERFORMANCE
            WHERE Created >= DATEADD(DAY, -7, SYSUTCDATETIME())
            """;

    private static final String GET_MODULE_PERFORMANCE_LAST_7_DAYS_SQL = """
            SELECT
                Module,
                AVG(CAST(DurationMs AS DECIMAL(18, 2))) AS AvgDurationMs,
                COUNT(*) AS MeasurementCount,
                SUM(CASE WHEN DurationMs BETWEEN 0 AND 999 THEN 1 ELSE 0 END) AS GoodPerformanceCount,
                SUM(CASE WHEN DurationMs BETWEEN 1000 AND 2999 THEN 1 ELSE 0 END) AS AcceptablePerformanceCount,
                SUM(CASE WHEN DurationMs >= 3000 THEN 1 ELSE 0 END) AS PoorPerformanceCount
            FROM PERFORMANCE
            WHERE Created >= DATEADD(DAY, -7, SYSUTCDATETIME())
            GROUP BY Module
            ORDER BY Module
            """;

    private static final String GET_PROJECT_MODULE_PERFORMANCE_LAST_7_DAYS_SQL = """
            SELECT
                ISNULL(P.ProjectName, '') AS ProjectName,
                PE.Module,
                AVG(CAST(PE.DurationMs AS DECIMAL(18, 2))) AS AvgDurationMs,
                COUNT(*) AS MeasurementCount,
                SUM(CASE WHEN PE.DurationMs BETWEEN 0 AND 999 THEN 1 ELSE 0 END) AS GoodPerformanceCount,
                SUM(CASE WHEN PE.DurationMs BETWEEN 1000 AND 2999 THEN 1 ELSE 0 END) AS AcceptablePerformanceCount,
                SUM(CASE WHEN PE.DurationMs >= 3000 THEN 1 ELSE 0 END) AS PoorPerformanceCount
            FROM PERFORMANCE PE
            LEFT JOIN CUSTOMER C
                ON PE.CustomerId = C.CustomerId
            LEFT JOIN PROJECT P
                ON PE.ProjectId = P.ProjectId
               AND PE.CustomerId = P.CustomerId
            WHERE PE.Created >= DATEADD(DAY, -7, SYSUTCDATETIME())
            GROUP BY
                ISNULL(P.ProjectName, ''),
                PE.Module
            ORDER BY
                ISNULL(P.ProjectName, ''),
                PE.Module
            """;

    private static final String GET_RECENT_PERFORMANCE_MEASUREMENTS_SQL = """
            SELECT TOP 100
                PE.Created,
                ISNULL(C.CustomerName, '') AS CustomerName,
                ISNULL(P.ProjectName, '') AS ProjectName,
                PE.Module,
                PE.DurationMs,
                CASE
                    WHEN PE.DurationMs BETWEEN 0 AND 999 THEN 'Good'
                    WHEN PE.DurationMs BETWEEN 1000 AND 2999 THEN 'Acceptable'
                    WHEN PE.DurationMs >= 3000 THEN 'Poor'
                    ELSE 'Unknown'
                END AS PerformanceInterval
            FROM PERFORMANCE PE
            LEFT JOIN CUSTOMER C
                ON PE.CustomerId = C.CustomerId
            LEFT JOIN PROJECT P
                ON PE.ProjectId = P.ProjectId
               AND PE.CustomerId = P.CustomerId
            ORDER BY PE.PerformanceId DESC
            """;

    public PerformanceProvider(WebSession webSession) {
        super(webSession);
    }

    public void logPerformance(String module, long durationMs) {

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(INSERT_PERFORMANCE_SQL)) {

                Integer customerId = null;
                Integer projectId = null;
                Integer userId = null;
                if (getWebSession() != null) {
                    customerId = getWebSession().getCustomerId();
                    projectId = getWebSession().getProjectId();
                    userId = getWebSession().getUserId();
                }

                setNullableInt(ps, 1, customerId);
                setNullableInt(ps, 2, projectId);
                setNullableInt(ps, 3, userId);
                ps.setString(4, module);
                ps.setInt(5, (int) durationMs);
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert performance failed : " + module + " " + durationMs);
                }
            }
        } catch (Exception e) {
            log.error("Failed to Insert event", e);
        }
    }

    public PerformanceKpis getPerformanceKpisLast7Days() throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_PERFORMANCE_KPIS_LAST_7_DAYS_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new PerformanceKpis(
                        rs.getInt("GoodPerformanceCount"),
                        rs.getInt("AcceptablePerformanceCount"),
                        rs.getInt("PoorPerformanceCount")
                );
            }
        }

        return new PerformanceKpis(0, 0, 0);
    }

    public List<ModulePerformance> getModulePerformanceLast7Days() throws SQLException {
        List<ModulePerformance> modulePerformanceList = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_MODULE_PERFORMANCE_LAST_7_DAYS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                modulePerformanceList.add(new ModulePerformance(
                        rs.getString("Module"),
                        rs.getDouble("AvgDurationMs"),
                        rs.getInt("MeasurementCount"),
                        rs.getInt("GoodPerformanceCount"),
                        rs.getInt("AcceptablePerformanceCount"),
                        rs.getInt("PoorPerformanceCount")
                ));
            }
        }

        return modulePerformanceList;
    }

    public List<ProjectModulePerformance> getProjectModulePerformanceLast7Days() throws SQLException {
        List<ProjectModulePerformance> projectModulePerformanceList = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_PROJECT_MODULE_PERFORMANCE_LAST_7_DAYS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                projectModulePerformanceList.add(new ProjectModulePerformance(
                        rs.getString("ProjectName"),
                        rs.getString("Module"),
                        rs.getDouble("AvgDurationMs"),
                        rs.getInt("MeasurementCount"),
                        rs.getInt("GoodPerformanceCount"),
                        rs.getInt("AcceptablePerformanceCount"),
                        rs.getInt("PoorPerformanceCount")
                ));
            }
        }

        return projectModulePerformanceList;
    }

    public List<RecentPerformanceMeasurement> getRecentPerformanceMeasurements() throws SQLException {
        List<RecentPerformanceMeasurement> recentPerformanceMeasurements = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_RECENT_PERFORMANCE_MEASUREMENTS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp created = rs.getTimestamp("Created");

                recentPerformanceMeasurements.add(new RecentPerformanceMeasurement(
                        toIsoString(created),
                        rs.getString("CustomerName"),
                        rs.getString("ProjectName"),
                        rs.getString("Module"),
                        rs.getInt("DurationMs"),
                        rs.getString("PerformanceInterval")
                ));
            }
        }

        return recentPerformanceMeasurements;
    }

    private String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }

    public record PerformanceKpis(
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount
    ) {
    }

    public record ModulePerformance(
            String module,
            double avgDurationMs,
            int count,
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount
    ) {
    }

    public record ProjectModulePerformance(
            String project,
            String module,
            double avgDurationMs,
            int count,
            int goodPerformanceCount,
            int acceptablePerformanceCount,
            int poorPerformanceCount
    ) {
    }

    public record RecentPerformanceMeasurement(
            String created,
            String customer,
            String project,
            String module,
            int durationMs,
            String performanceInterval
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

}