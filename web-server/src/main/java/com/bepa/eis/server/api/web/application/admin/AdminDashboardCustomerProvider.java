package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.ChartItem;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.CountryCustomerRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.CustomerCreationResponse;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.CustomerProblemRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.CustomersResponse;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.FailedCreationAttemptRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.FailedPaymentRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.FeatureUsageRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.ModuleChangeRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.ModuleStatusRow;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.ModulesResponse;
import com.bepa.eis.server.api.web.application.admin.AdminDashboardServlet.SubscriptionsPaymentsResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardCustomerProvider extends GenericProvider {

    private static final int TREND_DAYS = 10;

    public AdminDashboardCustomerProvider(WebSession webSession) {
        super(webSession);
    }

    public CustomersResponse loadCustomersDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int activeCustomers = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] IN (?, ?, ?, ?, ?, ?)
                    """,
                    CustomerStatus.TRIAL_ACTIVE.getId(),
                    CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId(),
                    CustomerStatus.PAYMENT_PENDING.getId(),
                    CustomerStatus.SUBSCRIPTION_ACTIVE.getId(),
                    CustomerStatus.SUBSCRIPTION_EXPIRING.getId(),
                    CustomerStatus.PAYMENT_OVERDUE.getId());

            int newCustomersThisMonth = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM (
                        SELECT
                            [CustomerId],
                            MIN([ChangedDateTime]) AS CreatedDateTime
                        FROM [dbo].[CUSTOMER]
                        GROUP BY [CustomerId]
                    ) CreatedCustomers
                    WHERE CreatedCustomers.CreatedDateTime >= DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1)
                      AND CreatedCustomers.CreatedDateTime < DATEADD(month, 1, DATEFROMPARTS(YEAR(SYSUTCDATETIME()), MONTH(SYSUTCDATETIME()), 1))
                    """);

            int pendingCustomers = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] IN (?, ?, ?)
                    """,
                    CustomerStatus.CREATED.getId(),
                    CustomerStatus.PENDING_CONFIRMATION.getId(),
                    CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId());

            int suspendedCustomers = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] = ?
                    """, CustomerStatus.SUSPENDED.getId());

            int customersWithIssues = queryInt(connection, """
                    SELECT COUNT(DISTINCT C.[CustomerId])
                    FROM [dbo].[CUSTOMER] C
                    LEFT JOIN [dbo].[CUSTOMER_PAYMENT] P
                        ON P.[CustomerId] = C.[CustomerId]
                       AND P.[PaymentStatus] IN ('FAILED', 'REJECTED', 'EXPIRED', 'TIMED_OUT', 'OVERDUE', 'DISPUTED', 'MANUAL_REVIEW')
                    LEFT JOIN [dbo].[CUSTOMER_WORKFLOW] W
                        ON W.[CustomerId] = C.[CustomerId]
                       AND W.[WorkflowStatus] = 'WAITING_FOR_MANUAL_ATTENTION'
                    WHERE C.[Latest] = 1
                      AND (
                            C.[CustomerStatus] IN (?, ?, ?)
                         OR P.[PaymentId] IS NOT NULL
                         OR W.[WorkflowId] IS NOT NULL
                      )
                    """,
                    CustomerStatus.PAYMENT_OVERDUE.getId(),
                    CustomerStatus.SUSPENDED.getId(),
                    CustomerStatus.CANCELLED.getId());

            return new CustomersResponse(
                    activeCustomers,
                    newCustomersThisMonth,
                    pendingCustomers,
                    suspendedCustomers,
                    customersWithIssues,
                    loadCustomerGrowthTrend(connection),
                    loadCustomersByModule(connection),
                    loadCustomerHealth(connection),
                    loadCustomersByCountry(connection),
                    loadCustomersWithProblems(connection)
            );
        }
    }

    public CustomerCreationResponse loadCustomerCreationDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int started = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [ChangedDateTime] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """, -(TREND_DAYS - 1));

            int customerInfo = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerName] IS NOT NULL
                      AND LEN(LTRIM(RTRIM([CustomerName]))) > 0
                      AND [ContactEmail] IS NOT NULL
                      AND LEN(LTRIM(RTRIM([ContactEmail]))) > 0
                      AND [ChangedDateTime] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """, -(TREND_DAYS - 1));

            int payment = queryInt(connection, """
                    SELECT COUNT(DISTINCT [CustomerId])
                    FROM [dbo].[CUSTOMER_PAYMENT]
                    WHERE [CreatedAt] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """, -(TREND_DAYS - 1));

            int confirmed = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] IN (?, ?, ?, ?, ?, ?)
                      AND [ChangedDateTime] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """,
                    CustomerStatus.TRIAL_ACTIVE.getId(),
                    CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId(),
                    CustomerStatus.PAYMENT_PENDING.getId(),
                    CustomerStatus.SUBSCRIPTION_ACTIVE.getId(),
                    CustomerStatus.SUBSCRIPTION_EXPIRING.getId(),
                    CustomerStatus.PAYMENT_OVERDUE.getId(),
                    -(TREND_DAYS - 1));

            int activated = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] = ?
                      AND [ChangedDateTime] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """,
                    CustomerStatus.SUBSCRIPTION_ACTIVE.getId(),
                    -(TREND_DAYS - 1));

            int pendingConfirmations = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER]
                    WHERE [Latest] = 1
                      AND [CustomerStatus] IN (?, ?)
                    """,
                    CustomerStatus.PENDING_CONFIRMATION.getId(),
                    CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId());

            int cvrLookupsToday = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_WORKFLOW_EVENT]
                    WHERE [CreatedAt] >= CAST(SYSUTCDATETIME() AS date)
                      AND [CreatedAt] < DATEADD(day, 1, CAST(SYSUTCDATETIME() AS date))
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%CVR%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%CVR%'
                      )
                    """);

            int cvrFailedToday = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_WORKFLOW_EVENT]
                    WHERE [CreatedAt] >= CAST(SYSUTCDATETIME() AS date)
                      AND [CreatedAt] < DATEADD(day, 1, CAST(SYSUTCDATETIME() AS date))
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%CVR%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%CVR%'
                      )
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%FAIL%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%FAIL%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%ERROR%'
                      )
                    """);

            int paymentValidations = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_PAYMENT]
                    WHERE [CreatedAt] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                    """, -(TREND_DAYS - 1));

            int failedPayments = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_PAYMENT]
                    WHERE [CreatedAt] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                      AND [PaymentStatus] IN (?, ?, ?, ?, ?)
                    """,
                    -(TREND_DAYS - 1),
                    CustomerPaymentStatus.FAILED.getCode(),
                    CustomerPaymentStatus.REJECTED.getCode(),
                    CustomerPaymentStatus.EXPIRED.getCode(),
                    CustomerPaymentStatus.TIMED_OUT.getCode(),
                    CustomerPaymentStatus.OVERDUE.getCode());

            int failedCreations = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_WORKFLOW_EVENT]
                    WHERE [CreatedAt] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%FAIL%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%FAIL%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%ERROR%'
                      )
                    """, -(TREND_DAYS - 1));

            int cvrSuccessRate = percent(cvrLookupsToday - cvrFailedToday, cvrLookupsToday);
            int paymentValidationRate = percent(paymentValidations - failedPayments, paymentValidations);

            return new CustomerCreationResponse(
                    started,
                    customerInfo,
                    payment,
                    confirmed,
                    activated,
                    pendingConfirmations,
                    cvrLookupsToday,
                    cvrSuccessRate,
                    paymentValidationRate,
                    failedCreations,
                    loadCustomerCreationTrend(connection),
                    List.of(
                            new ChartItem("Success", cvrSuccessRate, "#84d64b"),
                            new ChartItem("Failed", Math.max(100 - cvrSuccessRate, 0), "#ef4444")
                    ),
                    loadFailedCreationAttempts(connection)
            );
        }
    }

    public SubscriptionsPaymentsResponse loadSubscriptionsPaymentsDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int mrr = queryInt(connection, """
                    SELECT ISNULL(CAST(SUM(
                        CASE
                            WHEN SP.[BillingPeriodMonths] IS NULL OR SP.[BillingPeriodMonths] <= 0
                                THEN SP.[PriceAmount]
                            ELSE SP.[PriceAmount] / SP.[BillingPeriodMonths]
                        END
                    ) AS int), 0)
                    FROM [dbo].[CUSTOMER_SUBSCRIPTION] CS
                    LEFT JOIN [dbo].[SUBSCRIPTION_PLAN] SP
                        ON SP.[SubscriptionPlanId] = CS.[SubscriptionPlanId]
                    WHERE CS.[SubscriptionStatus] IN (?, ?, ?, ?, ?)
                    """,
                    CustomerSubscriptionStatus.TRIAL.getCode(),
                    CustomerSubscriptionStatus.TRIAL_EXPIRING.getCode(),
                    CustomerSubscriptionStatus.ACTIVE.getCode(),
                    CustomerSubscriptionStatus.EXPIRING.getCode(),
                    CustomerSubscriptionStatus.GRACE_PERIOD.getCode());

            int paymentErrors = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_PAYMENT]
                    WHERE [CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                      AND [PaymentStatus] IN (?, ?, ?, ?, ?)
                    """,
                    CustomerPaymentStatus.FAILED.getCode(),
                    CustomerPaymentStatus.REJECTED.getCode(),
                    CustomerPaymentStatus.EXPIRED.getCode(),
                    CustomerPaymentStatus.TIMED_OUT.getCode(),
                    CustomerPaymentStatus.OVERDUE.getCode());

            int trialsExpiring = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_SUBSCRIPTION]
                    WHERE [SubscriptionStatus] IN (?, ?)
                       OR (
                            [TrialEndAt] IS NOT NULL
                        AND [TrialEndAt] >= SYSUTCDATETIME()
                        AND [TrialEndAt] < DATEADD(day, 7, SYSUTCDATETIME())
                       )
                    """,
                    CustomerSubscriptionStatus.TRIAL.getCode(),
                    CustomerSubscriptionStatus.TRIAL_EXPIRING.getCode());

            int activeStart = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_SUBSCRIPTION]
                    WHERE [CreatedAt] < DATEADD(day, -30, SYSUTCDATETIME())
                      AND [SubscriptionStatus] IN (?, ?, ?, ?, ?)
                    """,
                    CustomerSubscriptionStatus.TRIAL.getCode(),
                    CustomerSubscriptionStatus.TRIAL_EXPIRING.getCode(),
                    CustomerSubscriptionStatus.ACTIVE.getCode(),
                    CustomerSubscriptionStatus.EXPIRING.getCode(),
                    CustomerSubscriptionStatus.GRACE_PERIOD.getCode());

            int cancelledLast30Days = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_SUBSCRIPTION]
                    WHERE [UpdatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                      AND [SubscriptionStatus] = ?
                    """, CustomerSubscriptionStatus.CANCELLED.getCode());

            double churn = roundDouble(activeStart == 0
                    ? 0.0
                    : ((double) cancelledLast30Days / activeStart) * 100.0, 1);

            int successfulPayments = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_PAYMENT]
                    WHERE [CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                      AND [PaymentStatus] IN (?, ?)
                    """,
                    CustomerPaymentStatus.CAPTURED.getCode(),
                    CustomerPaymentStatus.SUCCEEDED.getCode());

            return new SubscriptionsPaymentsResponse(
                    mrr,
                    mrr * 12,
                    paymentErrors,
                    trialsExpiring,
                    churn,
                    loadMrrTrend(connection),
                    loadRevenueByModule(connection),
                    loadSubscriptionStatusDistribution(connection),
                    List.of(
                            new ChartItem("Successful", successfulPayments, "#84d64b"),
                            new ChartItem("Failed", paymentErrors, "#ef4444")
                    ),
                    loadFailedPayments(connection)
            );
        }
    }

    public ModulesResponse loadModulesDashboardData() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int basisCustomers = loadModuleCustomerCount(connection, "BASIS");
            int proCustomers = loadModuleCustomerCount(connection, "PRO");
            int masterCustomers = loadModuleCustomerCount(connection, "MASTER");

            int upgrades = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_WORKFLOW_EVENT]
                    WHERE [CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%UPGRADE%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%UPGRADE%'
                      )
                    """);

            int downgrades = queryInt(connection, """
                    SELECT COUNT(*)
                    FROM [dbo].[CUSTOMER_WORKFLOW_EVENT]
                    WHERE [CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                      AND (
                            UPPER(ISNULL([EventType], '')) LIKE '%DOWNGRADE%'
                         OR UPPER(ISNULL([Description], '')) LIKE '%DOWNGRADE%'
                      )
                    """);

            return new ModulesResponse(
                    basisCustomers,
                    proCustomers,
                    masterCustomers,
                    upgrades,
                    downgrades,
                    loadModuleAdoptionTrend(connection),
                    loadModuleDistribution(connection),
                    loadFeatureUsage(connection),
                    loadModuleStatus(connection),
                    loadRecentModuleChanges(connection)
            );
        }
    }

    private List<Integer> loadCustomerGrowthTrend(Connection connection) throws SQLException {
        return queryTrend(
                connection,
                """
                        WITH Days AS (
                            SELECT DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date)) AS DayDate
                            UNION ALL
                            SELECT DATEADD(day, 1, DayDate)
                            FROM Days
                            WHERE DayDate < CAST(SYSUTCDATETIME() AS date)
                        ),
                        CreatedCustomers AS (
                            SELECT
                                [CustomerId],
                                MIN([ChangedDateTime]) AS CreatedDateTime
                            FROM [dbo].[CUSTOMER]
                            GROUP BY [CustomerId]
                        )
                        SELECT COUNT(CreatedCustomers.[CustomerId]) AS ItemCount
                        FROM Days
                        LEFT JOIN CreatedCustomers
                            ON CreatedCustomers.CreatedDateTime < DATEADD(day, 1, Days.DayDate)
                        GROUP BY Days.DayDate
                        ORDER BY Days.DayDate
                        OPTION (MAXRECURSION 10)
                        """,
                -(TREND_DAYS - 1)
        );
    }

    private List<ChartItem> loadCustomersByModule(Connection connection) throws SQLException {
        List<ChartItem> items = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    ISNULL([ModuleName], [ModuleCode]) AS ModuleName,
                    COUNT(DISTINCT [CustomerId]) AS ItemCount
                FROM [dbo].[CUSTOMER_MODULE]
                WHERE [Latest] = 1
                  AND [CustomerModuleStatus] IN (?, ?)
                GROUP BY ISNULL([ModuleName], [ModuleCode])
                ORDER BY ItemCount DESC
                """)) {

            statement.setInt(1, CustomerModuleStatus.ACTIVE.getId());
            statement.setInt(2, CustomerModuleStatus.TRIAL.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                int index = 0;

                while (resultSet.next()) {
                    items.add(new ChartItem(
                            safeText(resultSet.getString("ModuleName"), "Unknown"),
                            resultSet.getInt("ItemCount"),
                            chartColor(index++)
                    ));
                }
            }
        }

        return items;
    }

    private List<ChartItem> loadCustomerHealth(Connection connection) throws SQLException {
        int healthy = queryInt(connection, """
                SELECT COUNT(*)
                FROM [dbo].[CUSTOMER]
                WHERE [Latest] = 1
                  AND [CustomerStatus] IN (?, ?, ?)
                """,
                CustomerStatus.TRIAL_ACTIVE.getId(),
                CustomerStatus.SUBSCRIPTION_ACTIVE.getId(),
                CustomerStatus.SUBSCRIPTION_EXPIRING.getId());

        int warning = queryInt(connection, """
                SELECT COUNT(*)
                FROM [dbo].[CUSTOMER]
                WHERE [Latest] = 1
                  AND [CustomerStatus] IN (?, ?, ?, ?)
                """,
                CustomerStatus.CREATED.getId(),
                CustomerStatus.PENDING_CONFIRMATION.getId(),
                CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId(),
                CustomerStatus.PAYMENT_PENDING.getId());

        int critical = queryInt(connection, """
                SELECT COUNT(*)
                FROM [dbo].[CUSTOMER]
                WHERE [Latest] = 1
                  AND [CustomerStatus] IN (?, ?, ?)
                """,
                CustomerStatus.PAYMENT_OVERDUE.getId(),
                CustomerStatus.SUSPENDED.getId(),
                CustomerStatus.CANCELLED.getId());

        return List.of(
                new ChartItem("Healthy", healthy, "#84d64b"),
                new ChartItem("Warning", warning, "#f7c948"),
                new ChartItem("Critical", critical, "#ef4444")
        );
    }

    private List<CountryCustomerRow> loadCustomersByCountry(Connection connection) throws SQLException {
        List<CountryCustomerRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 10
                    ISNULL(NULLIF(LTRIM(RTRIM([Country])), ''), 'Unknown') AS CountryName,
                    COUNT(*) AS CustomerCount
                FROM [dbo].[CUSTOMER]
                WHERE [Latest] = 1
                GROUP BY ISNULL(NULLIF(LTRIM(RTRIM([Country])), ''), 'Unknown')
                ORDER BY CustomerCount DESC, CountryName ASC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new CountryCustomerRow(
                        resultSet.getString("CountryName"),
                        resultSet.getInt("CustomerCount")
                ));
            }
        }

        return rows;
    }

    private List<CustomerProblemRow> loadCustomersWithProblems(Connection connection) throws SQLException {
        List<CustomerProblemRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 20
                    C.[CustomerName],
                    ISNULL(NULLIF(LTRIM(RTRIM(C.[Country])), ''), 'Unknown') AS CountryName,
                    ISNULL(CM.[ModuleName], CM.[ModuleCode]) AS ModuleName,
                    C.[CustomerStatus],
                    P.[PaymentStatus],
                    P.[FailureReason],
                    W.[WorkflowStatus],
                    W.[LastError]
                FROM [dbo].[CUSTOMER] C
                OUTER APPLY (
                    SELECT TOP 1
                        [ModuleName],
                        [ModuleCode]
                    FROM [dbo].[CUSTOMER_MODULE] CM1
                    WHERE CM1.[CustomerId] = C.[CustomerId]
                      AND CM1.[Latest] = 1
                    ORDER BY CM1.[UpdatedAt] DESC, CM1.[CustomerModuleId] DESC
                ) CM
                OUTER APPLY (
                    SELECT TOP 1
                        [PaymentStatus],
                        [FailureReason],
                        [CreatedAt],
                        [PaymentId]
                    FROM [dbo].[CUSTOMER_PAYMENT] P1
                    WHERE P1.[CustomerId] = C.[CustomerId]
                      AND P1.[PaymentStatus] IN ('FAILED', 'REJECTED', 'EXPIRED', 'TIMED_OUT', 'OVERDUE', 'DISPUTED', 'MANUAL_REVIEW')
                    ORDER BY P1.[CreatedAt] DESC, P1.[PaymentId] DESC
                ) P
                OUTER APPLY (
                    SELECT TOP 1
                        [WorkflowStatus],
                        [LastError],
                        [UpdatedAt],
                        [WorkflowId]
                    FROM [dbo].[CUSTOMER_WORKFLOW] W1
                    WHERE W1.[CustomerId] = C.[CustomerId]
                      AND W1.[WorkflowStatus] = 'WAITING_FOR_MANUAL_ATTENTION'
                    ORDER BY W1.[UpdatedAt] DESC, W1.[WorkflowId] DESC
                ) W
                WHERE C.[Latest] = 1
                  AND (
                        C.[CustomerStatus] IN (?, ?, ?)
                     OR P.[PaymentStatus] IS NOT NULL
                     OR W.[WorkflowStatus] IS NOT NULL
                  )
                ORDER BY
                    CASE
                        WHEN C.[CustomerStatus] IN (?, ?) THEN 1
                        WHEN P.[PaymentStatus] IS NOT NULL THEN 2
                        WHEN W.[WorkflowStatus] IS NOT NULL THEN 3
                        ELSE 4
                    END,
                    C.[CustomerName] ASC
                """)) {

            statement.setInt(1, CustomerStatus.PAYMENT_OVERDUE.getId());
            statement.setInt(2, CustomerStatus.SUSPENDED.getId());
            statement.setInt(3, CustomerStatus.CANCELLED.getId());
            statement.setInt(4, CustomerStatus.SUSPENDED.getId());
            statement.setInt(5, CustomerStatus.CANCELLED.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int customerStatusId = resultSet.getInt("CustomerStatus");
                    CustomerStatus customerStatus = CustomerStatus.fromIdOrDefault(
                            resultSet.wasNull() ? null : customerStatusId,
                            CustomerStatus.CREATED
                    );

                    String issue = buildCustomerIssue(
                            customerStatus,
                            resultSet.getString("PaymentStatus"),
                            resultSet.getString("FailureReason"),
                            resultSet.getString("WorkflowStatus"),
                            resultSet.getString("LastError")
                    );

                    rows.add(new CustomerProblemRow(
                            resultSet.getString("CustomerName"),
                            resultSet.getString("CountryName"),
                            safeText(resultSet.getString("ModuleName"), "—"),
                            customerProblemStatus(customerStatus, resultSet.getString("PaymentStatus"), resultSet.getString("WorkflowStatus")),
                            issue
                    ));
                }
            }
        }

        return rows;
    }

    private String buildCustomerIssue(
            CustomerStatus customerStatus,
            String paymentStatus,
            String failureReason,
            String workflowStatus,
            String lastError
    ) {
        if (customerStatus == CustomerStatus.SUSPENDED) {
            return "Customer is suspended";
        }

        if (customerStatus == CustomerStatus.CANCELLED) {
            return "Customer is cancelled";
        }

        if (customerStatus == CustomerStatus.PAYMENT_OVERDUE) {
            return "Payment is overdue";
        }

        if (paymentStatus != null && !paymentStatus.isBlank()) {
            return safeText(failureReason, "Payment status: " + paymentStatus);
        }

        if (workflowStatus != null && !workflowStatus.isBlank()) {
            return safeText(lastError, "Workflow requires manual attention");
        }

        return "Customer requires attention";
    }

    private String customerProblemStatus(
            CustomerStatus customerStatus,
            String paymentStatus,
            String workflowStatus
    ) {
        if (customerStatus == CustomerStatus.SUSPENDED
                || customerStatus == CustomerStatus.CANCELLED
                || CustomerPaymentStatus.FAILED.getCode().equalsIgnoreCase(paymentStatus)
                || CustomerPaymentStatus.OVERDUE.getCode().equalsIgnoreCase(paymentStatus)) {
            return "Critical";
        }

        if (workflowStatus != null && !workflowStatus.isBlank()) {
            return "Warning";
        }

        return "Warning";
    }

    private List<Integer> loadCustomerCreationTrend(Connection connection) throws SQLException {
        return queryTrend(
                connection,
                """
                        WITH Days AS (
                            SELECT DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date)) AS DayDate
                            UNION ALL
                            SELECT DATEADD(day, 1, DayDate)
                            FROM Days
                            WHERE DayDate < CAST(SYSUTCDATETIME() AS date)
                        ),
                        Counts AS (
                            SELECT CAST([ChangedDateTime] AS date) AS DayDate, COUNT(*) AS ItemCount
                            FROM [dbo].[CUSTOMER]
                            WHERE [Latest] = 1
                              AND [ChangedDateTime] >= DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date))
                            GROUP BY CAST([ChangedDateTime] AS date)
                        )
                        SELECT ISNULL(Counts.ItemCount, 0) AS ItemCount
                        FROM Days
                        LEFT JOIN Counts ON Counts.DayDate = Days.DayDate
                        ORDER BY Days.DayDate
                        OPTION (MAXRECURSION 10)
                        """,
                -(TREND_DAYS - 1),
                -(TREND_DAYS - 1)
        );
    }

    private List<Integer> loadMrrTrend(Connection connection) throws SQLException {
        return queryTrend(
                connection,
                """
                        WITH Days AS (
                            SELECT DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date)) AS DayDate
                            UNION ALL
                            SELECT DATEADD(day, 1, DayDate)
                            FROM Days
                            WHERE DayDate < CAST(SYSUTCDATETIME() AS date)
                        )
                        SELECT ISNULL(CAST(SUM(
                            CASE
                                WHEN CS.[CreatedAt] <= DATEADD(day, 1, Days.DayDate)
                                 AND CS.[SubscriptionStatus] IN ('TRIAL', 'TRIAL_EXPIRING', 'ACTIVE', 'EXPIRING', 'GRACE_PERIOD')
                                THEN
                                    CASE
                                        WHEN SP.[BillingPeriodMonths] IS NULL OR SP.[BillingPeriodMonths] <= 0
                                            THEN SP.[PriceAmount]
                                        ELSE SP.[PriceAmount] / SP.[BillingPeriodMonths]
                                    END
                                ELSE 0
                            END
                        ) AS int), 0) AS ItemCount
                        FROM Days
                        LEFT JOIN [dbo].[CUSTOMER_SUBSCRIPTION] CS
                            ON 1 = 1
                        LEFT JOIN [dbo].[SUBSCRIPTION_PLAN] SP
                            ON SP.[SubscriptionPlanId] = CS.[SubscriptionPlanId]
                        GROUP BY Days.DayDate
                        ORDER BY Days.DayDate
                        OPTION (MAXRECURSION 10)
                        """,
                -(TREND_DAYS - 1)
        );
    }

    private List<Integer> loadModuleAdoptionTrend(Connection connection) throws SQLException {
        return queryTrend(
                connection,
                """
                        WITH Days AS (
                            SELECT DATEADD(day, ?, CAST(SYSUTCDATETIME() AS date)) AS DayDate
                            UNION ALL
                            SELECT DATEADD(day, 1, DayDate)
                            FROM Days
                            WHERE DayDate < CAST(SYSUTCDATETIME() AS date)
                        )
                        SELECT COUNT(DISTINCT CM.[CustomerId]) AS ItemCount
                        FROM Days
                        LEFT JOIN [dbo].[CUSTOMER_MODULE] CM
                            ON CM.[Latest] = 1
                           AND CM.[CreatedAt] < DATEADD(day, 1, Days.DayDate)
                           AND CM.[CustomerModuleStatus] IN (?, ?)
                        GROUP BY Days.DayDate
                        ORDER BY Days.DayDate
                        OPTION (MAXRECURSION 10)
                        """,
                -(TREND_DAYS - 1),
                CustomerModuleStatus.ACTIVE.getId(),
                CustomerModuleStatus.TRIAL.getId()
        );
    }

    private List<FailedCreationAttemptRow> loadFailedCreationAttempts(Connection connection) throws SQLException {
        List<FailedCreationAttemptRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 10
                    E.[CreatedAt],
                    ISNULL(C.[CustomerName], 'Unknown') AS CustomerName,
                    ISNULL(E.[EventType], 'Customer creation') AS StepName,
                    ISNULL(E.[Description], 'Failed') AS ErrorText
                FROM [dbo].[CUSTOMER_WORKFLOW_EVENT] E
                LEFT JOIN [dbo].[CUSTOMER] C
                    ON C.[CustomerId] = E.[CustomerId]
                   AND C.[Latest] = 1
                WHERE E.[CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                  AND (
                        UPPER(ISNULL(E.[EventType], '')) LIKE '%FAIL%'
                     OR UPPER(ISNULL(E.[Description], '')) LIKE '%FAIL%'
                     OR UPPER(ISNULL(E.[Description], '')) LIKE '%ERROR%'
                  )
                ORDER BY E.[CreatedAt] DESC, E.[WorkflowEventId] DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new FailedCreationAttemptRow(
                        formatTime(resultSet.getTimestamp("CreatedAt")),
                        resultSet.getString("CustomerName"),
                        resultSet.getString("StepName"),
                        resultSet.getString("ErrorText"),
                        "Open"
                ));
            }
        }

        return rows;
    }

    private List<ChartItem> loadRevenueByModule(Connection connection) throws SQLException {
        List<ChartItem> items = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    ISNULL(SP.[ModuleName], CS.[SubscriptionPlanName]) AS ModuleName,
                    ISNULL(CAST(SUM(
                        CASE
                            WHEN SP.[BillingPeriodMonths] IS NULL OR SP.[BillingPeriodMonths] <= 0
                                THEN SP.[PriceAmount]
                            ELSE SP.[PriceAmount] / SP.[BillingPeriodMonths]
                        END
                    ) AS decimal(18,2)), 0) AS Revenue
                FROM [dbo].[CUSTOMER_SUBSCRIPTION] CS
                LEFT JOIN [dbo].[SUBSCRIPTION_PLAN] SP
                    ON SP.[SubscriptionPlanId] = CS.[SubscriptionPlanId]
                WHERE CS.[SubscriptionStatus] IN ('TRIAL', 'TRIAL_EXPIRING', 'ACTIVE', 'EXPIRING', 'GRACE_PERIOD')
                GROUP BY ISNULL(SP.[ModuleName], CS.[SubscriptionPlanName])
                ORDER BY Revenue DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            int index = 0;

            while (resultSet.next()) {
                BigDecimal revenue = resultSet.getBigDecimal("Revenue");

                items.add(new ChartItem(
                        safeText(resultSet.getString("ModuleName"), "Unknown"),
                        revenue == null ? 0 : revenue.doubleValue(),
                        chartColor(index++)
                ));
            }
        }

        return items;
    }

    private List<ChartItem> loadSubscriptionStatusDistribution(Connection connection) throws SQLException {
        List<ChartItem> items = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT [SubscriptionStatus], COUNT(*) AS ItemCount
                FROM [dbo].[CUSTOMER_SUBSCRIPTION]
                GROUP BY [SubscriptionStatus]
                ORDER BY ItemCount DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            int index = 0;

            while (resultSet.next()) {
                CustomerSubscriptionStatus status = CustomerSubscriptionStatus.fromCode(
                        resultSet.getString("SubscriptionStatus")
                );

                items.add(new ChartItem(
                        status == null ? resultSet.getString("SubscriptionStatus") : status.getLabel(),
                        resultSet.getInt("ItemCount"),
                        chartColor(index++)
                ));
            }
        }

        return items;
    }

    private List<FailedPaymentRow> loadFailedPayments(Connection connection) throws SQLException {
        List<FailedPaymentRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 10
                    P.[CreatedAt],
                    ISNULL(C.[CustomerName], 'Unknown') AS CustomerName,
                    P.[Amount],
                    P.[Currency],
                    ISNULL(P.[FailureReason], P.[PaymentStatus]) AS FailureReason
                FROM [dbo].[CUSTOMER_PAYMENT] P
                LEFT JOIN [dbo].[CUSTOMER] C
                    ON C.[CustomerId] = P.[CustomerId]
                   AND C.[Latest] = 1
                WHERE P.[PaymentStatus] IN ('FAILED', 'REJECTED', 'EXPIRED', 'TIMED_OUT', 'OVERDUE')
                ORDER BY P.[CreatedAt] DESC, P.[PaymentId] DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new FailedPaymentRow(
                        formatTime(resultSet.getTimestamp("CreatedAt")),
                        resultSet.getString("CustomerName"),
                        formatAmount(
                                resultSet.getBigDecimal("Amount"),
                                resultSet.getString("Currency")
                        ),
                        resultSet.getString("FailureReason"),
                        "Open"
                ));
            }
        }

        return rows;
    }

    private int loadModuleCustomerCount(
            Connection connection,
            String moduleNamePart
    ) throws SQLException {
        String searchValue = "%" + moduleNamePart.toUpperCase(Locale.ENGLISH) + "%";

        return queryInt(connection, """
                SELECT COUNT(DISTINCT [CustomerId])
                FROM [dbo].[CUSTOMER_MODULE]
                WHERE [Latest] = 1
                  AND [CustomerModuleStatus] IN (?, ?)
                  AND (
                        UPPER(ISNULL([ModuleCode], '')) LIKE ?
                     OR UPPER(ISNULL([ModuleName], '')) LIKE ?
                  )
                """,
                CustomerModuleStatus.ACTIVE.getId(),
                CustomerModuleStatus.TRIAL.getId(),
                searchValue,
                searchValue);
    }

    private List<ChartItem> loadModuleDistribution(Connection connection) throws SQLException {
        return loadCustomersByModule(connection);
    }

    private List<FeatureUsageRow> loadFeatureUsage(Connection connection) throws SQLException {
        List<FeatureUsageRow> rows = new ArrayList<>();

        if (!tableExists(connection, "PERFORMANCE_MEASUREMENT")) {
            return rows;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 10
                    [Module],
                    COUNT(*) AS UsageCount
                FROM [dbo].[PERFORMANCE_MEASUREMENT]
                WHERE [Created] >= DATEADD(day, -30, SYSUTCDATETIME())
                  AND [Module] IS NOT NULL
                  AND LEN(LTRIM(RTRIM([Module]))) > 0
                GROUP BY [Module]
                ORDER BY UsageCount DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new FeatureUsageRow(
                        resultSet.getString("Module"),
                        resultSet.getInt("UsageCount")
                ));
            }
        }

        return rows;
    }

    private List<ModuleStatusRow> loadModuleStatus(Connection connection) throws SQLException {
        List<ModuleStatusRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    [ModuleName],
                    [Active]
                FROM [dbo].[SUBSCRIPTION_PLAN]
                ORDER BY [ModuleName], [PlanName]
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new ModuleStatusRow(
                        resultSet.getString("ModuleName"),
                        resultSet.getBoolean("Active") ? "Available" : "Disabled"
                ));
            }
        }

        return rows;
    }

    private List<ModuleChangeRow> loadRecentModuleChanges(Connection connection) throws SQLException {
        List<ModuleChangeRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP 10
                    E.[CreatedAt],
                    ISNULL(C.[CustomerName], 'Unknown') AS CustomerName,
                    ISNULL(E.[Description], E.[EventType]) AS ChangeText,
                    ISNULL(E.[FromState], '—') AS FromValue,
                    ISNULL(E.[ToState], '—') AS ToValue
                FROM [dbo].[CUSTOMER_WORKFLOW_EVENT] E
                LEFT JOIN [dbo].[CUSTOMER] C
                    ON C.[CustomerId] = E.[CustomerId]
                   AND C.[Latest] = 1
                WHERE E.[CreatedAt] >= DATEADD(day, -30, SYSUTCDATETIME())
                  AND (
                        UPPER(ISNULL(E.[EventType], '')) LIKE '%MODULE%'
                     OR UPPER(ISNULL(E.[EventType], '')) LIKE '%UPGRADE%'
                     OR UPPER(ISNULL(E.[EventType], '')) LIKE '%DOWNGRADE%'
                     OR UPPER(ISNULL(E.[Description], '')) LIKE '%MODULE%'
                     OR UPPER(ISNULL(E.[Description], '')) LIKE '%UPGRADE%'
                     OR UPPER(ISNULL(E.[Description], '')) LIKE '%DOWNGRADE%'
                  )
                ORDER BY E.[CreatedAt] DESC, E.[WorkflowEventId] DESC
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new ModuleChangeRow(
                        formatTime(resultSet.getTimestamp("CreatedAt")),
                        resultSet.getString("CustomerName"),
                        resultSet.getString("ChangeText"),
                        resultSet.getString("FromValue"),
                        resultSet.getString("ToValue"),
                        "OK"
                ));
            }
        }

        return rows;
    }

    private List<Integer> queryTrend(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        List<Integer> values = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getInt("ItemCount"));
                }
            }
        }

        while (values.size() < TREND_DAYS) {
            values.add(0);
        }

        if (values.size() > TREND_DAYS) {
            return values.subList(values.size() - TREND_DAYS, values.size());
        }

        return values;
    }

    private int queryInt(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        return 0;
    }

    private void bindParameters(
            PreparedStatement statement,
            Object... parameters
    ) throws SQLException {
        if (parameters == null) {
            return;
        }

        for (int i = 0; i < parameters.length; i++) {
            Object value = parameters[i];
            int parameterIndex = i + 1;

            if (value instanceof Integer integerValue) {
                statement.setInt(parameterIndex, integerValue);
            } else if (value instanceof String stringValue) {
                statement.setString(parameterIndex, stringValue);
            } else if (value instanceof Long longValue) {
                statement.setLong(parameterIndex, longValue);
            } else if (value instanceof BigDecimal bigDecimalValue) {
                statement.setBigDecimal(parameterIndex, bigDecimalValue);
            } else {
                statement.setObject(parameterIndex, value);
            }
        }
    }

    private boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'dbo'
                  AND TABLE_NAME = ?
                """)) {

            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private int percent(
            int numerator,
            int denominator
    ) {
        if (denominator <= 0) {
            return 0;
        }

        return Math.max(0, Math.min(100, (int) Math.round((numerator * 100.0) / denominator)));
    }

    private double roundDouble(
            double value,
            int decimals
    ) {
        double multiplier = Math.pow(10, decimals);
        return Math.round(value * multiplier) / multiplier;
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        LocalDateTime value = timestamp.toLocalDateTime();

        return String.format(
                "%02d:%02d",
                value.getHour(),
                value.getMinute()
        );
    }

    private String formatAmount(
            BigDecimal amount,
            String currency
    ) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = safeText(currency, "EUR");

        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("da", "DK"));
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);

        return numberFormat.format(safeAmount.setScale(2, RoundingMode.HALF_UP)) + " " + safeCurrency;
    }

    private String chartColor(int index) {
        String[] colors = {
                "#2f9cff",
                "#8b5cf6",
                "#84d64b",
                "#f7c948",
                "#ef4444",
                "#fb923c"
        };

        return colors[Math.floorMod(index, colors.length)];
    }

    private String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}