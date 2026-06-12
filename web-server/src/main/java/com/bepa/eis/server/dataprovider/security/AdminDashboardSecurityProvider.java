package com.bepa.eis.server.dataprovider.security;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.ChartItem;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.SecurityStatusRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardSecurityProvider extends GenericProvider {

    private static final int FAILED_LOGIN_TREND_DAYS = 10;
    private static final int LOGIN_ACTIVITY_HOURS = 168;
    private static final int LOGIN_HEALTH_DAYS = 7;
    private static final int ACTIVE_USER_DAYS = 30;
    private static final int LOGIN_COUNTRY_DAYS = 30;
    private static final int ADMIN_CHANGE_DAYS = 7;

    public AdminDashboardSecurityProvider(WebSession webSession) {
        super(webSession);
    }

    public SecurityDashboardData loadSecurityDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int activeUsers = queryInt(connection, """
                    SELECT COUNT(DISTINCT UserId)
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE Success = 1
                      AND LoginTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                      AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                      AND UserId IS NOT NULL
                    """, -(ACTIVE_USER_DAYS - 1));

            int loginsToday = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE Success = 1
                      AND LoginTime >= CAST(GETDATE() AS date)
                      AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                    """);

            int failedLogins = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE Success = 0
                      AND LoginTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                      AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                    """, -(LOGIN_HEALTH_DAYS - 1));

            int lockedUsers = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[USERS]
                    WHERE LockedUntil IS NOT NULL
                      AND LockedUntil > GETDATE()
                    """);

            int newUsers = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[USERS]
                    WHERE Created >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)
                      AND Created < DATEADD(month, 1, DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1))
                    """);

            int mfaCoverage = queryInt(connection, """
                    SELECT ISNULL(CAST(
                        100.0 * SUM(CASE WHEN MfaEnabled = 1 THEN 1 ELSE 0 END)
                        / NULLIF(COUNT(*), 0)
                        AS int
                    ), 0)
                    FROM [dbo].[USERS]
                    WHERE Active = 1
                    """);

            int adminChanges = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[AUDIT_EVENT]
                    WHERE EventType IN ('ROLE_CHANGED', 'ADMIN_CREATED', 'PERMISSION_CHANGED')
                      AND EventTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                      AND EventTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                    """, -(ADMIN_CHANGE_DAYS - 1));

            LoginHealth loginHealth = loadLoginHealth(connection);
            List<Integer> failedLoginTrend = loadFailedLoginTrend(connection);
            List<HourlyLoginPoint> hourlyLoginActivity7Days = loadHourlyLoginActivity7Days(connection);
            List<LoginCountryRow> loginsByCountry = loadLoginsByCountry(connection);
            List<RecentLoginRow> recentLogins = loadRecentLogins(connection);
            List<AuditEventRow> auditEvents = loadAuditEvents(connection);

            int securityScore = calculateSecurityScore(
                    failedLogins,
                    lockedUsers,
                    mfaCoverage,
                    adminChanges
            );

            List<ChartItem> loginHealthChart = List.of(
                    new ChartItem("Successful", loginHealth.successful(), "#84d64b"),
                    new ChartItem("Failed", loginHealth.failed(), "#ef4444")
            );

            List<ChartItem> mfaCoverageDistribution = List.of(
                    new ChartItem("MFA enabled", mfaCoverage, "#84d64b"),
                    new ChartItem("Missing MFA", Math.max(100 - mfaCoverage, 0), "#ef4444")
            );

            List<ChartItem> securityEventTypes = List.of(
                    new ChartItem("Failed login", failedLogins, "#ef4444"),
                    new ChartItem("Admin changes", adminChanges, "#f7c948"),
                    new ChartItem("Locked account", lockedUsers, "#8b5cf6"),
                    new ChartItem("New user", newUsers, "#2f9cff")
            );

            List<SecurityStatusRow> securityStatus = buildSecurityStatus(
                    failedLogins,
                    lockedUsers,
                    mfaCoverage,
                    adminChanges,
                    securityScore
            );

            return new SecurityDashboardData(
                    securityScore,
                    failedLogins,
                    lockedUsers,
                    mfaCoverage,
                    adminChanges,
                    newUsers,
                    activeUsers,
                    loginsToday,
                    failedLoginTrend,
                    hourlyLoginActivity7Days,
                    loginsByCountry,
                    loginHealthChart,
                    mfaCoverageDistribution,
                    securityEventTypes,
                    securityStatus,
                    recentLogins,
                    auditEvents
            );
        }
    }

    private LoginHealth loadLoginHealth(Connection connection) throws SQLException {
        String sql = """
                SELECT
                    ISNULL(SUM(CASE WHEN Success = 1 THEN 1 ELSE 0 END), 0) AS Successful,
                    ISNULL(SUM(CASE WHEN Success = 0 THEN 1 ELSE 0 END), 0) AS Failed
                FROM [dbo].[USER_LOGIN_ACTIVITY]
                WHERE LoginTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                  AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, -(LOGIN_HEALTH_DAYS - 1));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new LoginHealth(
                            resultSet.getInt("Successful"),
                            resultSet.getInt("Failed")
                    );
                }
            }
        }

        return new LoginHealth(0, 0);
    }

    private List<Integer> loadFailedLoginTrend(Connection connection) throws SQLException {
        String sql = """
                WITH Days AS (
                    SELECT DATEADD(day, ?, CAST(GETDATE() AS date)) AS LoginDate
                    UNION ALL
                    SELECT DATEADD(day, 1, LoginDate)
                    FROM Days
                    WHERE LoginDate < CAST(GETDATE() AS date)
                ),
                FailedCounts AS (
                    SELECT
                        CAST(LoginTime AS date) AS LoginDate,
                        COUNT(*) AS FailedLogins
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE Success = 0
                      AND LoginTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                      AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                    GROUP BY CAST(LoginTime AS date)
                )
                SELECT
                    ISNULL(FailedCounts.FailedLogins, 0) AS FailedLogins
                FROM Days
                LEFT JOIN FailedCounts
                    ON Days.LoginDate = FailedCounts.LoginDate
                ORDER BY Days.LoginDate
                OPTION (MAXRECURSION 10)
                """;

        List<Integer> values = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int startOffset = -(FAILED_LOGIN_TREND_DAYS - 1);
            statement.setInt(1, startOffset);
            statement.setInt(2, startOffset);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getInt("FailedLogins"));
                }
            }
        }

        return values;
    }

    private List<HourlyLoginPoint> loadHourlyLoginActivity7Days(Connection connection) throws SQLException {
        String sql = """
                WITH Hours AS (
                    SELECT DATEADD(hour, ?, DATEADD(hour, DATEDIFF(hour, 0, GETDATE()), 0)) AS LoginHour
                    UNION ALL
                    SELECT DATEADD(hour, 1, LoginHour)
                    FROM Hours
                    WHERE LoginHour < DATEADD(hour, DATEDIFF(hour, 0, GETDATE()), 0)
                ),
                LoginCounts AS (
                    SELECT
                        DATEADD(hour, DATEDIFF(hour, 0, LoginTime), 0) AS LoginHour,
                        ISNULL(SUM(CASE WHEN Success = 1 THEN 1 ELSE 0 END), 0) AS Successful,
                        ISNULL(SUM(CASE WHEN Success = 0 THEN 1 ELSE 0 END), 0) AS Failed,
                        COUNT(*) AS Total
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE LoginTime >= DATEADD(hour, ?, DATEADD(hour, DATEDIFF(hour, 0, GETDATE()), 0))
                      AND LoginTime < DATEADD(hour, 1, DATEADD(hour, DATEDIFF(hour, 0, GETDATE()), 0))
                    GROUP BY DATEADD(hour, DATEDIFF(hour, 0, LoginTime), 0)
                )
                SELECT
                    Hours.LoginHour,
                    ISNULL(LoginCounts.Successful, 0) AS Successful,
                    ISNULL(LoginCounts.Failed, 0) AS Failed,
                    ISNULL(LoginCounts.Total, 0) AS Total
                FROM Hours
                LEFT JOIN LoginCounts
                    ON Hours.LoginHour = LoginCounts.LoginHour
                ORDER BY Hours.LoginHour
                OPTION (MAXRECURSION 168)
                """;

        List<HourlyLoginPoint> points = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int startOffset = -(LOGIN_ACTIVITY_HOURS - 1);
            statement.setInt(1, startOffset);
            statement.setInt(2, startOffset);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    points.add(new HourlyLoginPoint(
                            toIsoString(resultSet.getTimestamp("LoginHour")),
                            resultSet.getInt("Successful"),
                            resultSet.getInt("Failed"),
                            resultSet.getInt("Total")
                    ));
                }
            }
        }

        return points;
    }

    private List<LoginCountryRow> loadLoginsByCountry(Connection connection) throws SQLException {
        String sql = """
                SELECT TOP 20
                    ISNULL(CountryCode, '??') AS CountryCode,
                    ISNULL(CountryName, 'Unknown') AS CountryName,
                    ISNULL(SUM(CASE WHEN Success = 1 THEN 1 ELSE 0 END), 0) AS Successful,
                    ISNULL(SUM(CASE WHEN Success = 0 THEN 1 ELSE 0 END), 0) AS Failed,
                    COUNT(*) AS Total
                FROM [dbo].[USER_LOGIN_ACTIVITY]
                WHERE LoginTime >= DATEADD(day, ?, CAST(GETDATE() AS date))
                  AND LoginTime < DATEADD(day, 1, CAST(GETDATE() AS date))
                GROUP BY CountryCode, CountryName
                ORDER BY Total DESC
                """;

        List<LoginCountryRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, -(LOGIN_COUNTRY_DAYS - 1));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new LoginCountryRow(
                            resultSet.getString("CountryCode"),
                            resultSet.getString("CountryName"),
                            resultSet.getInt("Successful"),
                            resultSet.getInt("Failed"),
                            resultSet.getInt("Total")
                    ));
                }
            }
        }

        return rows;
    }

    private List<RecentLoginRow> loadRecentLogins(Connection connection) throws SQLException {
        String sql = """
                SELECT TOP 20
                    LoginTime,
                    Email,
                    IpAddress,
                    CountryName,
                    Success,
                    FailureReason
                FROM [dbo].[USER_LOGIN_ACTIVITY]
                ORDER BY LoginTime DESC
                """;

        List<RecentLoginRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                boolean success = resultSet.getBoolean("Success");
                String failureReason = resultSet.getString("FailureReason");

                rows.add(new RecentLoginRow(
                        toIsoString(resultSet.getTimestamp("LoginTime")),
                        resultSet.getString("Email"),
                        resultSet.getString("IpAddress"),
                        resultSet.getString("CountryName"),
                        success,
                        success ? "OK" : safeText(failureReason, "Failed")
                ));
            }
        }

        return rows;
    }

    private List<AuditEventRow> loadAuditEvents(Connection connection) throws SQLException {
        String sql = """
                SELECT TOP 20
                    EventTime,
                    ActorEmail,
                    EventType,
                    EntityType,
                    EntityId,
                    Description,
                    Status
                FROM (
                    SELECT
                        EventTime,
                        ISNULL(ActorEmail, 'system') AS ActorEmail,
                        EventType,
                        EntityType,
                        EntityId,
                        Description,
                        Status
                    FROM [dbo].[AUDIT_EVENT]

                    UNION ALL

                    SELECT
                        LoginTime AS EventTime,
                        ISNULL(Email, 'unknown') AS ActorEmail,
                        CASE
                            WHEN Success = 1 THEN 'LOGIN_SUCCESS'
                            ELSE 'LOGIN_FAILED'
                        END AS EventType,
                        'USER_LOGIN_ACTIVITY' AS EntityType,
                        COALESCE(CAST(UserId AS varchar(50)), IpAddress, SessionId, 'login') AS EntityId,
                        CASE
                            WHEN Success = 1 THEN 'Login successful'
                            ELSE ISNULL(FailureReason, 'Login failed')
                        END AS Description,
                        CASE
                            WHEN Success = 1 THEN 'OK'
                            ELSE 'Warning'
                        END AS Status
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE LoginTime >= DATEADD(day, -30, CAST(GETDATE() AS date))
                ) Events
                ORDER BY EventTime DESC
                """;

        List<AuditEventRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String entityType = resultSet.getString("EntityType");
                String entityId = resultSet.getString("EntityId");
                String description = resultSet.getString("Description");

                rows.add(new AuditEventRow(
                        toIsoString(resultSet.getTimestamp("EventTime")),
                        resultSet.getString("ActorEmail"),
                        safeText(description, resultSet.getString("EventType")),
                        buildAuditObject(entityType, entityId),
                        safeText(resultSet.getString("Status"), "OK")
                ));
            }
        }

        return rows;
    }

    private List<SecurityStatusRow> buildSecurityStatus(
            int failedLogins,
            int lockedUsers,
            int mfaCoverage,
            int adminChanges,
            int securityScore
    ) {
        return List.of(
                new SecurityStatusRow(
                        "Security score",
                        securityScore >= 85 ? "OK" : securityScore >= 70 ? "Warning" : "Critical"
                ),
                new SecurityStatusRow(
                        "Failed logins",
                        failedLogins <= 25 ? "OK" : failedLogins <= 100 ? "Warning" : "Critical"
                ),
                new SecurityStatusRow(
                        "Locked accounts",
                        lockedUsers == 0 ? "OK" : lockedUsers <= 5 ? "Warning" : "Critical"
                ),
                new SecurityStatusRow(
                        "MFA coverage",
                        mfaCoverage >= 90 ? "OK" : mfaCoverage >= 70 ? "Warning" : "Critical"
                ),
                new SecurityStatusRow(
                        "Admin changes",
                        adminChanges <= 10 ? "OK" : adminChanges <= 25 ? "Warning" : "Critical"
                )
        );
    }

    private int queryInt(Connection connection, String sql, int... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setInt(i + 1, parameters[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        return 0;
    }

    private int calculateSecurityScore(
            int failedLogins,
            int lockedUsers,
            int mfaCoverage,
            int adminChanges
    ) {
        int score = 100;

        score -= Math.min(failedLogins / 10, 20);
        score -= Math.min(lockedUsers * 5, 20);
        score -= Math.max(0, 90 - mfaCoverage) / 2;
        score -= Math.min(adminChanges, 10);

        return Math.max(score, 0);
    }

    private String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }

    private String buildAuditObject(String entityType, String entityId) {
        if ((entityType == null || entityType.isBlank()) && (entityId == null || entityId.isBlank())) {
            return "—";
        }

        if (entityType == null || entityType.isBlank()) {
            return entityId;
        }

        if (entityId == null || entityId.isBlank()) {
            return entityType;
        }

        if ("USER_LOGIN_ACTIVITY".equalsIgnoreCase(entityType)) {
            return "Login: " + entityId;
        }

        return entityType + ": " + entityId;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public record SecurityDashboardData(
            int securityScore,
            int failedLogins,
            int lockedUsers,
            int mfaCoverage,
            int adminChanges,
            int newUsers,
            int activeUsers,
            int loginsToday,
            List<Integer> failedLoginTrend,
            List<HourlyLoginPoint> hourlyLoginActivity7Days,
            List<LoginCountryRow> loginsByCountry,
            List<ChartItem> loginHealth,
            List<ChartItem> mfaCoverageDistribution,
            List<ChartItem> securityEventTypes,
            List<SecurityStatusRow> securityStatus,
            List<RecentLoginRow> recentLogins,
            List<AuditEventRow> auditEvents
    ) {
    }

    public record LoginHealth(
            int successful,
            int failed
    ) {
    }

    public record HourlyLoginPoint(
            String hour,
            int successful,
            int failed,
            int total
    ) {
    }

    public record LoginCountryRow(
            String countryCode,
            String countryName,
            int successful,
            int failed,
            int total
    ) {
    }

    public record RecentLoginRow(
            String time,
            String email,
            String ipAddress,
            String countryName,
            boolean success,
            String status
    ) {
    }

    public record AuditEventRow(
            String time,
            String user,
            String action,
            String object,
            String status
    ) {
    }
}