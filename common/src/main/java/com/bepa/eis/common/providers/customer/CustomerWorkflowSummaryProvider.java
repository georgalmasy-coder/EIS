package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.CustomerWorkflowSummary;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerWorkflowSummaryProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowSummaryProvider.class);

    private static final String SELECT_WORKFLOW_SUMMARIES_SQL =
            "SELECT TOP (?) " +
                    "CW.WorkflowId, " +
                    "CW.CustomerId, " +
                    "CW.WorkflowType, " +
                    "CW.WorkflowStatus, " +
                    "CW.CurrentState, " +
                    "CW.SubscriptionId, " +
                    "CS.SubscriptionStatus, " +
                    "CS.TrialEndAt, " +
                    "CS.PeriodEndAt, " +
                    "CW.PaymentId, " +
                    "CP.PaymentStatus, " +
                    "CP.PaymentDueAt, " +
                    "CP.GracePeriodEndsAt AS PaymentGracePeriodEndsAt, " +
                    "CW.NextActionAt, " +
                    "CW.RetryCount, " +
                    "CW.LastEventType, " +
                    "CW.LastEventAt, " +
                    "CW.LastError, " +
                    "CW.LockedAt, " +
                    "CW.LockedBy, " +
                    "CW.CreatedAt, " +
                    "CW.UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] CW " +
                    "LEFT JOIN [dbo].[CUSTOMER_SUBSCRIPTION] CS " +
                    "  ON CW.SubscriptionId = CS.SubscriptionId " +
                    "LEFT JOIN [dbo].[CUSTOMER_PAYMENT] CP " +
                    "  ON CW.PaymentId = CP.PaymentId " +
                    "ORDER BY CW.UpdatedAt DESC, CW.WorkflowId DESC ";

    private static final String SELECT_WORKFLOW_SUMMARIES_BY_STATUS_SQL =
            "SELECT TOP (?) " +
                    "CW.WorkflowId, " +
                    "CW.CustomerId, " +
                    "CW.WorkflowType, " +
                    "CW.WorkflowStatus, " +
                    "CW.CurrentState, " +
                    "CW.SubscriptionId, " +
                    "CS.SubscriptionStatus, " +
                    "CS.TrialEndAt, " +
                    "CS.PeriodEndAt, " +
                    "CW.PaymentId, " +
                    "CP.PaymentStatus, " +
                    "CP.PaymentDueAt, " +
                    "CP.GracePeriodEndsAt AS PaymentGracePeriodEndsAt, " +
                    "CW.NextActionAt, " +
                    "CW.RetryCount, " +
                    "CW.LastEventType, " +
                    "CW.LastEventAt, " +
                    "CW.LastError, " +
                    "CW.LockedAt, " +
                    "CW.LockedBy, " +
                    "CW.CreatedAt, " +
                    "CW.UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] CW " +
                    "LEFT JOIN [dbo].[CUSTOMER_SUBSCRIPTION] CS " +
                    "  ON CW.SubscriptionId = CS.SubscriptionId " +
                    "LEFT JOIN [dbo].[CUSTOMER_PAYMENT] CP " +
                    "  ON CW.PaymentId = CP.PaymentId " +
                    "WHERE CW.WorkflowStatus = ? " +
                    "ORDER BY CW.UpdatedAt DESC, CW.WorkflowId DESC ";

    private static final String SELECT_WORKFLOW_SUMMARIES_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (?) " +
                    "CW.WorkflowId, " +
                    "CW.CustomerId, " +
                    "CW.WorkflowType, " +
                    "CW.WorkflowStatus, " +
                    "CW.CurrentState, " +
                    "CW.SubscriptionId, " +
                    "CS.SubscriptionStatus, " +
                    "CS.TrialEndAt, " +
                    "CS.PeriodEndAt, " +
                    "CW.PaymentId, " +
                    "CP.PaymentStatus, " +
                    "CP.PaymentDueAt, " +
                    "CP.GracePeriodEndsAt AS PaymentGracePeriodEndsAt, " +
                    "CW.NextActionAt, " +
                    "CW.RetryCount, " +
                    "CW.LastEventType, " +
                    "CW.LastEventAt, " +
                    "CW.LastError, " +
                    "CW.LockedAt, " +
                    "CW.LockedBy, " +
                    "CW.CreatedAt, " +
                    "CW.UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] CW " +
                    "LEFT JOIN [dbo].[CUSTOMER_SUBSCRIPTION] CS " +
                    "  ON CW.SubscriptionId = CS.SubscriptionId " +
                    "LEFT JOIN [dbo].[CUSTOMER_PAYMENT] CP " +
                    "  ON CW.PaymentId = CP.PaymentId " +
                    "WHERE CW.CustomerId = ? " +
                    "ORDER BY CW.UpdatedAt DESC, CW.WorkflowId DESC ";

    public CustomerWorkflowSummaryProvider(WebSession webSession) {
        super(webSession);
    }

    public List<CustomerWorkflowSummary> getLatestWorkflowSummaries() {
        return getLatestWorkflowSummaries(100);
    }

    public List<CustomerWorkflowSummary> getLatestWorkflowSummaries(int maxRows) {
        List<CustomerWorkflowSummary> summaries = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 1000));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WORKFLOW_SUMMARIES_SQL)) {

            statement.setInt(1, safeMaxRows);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summaries.add(mapSummary(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow summaries.", e);
        }

        enrichCustomerInfo(summaries);

        return summaries;
    }

    public List<CustomerWorkflowSummary> getWorkflowSummariesByStatus(
            String workflowStatus,
            int maxRows
    ) {
        if (workflowStatus == null || workflowStatus.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<CustomerWorkflowSummary> summaries = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 1000));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WORKFLOW_SUMMARIES_BY_STATUS_SQL)) {

            statement.setInt(1, safeMaxRows);
            statement.setString(2, workflowStatus.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summaries.add(mapSummary(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow summaries by status={}", workflowStatus, e);
        }

        enrichCustomerInfo(summaries);

        return summaries;
    }

    public List<CustomerWorkflowSummary> getWorkflowSummariesByCustomerId(
            Integer customerId,
            int maxRows
    ) {
        if (customerId == null) {
            return Collections.emptyList();
        }

        List<CustomerWorkflowSummary> summaries = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 1000));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WORKFLOW_SUMMARIES_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, safeMaxRows);
            statement.setInt(2, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summaries.add(mapSummary(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow summaries by customerId={}", customerId, e);
        }

        enrichCustomerInfo(summaries);

        return summaries;
    }

    private CustomerWorkflowSummary mapSummary(ResultSet resultSet) throws SQLException {
        CustomerWorkflowSummary summary = new CustomerWorkflowSummary();

        int workflowId = resultSet.getInt("WorkflowId");
        summary.setWorkflowId(resultSet.wasNull() ? null : workflowId);

        int customerId = resultSet.getInt("CustomerId");
        summary.setCustomerId(resultSet.wasNull() ? null : customerId);

        summary.setWorkflowType(resultSet.getString("WorkflowType"));
        summary.setWorkflowStatus(resultSet.getString("WorkflowStatus"));
        summary.setCurrentState(resultSet.getString("CurrentState"));

        int subscriptionId = resultSet.getInt("SubscriptionId");
        summary.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        summary.setSubscriptionStatus(resultSet.getString("SubscriptionStatus"));
        summary.setTrialEndAt(resultSet.getTimestamp("TrialEndAt"));
        summary.setPeriodEndAt(resultSet.getTimestamp("PeriodEndAt"));

        int paymentId = resultSet.getInt("PaymentId");
        summary.setPaymentId(resultSet.wasNull() ? null : paymentId);

        summary.setPaymentStatus(resultSet.getString("PaymentStatus"));
        summary.setPaymentDueAt(resultSet.getTimestamp("PaymentDueAt"));
        summary.setPaymentGracePeriodEndsAt(resultSet.getTimestamp("PaymentGracePeriodEndsAt"));

        summary.setNextActionAt(resultSet.getTimestamp("NextActionAt"));

        int retryCount = resultSet.getInt("RetryCount");
        summary.setRetryCount(resultSet.wasNull() ? null : retryCount);

        summary.setLastEventType(resultSet.getString("LastEventType"));
        summary.setLastEventAt(resultSet.getTimestamp("LastEventAt"));
        summary.setLastError(resultSet.getString("LastError"));
        summary.setLockedAt(resultSet.getTimestamp("LockedAt"));
        summary.setLockedBy(resultSet.getString("LockedBy"));
        summary.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        summary.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return summary;
    }

    private void enrichCustomerInfo(List<CustomerWorkflowSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return;
        }

        CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(getWebSession());

        for (CustomerWorkflowSummary summary : summaries) {
            if (summary == null || summary.getCustomerId() == null) {
                continue;
            }

            try {
                CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(summary.getCustomerId());

                if (customer != null) {
                    summary.setCustomerName(customer.getCustomerName());
                    summary.setContactEmail(customer.getContactEmail());
                    summary.setCustomerStatus(customer.getCustomerStatus());
                }
            } catch (RuntimeException e) {
                log.debug(
                        "Could not enrich customer workflow summary with customer info. customerId={}",
                        summary.getCustomerId(),
                        e
                );
            }
        }
    }
}