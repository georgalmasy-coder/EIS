package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.common.providers.misc.ComponentHealthProvider;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardOverviewProvider extends GenericProvider {
    public AdminDashboardOverviewProvider() {
        super(null);
    }

    public OverviewResponse loadOverview() throws SQLException {
        return loadOverview(null);
    }

    public OverviewResponse loadOverview(String currentSessionId) throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int timeoutMinutes = GlobalConfiguration.getSessionTimeoutMinutes();
            int onlineUsers;
            int otherOnlineUsers;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(DISTINCT S.UserId) AS OnlineUsers,
                           COUNT(DISTINCT CASE WHEN CurrentUser.UserId IS NULL
                               THEN S.UserId END) AS OtherOnlineUsers
                    FROM [dbo].[SESSION] S
                    INNER JOIN [dbo].[USERS] U ON U.UserId = S.UserId AND U.Active = 1
                    LEFT JOIN (SELECT DISTINCT UserId FROM [dbo].[SESSION] WHERE SessionId = ?) CurrentUser
                        ON CurrentUser.UserId = S.UserId
                    WHERE S.LastAccessed >= DATEADD(minute, -?, GETDATE())
                      AND S.LogoutAt IS NULL AND S.ExpiredAt IS NULL
                    """)) {
                statement.setString(1, currentSessionId);
                statement.setInt(2, timeoutMinutes);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    onlineUsers = rows.getInt("OnlineUsers");
                    otherOnlineUsers = rows.getInt("OtherOnlineUsers");
                }
            }
            int activeCustomers = count(connection, """
                    SELECT COUNT(*) FROM CUSTOMER WHERE Latest = 1 AND CustomerStatus IN (?, ?, ?, ?, ?, ?)
                    """, CustomerStatus.TRIAL_ACTIVE.getId(), CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId(),
                    CustomerStatus.PAYMENT_PENDING.getId(), CustomerStatus.SUBSCRIPTION_ACTIVE.getId(),
                    CustomerStatus.SUBSCRIPTION_EXPIRING.getId(), CustomerStatus.PAYMENT_OVERDUE.getId());
            int activeUsers = count(connection, """
                    SELECT COUNT(DISTINCT UserId) FROM USER_LOGIN_ACTIVITY
                    WHERE Success = 1 AND LoginTime >= CAST(GETDATE() AS date) AND LoginTime <= GETDATE()
                    """);
            int criticalAlerts = count(connection, """
                    SELECT COUNT(*) FROM INCIDENTS WHERE SeverityId = ?
                    AND LogCreated >= CAST(GETDATE() AS date) AND LogCreated <= GETDATE()
                    """, SeverityType.CRITICAL.getId());
            int paymentErrors = count(connection, """
                    SELECT COUNT(*) FROM CUSTOMER_PAYMENT
                    WHERE CreatedAt >= DATEADD(day, -30, SYSUTCDATETIME()) AND CreatedAt <= SYSUTCDATETIME()
                    AND PaymentStatus IN ('FAILED', 'REJECTED', 'EXPIRED', 'TIMED_OUT', 'OVERDUE')
                    """);
            int pendingConfirmations = count(connection, """
                    SELECT COUNT(*) FROM CUSTOMER WHERE Latest = 1 AND CustomerStatus IN (?, ?)
                    """, CustomerStatus.PENDING_CONFIRMATION.getId(), CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId());
            int lockedUsers = count(connection, "SELECT COUNT(*) FROM USERS WHERE LockedUntil > GETDATE()");
            List<Integer> activity = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    WITH Days AS (
                        SELECT DATEADD(day, -N, CAST(GETDATE() AS date)) AS EventDate
                        FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) D(N)
                    )
                    SELECT COUNT(E.EventCreated) AS Events FROM Days D
                    LEFT JOIN EVENT E ON E.EventCreated >= D.EventDate
                        AND E.EventCreated < DATEADD(day, 1, D.EventDate) AND E.EventCreated <= GETDATE()
                    GROUP BY D.EventDate ORDER BY D.EventDate
                    """); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) activity.add(rows.getInt("Events"));
            }
            var mail = new MailProvider(null).getMailQueueStatistics();
            int retrying = mail.getFailedCount() == null ? 0 : mail.getFailedCount();
            int undelivered = mail.getUndeliveredCount() == null ? 0 : mail.getUndeliveredCount();
            var events = new EventProvider(null).getRecentEvent().stream()
                    .map(event -> new EventRow(event.time(), event.type(), event.description(), event.project(), event.processedAt()))
                    .toList();
            var components = new ComponentHealthProvider().load();
            int failedIntegrations = (int) components.stream()
                    .filter(component -> component.id().startsWith("integration.") && "Critical".equals(component.status())).count();
            List<ChartItem> distribution = new ArrayList<>();
            String[] statuses = {"OK", "Warning", "Critical", "Unknown"};
            String[] colors = {"#84d64b", "#f7c948", "#ef4444", "#9ca3af"};
            for (int i = 0; i < statuses.length; i++) {
                String status = statuses[i];
                distribution.add(new ChartItem(status,
                        (int) components.stream().filter(component -> status.equals(component.status())).count(), colors[i]));
            }
            return new OverviewResponse(ComponentHealthProvider.score(components), activeCustomers, activeUsers, criticalAlerts, paymentErrors,
                    pendingConfirmations, retrying, undelivered, failedIntegrations, lockedUsers, activity, distribution, events,
                    components.stream().map(component -> new StatusRow(component.label(), component.status(), component.lastObservedAt())).toList(), components,
                    onlineUsers, otherOnlineUsers, timeoutMinutes);
        }
    }

    private int count(Connection connection, String sql, int... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setInt(i + 1, parameters[i]);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}
