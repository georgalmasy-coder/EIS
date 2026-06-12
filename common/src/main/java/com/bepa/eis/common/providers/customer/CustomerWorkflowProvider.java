package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CustomerWorkflowProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowProvider.class);

    private static final String INSERT_WORKFLOW_SQL =
            "INSERT INTO [dbo].[CUSTOMER_WORKFLOW] ( " +
                    "CustomerId, " +
                    "WorkflowType, " +
                    "WorkflowStatus, " +
                    "CurrentState, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "NextActionAt, " +
                    "RetryCount, " +
                    "LastEventType, " +
                    "LastEventAt, " +
                    "LastError " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_WORKFLOW_BY_ID_SQL =
            "SELECT " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "WorkflowType, " +
                    "WorkflowStatus, " +
                    "CurrentState, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "NextActionAt, " +
                    "RetryCount, " +
                    "LastEventType, " +
                    "LastEventAt, " +
                    "LastError, " +
                    "LockedAt, " +
                    "LockedBy, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] " +
                    "WHERE WorkflowId = ? ";

    private static final String SELECT_ACTIVE_WORKFLOW_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (1) " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "WorkflowType, " +
                    "WorkflowStatus, " +
                    "CurrentState, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "NextActionAt, " +
                    "RetryCount, " +
                    "LastEventType, " +
                    "LastEventAt, " +
                    "LastError, " +
                    "LockedAt, " +
                    "LockedBy, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] " +
                    "WHERE CustomerId = ? " +
                    "  AND WorkflowStatus = 'ACTIVE' " +
                    "ORDER BY WorkflowId DESC ";

    private static final String SELECT_DUE_WORKFLOWS_SQL =
            "SELECT TOP (?) " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "WorkflowType, " +
                    "WorkflowStatus, " +
                    "CurrentState, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "NextActionAt, " +
                    "RetryCount, " +
                    "LastEventType, " +
                    "LastEventAt, " +
                    "LastError, " +
                    "LockedAt, " +
                    "LockedBy, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW] " +
                    "WHERE WorkflowStatus = 'ACTIVE' " +
                    "  AND (NextActionAt IS NULL OR NextActionAt <= SYSUTCDATETIME()) " +
                    "  AND LockedAt IS NULL " +
                    "ORDER BY " +
                    "  CASE WHEN NextActionAt IS NULL THEN 0 ELSE 1 END ASC, " +
                    "  NextActionAt ASC, " +
                    "  WorkflowId ASC ";

    private static final String LOCK_WORKFLOW_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    LockedAt = SYSUTCDATETIME(), " +
                    "    LockedBy = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE WorkflowId = ? " +
                    "  AND WorkflowStatus = 'ACTIVE' " +
                    "  AND LockedAt IS NULL ";

    private static final String RELEASE_WORKFLOW_LOCK_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE WorkflowId = ? ";

    private static final String UPDATE_WORKFLOW_STATE_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    WorkflowStatus = ?, " +
                    "    CurrentState = ?, " +
                    "    SubscriptionId = ?, " +
                    "    PaymentId = ?, " +
                    "    NextActionAt = ?, " +
                    "    LastEventType = ?, " +
                    "    LastEventAt = SYSUTCDATETIME(), " +
                    "    LastError = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME(), " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL " +
                    "WHERE WorkflowId = ? ";

    private static final String MARK_WORKFLOW_ERROR_SQL =
            "UPDATE [dbo].[CUSTOMER_WORKFLOW] " +
                    "SET " +
                    "    WorkflowStatus = ?, " +
                    "    CurrentState = ?, " +
                    "    RetryCount = RetryCount + 1, " +
                    "    LastEventType = ?, " +
                    "    LastEventAt = SYSUTCDATETIME(), " +
                    "    LastError = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME(), " +
                    "    LockedAt = NULL, " +
                    "    LockedBy = NULL " +
                    "WHERE WorkflowId = ? ";

    private static final String INSERT_WORKFLOW_EVENT_SQL =
            "INSERT INTO [dbo].[CUSTOMER_WORKFLOW_EVENT] ( " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "EventType, " +
                    "EventCategory, " +
                    "FromState, " +
                    "ToState, " +
                    "Description, " +
                    "PayloadJson, " +
                    "CreatedByUserId " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    public CustomerWorkflowProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createWorkflow(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getCustomerId() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_WORKFLOW_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, workflow.getCustomerId());
            statement.setString(2, safeText(workflow.getWorkflowType(), "CUSTOMER_ONBOARDING"));
            statement.setString(3, workflow.getWorkflowStatusCode());
            statement.setString(4, workflow.getCurrentStateCode());
            setNullableInt(statement, 5, workflow.getSubscriptionId());
            setNullableInt(statement, 6, workflow.getPaymentId());
            statement.setTimestamp(7, workflow.getNextActionAt());
            statement.setInt(8, workflow.getRetryCount());
            statement.setString(9, safeText(workflow.getLastEventType(), ""));
            statement.setTimestamp(10, workflow.getLastEventAt());
            statement.setString(11, safeText(workflow.getLastError(), ""));

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer workflowId = generatedKeys.getInt(1);
                    workflow.setWorkflowId(workflowId);
                    return workflowId;
                }
            }
        } catch (SQLException e) {
            log.error("Error creating customer workflow. customerId={}", workflow.getCustomerId(), e);
        }

        return null;
    }

    public CustomerWorkflow getWorkflowById(Integer workflowId) {
        if (workflowId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WORKFLOW_BY_ID_SQL)) {

            statement.setInt(1, workflowId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapWorkflow(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow. workflowId={}", workflowId, e);
        }

        return null;
    }

    public CustomerWorkflow getActiveWorkflowByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_WORKFLOW_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapWorkflow(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading active customer workflow. customerId={}", customerId, e);
        }

        return null;
    }

    public List<CustomerWorkflow> getDueWorkflows(int maxRows) {
        List<CustomerWorkflow> workflows = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 500));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_DUE_WORKFLOWS_SQL)) {

            statement.setInt(1, safeMaxRows);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    workflows.add(mapWorkflow(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading due customer workflows.", e);
        }

        return workflows;
    }

    public boolean lockWorkflow(Integer workflowId, String workerId) {
        if (workflowId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(LOCK_WORKFLOW_SQL)) {

            statement.setString(1, safeText(workerId, "unknown-worker"));
            statement.setInt(2, workflowId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error locking customer workflow. workflowId={}", workflowId, e);
            return false;
        }
    }

    public boolean releaseWorkflowLock(Integer workflowId) {
        if (workflowId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(RELEASE_WORKFLOW_LOCK_SQL)) {

            statement.setInt(1, workflowId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error releasing customer workflow lock. workflowId={}", workflowId, e);
            return false;
        }
    }

    public boolean updateWorkflowState(
            CustomerWorkflow workflow,
            CustomerWorkflowEventType eventType,
            String errorMessage
    ) {
        if (workflow == null || workflow.getWorkflowId() == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_WORKFLOW_STATE_SQL)) {

            statement.setString(1, workflow.getWorkflowStatusCode());
            statement.setString(2, workflow.getCurrentStateCode());
            setNullableInt(statement, 3, workflow.getSubscriptionId());
            setNullableInt(statement, 4, workflow.getPaymentId());
            statement.setTimestamp(5, workflow.getNextActionAt());
            statement.setString(6, eventType == null ? "" : eventType.getCode());
            statement.setString(7, safeText(errorMessage, ""));
            statement.setInt(8, workflow.getWorkflowId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating customer workflow state. workflowId={}", workflow.getWorkflowId(), e);
            return false;
        }
    }

    public boolean markWorkflowError(
            Integer workflowId,
            CustomerWorkflowState currentState,
            CustomerWorkflowEventType eventType,
            String errorMessage,
            boolean manualAttentionRequired
    ) {
        if (workflowId == null) {
            return false;
        }

        CustomerWorkflowStatus workflowStatus = manualAttentionRequired
                ? CustomerWorkflowStatus.WAITING_FOR_MANUAL_ATTENTION
                : CustomerWorkflowStatus.ACTIVE;

        CustomerWorkflowState safeState = currentState == null
                ? CustomerWorkflowState.WAITING_FOR_MANUAL_ATTENTION
                : currentState;

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_WORKFLOW_ERROR_SQL)) {

            statement.setString(1, workflowStatus.getCode());
            statement.setString(2, safeState.getCode());
            statement.setString(3, eventType == null ? CustomerWorkflowEventType.WORKFLOW_ERROR.getCode() : eventType.getCode());
            statement.setString(4, truncate(safeText(errorMessage, "Unknown customer workflow error"), 4000));
            statement.setInt(5, workflowId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking customer workflow as failed. workflowId={}", workflowId, e);
            return false;
        }
    }

    public Integer createWorkflowEvent(CustomerWorkflowEvent event) {
        if (event == null || event.getWorkflowId() == null || event.getCustomerId() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_WORKFLOW_EVENT_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, event.getWorkflowId());
            statement.setInt(2, event.getCustomerId());
            statement.setString(3, event.getEventTypeCode());
            statement.setString(4, event.getEventCategory());
            statement.setString(5, event.getFromStateCode());
            statement.setString(6, event.getToStateCode());
            statement.setString(7, truncate(safeText(event.getDescription(), ""), 1000));
            statement.setString(8, safeText(event.getPayloadJson(), "{}"));

            if (event.getCreatedByUserId() == null) {
                statement.setNull(9, Types.INTEGER);
            } else {
                statement.setInt(9, event.getCreatedByUserId());
            }

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer workflowEventId = generatedKeys.getInt(1);
                    event.setWorkflowEventId(workflowEventId);
                    return workflowEventId;
                }
            }
        } catch (SQLException e) {
            log.error(
                    "Error creating customer workflow event. workflowId={}, eventType={}",
                    event.getWorkflowId(),
                    event.getEventTypeCode(),
                    e
            );
        }

        return null;
    }

    private CustomerWorkflow mapWorkflow(ResultSet resultSet) throws SQLException {
        CustomerWorkflow workflow = new CustomerWorkflow();

        int workflowId = resultSet.getInt("WorkflowId");
        workflow.setWorkflowId(resultSet.wasNull() ? null : workflowId);

        int customerId = resultSet.getInt("CustomerId");
        workflow.setCustomerId(resultSet.wasNull() ? null : customerId);

        workflow.setWorkflowType(resultSet.getString("WorkflowType"));
        workflow.setWorkflowStatusCode(resultSet.getString("WorkflowStatus"));
        workflow.setCurrentStateCode(resultSet.getString("CurrentState"));

        int subscriptionId = resultSet.getInt("SubscriptionId");
        workflow.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        int paymentId = resultSet.getInt("PaymentId");
        workflow.setPaymentId(resultSet.wasNull() ? null : paymentId);

        workflow.setNextActionAt(resultSet.getTimestamp("NextActionAt"));

        int retryCount = resultSet.getInt("RetryCount");
        workflow.setRetryCount(resultSet.wasNull() ? null : retryCount);

        workflow.setLastEventType(resultSet.getString("LastEventType"));
        workflow.setLastEventAt(resultSet.getTimestamp("LastEventAt"));
        workflow.setLastError(resultSet.getString("LastError"));

        workflow.setLockedAt(resultSet.getTimestamp("LockedAt"));
        workflow.setLockedBy(resultSet.getString("LockedBy"));

        workflow.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        workflow.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return workflow;
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

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}