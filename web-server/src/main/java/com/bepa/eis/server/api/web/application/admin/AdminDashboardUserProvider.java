package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardUserProvider extends GenericProvider {
    private static final String[] ROLE_COLORS = {"#ef4444", "#f7c948", "#84d64b", "#2f9cff", "#9ca3af"};

    public AdminDashboardUserProvider(WebSession webSession) {
        super(webSession);
    }

    public UsersResponse loadUsersDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int activeUsers;
            int loginsToday;
            int failedLogins;
            int successfulLast24Hours;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT
                        COUNT(DISTINCT CASE WHEN Success = 1 AND LoginTime >= CAST(GETDATE() AS date) THEN UserId END) AS ActiveUsers,
                        COUNT(CASE WHEN Success = 1 AND LoginTime >= CAST(GETDATE() AS date) THEN 1 END) AS LoginsToday,
                        COUNT(CASE WHEN Success = 0 THEN 1 END) AS FailedLogins,
                        COUNT(CASE WHEN Success = 1 THEN 1 END) AS SuccessfulLogins
                    FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE LoginTime >= DATEADD(hour, -24, GETDATE()) AND LoginTime <= GETDATE()
                    """); ResultSet rows = statement.executeQuery()) {
                rows.next();
                activeUsers = rows.getInt("ActiveUsers");
                loginsToday = rows.getInt("LoginsToday");
                failedLogins = rows.getInt("FailedLogins");
                successfulLast24Hours = rows.getInt("SuccessfulLogins");
            }

            int lockedAccounts = queryInt(connection, """
                    SELECT COUNT(*) FROM [dbo].[USERS] WHERE LockedUntil > GETDATE()
                    """);
            int newUsers = queryInt(connection, """
                    SELECT COUNT(*) FROM [dbo].[USERS]
                    WHERE Created >= DATEADD(day, -6, CAST(GETDATE() AS date)) AND Created <= GETDATE()
                    """);

            return new UsersResponse(activeUsers, loginsToday, failedLogins, lockedAccounts, newUsers,
                    loadLoginActivity(connection), loadUsersByRole(connection),
                    List.of(new ChartItem("Successful", successfulLast24Hours, "#84d64b"),
                            new ChartItem("Failed", failedLogins, "#ef4444")),
                    loadUsersByCustomer(connection), loadInactiveUsers(connection));
        }
    }

    private List<Integer> loadLoginActivity(Connection connection) throws SQLException {
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                WITH Days AS (
                    SELECT DATEADD(day, -N, CAST(GETDATE() AS date)) AS LoginDate
                    FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) D(N)
                )
                SELECT COUNT(L.LoginTime) AS Logins
                FROM Days D
                LEFT JOIN [dbo].[USER_LOGIN_ACTIVITY] L
                    ON L.LoginTime >= D.LoginDate AND L.LoginTime < DATEADD(day, 1, D.LoginDate)
                    AND L.LoginTime <= GETDATE() AND L.Success = 1
                GROUP BY D.LoginDate ORDER BY D.LoginDate
                """); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) result.add(rows.getInt("Logins"));
        }
        return result;
    }

    private List<ChartItem> loadUsersByRole(Connection connection) throws SQLException {
        List<ChartItem> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT UserRole, COUNT(*) AS Users FROM [dbo].[USERS]
                GROUP BY UserRole ORDER BY UserRole
                """); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                UserRoles role = UserRoles.fromIdOrDefault((Integer) rows.getObject("UserRole"), UserRoles.INVALID_USER_ROLE);
                result.add(new ChartItem(role.getLabel(), rows.getInt("Users"),
                        ROLE_COLORS[Math.floorMod(role.getId() - 1, ROLE_COLORS.length)]));
            }
        }
        return result;
    }

    private List<CustomerUsersRow> loadUsersByCustomer(Connection connection) throws SQLException {
        List<CustomerUsersRow> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP (10) C.CustomerName, COUNT(DISTINCT U.UserId) AS Users
                FROM [dbo].[CUSTOMER] C
                INNER JOIN [dbo].[USER_CUSTOMER] UC ON UC.CustomerId = C.CustomerId
                INNER JOIN [dbo].[USERS] U ON U.UserId = UC.UserId
                WHERE C.Latest = 1
                GROUP BY C.CustomerId, C.CustomerName
                ORDER BY Users DESC, C.CustomerName, C.CustomerId
                """); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) result.add(new CustomerUsersRow(rows.getString("CustomerName"), rows.getInt("Users")));
        }
        return result;
    }

    private List<InactiveUserRow> loadInactiveUsers(Connection connection) throws SQLException {
        List<InactiveUserRow> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP (50) U.Email, U.Name, U.UserRole, L.LastLogin, C.CustomerNames,
                    CASE WHEN U.Active = 0 OR U.LockedUntil > GETDATE()
                        OR COALESCE(L.LastLogin, U.Created) < DATEADD(day, -90, GETDATE())
                        THEN 'Critical' ELSE 'Warning' END AS Status
                FROM [dbo].[USERS] U
                OUTER APPLY (
                    SELECT MAX(LoginTime) AS LastLogin FROM [dbo].[USER_LOGIN_ACTIVITY]
                    WHERE UserId = U.UserId AND Success = 1
                ) L
                OUTER APPLY (
                    SELECT STRING_AGG(X.CustomerName, ', ') AS CustomerNames
                    FROM (
                        SELECT DISTINCT C.CustomerName FROM [dbo].[USER_CUSTOMER] UC
                        INNER JOIN [dbo].[CUSTOMER] C ON C.CustomerId = UC.CustomerId AND C.Latest = 1
                        WHERE UC.UserId = U.UserId
                    ) X
                ) C
                WHERE U.Active = 0 OR U.LockedUntil > GETDATE()
                    OR COALESCE(L.LastLogin, U.Created) < DATEADD(day, -30, GETDATE())
                ORDER BY COALESCE(L.LastLogin, U.Created), U.UserId
                """); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Timestamp lastLogin = rows.getTimestamp("LastLogin");
                UserRoles role = UserRoles.fromIdOrDefault((Integer) rows.getObject("UserRole"), UserRoles.INVALID_USER_ROLE);
                String email = rows.getString("Email");
                result.add(new InactiveUserRow(email == null || email.isBlank() ? rows.getString("Name") : email,
                        rows.getString("CustomerNames"), role.getLabel(),
                        lastLogin == null ? null : lastLogin.toLocalDateTime().toString(), rows.getString("Status")));
            }
        }
        return result;
    }

    private int queryInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
