package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
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

public class CustomerWorkflowEventProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowEventProvider.class);

    private static final String SELECT_EVENTS_BY_WORKFLOW_ID_SQL =
            "SELECT TOP (?) " +
                    "WorkflowEventId, " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "EventType, " +
                    "EventCategory, " +
                    "FromState, " +
                    "ToState, " +
                    "Description, " +
                    "PayloadJson, " +
                    "CreatedAt, " +
                    "CreatedByUserId " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW_EVENT] " +
                    "WHERE WorkflowId = ? " +
                    "ORDER BY CreatedAt DESC, WorkflowEventId DESC ";

    private static final String SELECT_EVENTS_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (?) " +
                    "WorkflowEventId, " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "EventType, " +
                    "EventCategory, " +
                    "FromState, " +
                    "ToState, " +
                    "Description, " +
                    "PayloadJson, " +
                    "CreatedAt, " +
                    "CreatedByUserId " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW_EVENT] " +
                    "WHERE CustomerId = ? " +
                    "ORDER BY CreatedAt DESC, WorkflowEventId DESC ";

    private static final String SELECT_EVENT_BY_ID_SQL =
            "SELECT " +
                    "WorkflowEventId, " +
                    "WorkflowId, " +
                    "CustomerId, " +
                    "EventType, " +
                    "EventCategory, " +
                    "FromState, " +
                    "ToState, " +
                    "Description, " +
                    "PayloadJson, " +
                    "CreatedAt, " +
                    "CreatedByUserId " +
                    "FROM [dbo].[CUSTOMER_WORKFLOW_EVENT] " +
                    "WHERE WorkflowEventId = ? ";

    public CustomerWorkflowEventProvider(WebSession webSession) {
        super(webSession);
    }

    public CustomerWorkflowEvent getWorkflowEventById(Integer workflowEventId) {
        if (workflowEventId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENT_BY_ID_SQL)) {

            statement.setInt(1, workflowEventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapWorkflowEvent(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow event. workflowEventId={}", workflowEventId, e);
        }

        return null;
    }

    public List<CustomerWorkflowEvent> getWorkflowEventsByWorkflowId(Integer workflowId) {
        return getWorkflowEventsByWorkflowId(
                workflowId,
                100
        );
    }

    public List<CustomerWorkflowEvent> getWorkflowEventsByWorkflowId(
            Integer workflowId,
            int maxRows
    ) {
        if (workflowId == null) {
            return Collections.emptyList();
        }

        List<CustomerWorkflowEvent> events = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 1000));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENTS_BY_WORKFLOW_ID_SQL)) {

            statement.setInt(1, safeMaxRows);
            statement.setInt(2, workflowId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapWorkflowEvent(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow events. workflowId={}", workflowId, e);
        }

        return events;
    }

    public List<CustomerWorkflowEvent> getWorkflowEventsByCustomerId(Integer customerId) {
        return getWorkflowEventsByCustomerId(
                customerId,
                100
        );
    }

    public List<CustomerWorkflowEvent> getWorkflowEventsByCustomerId(
            Integer customerId,
            int maxRows
    ) {
        if (customerId == null) {
            return Collections.emptyList();
        }

        List<CustomerWorkflowEvent> events = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 1000));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENTS_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, safeMaxRows);
            statement.setInt(2, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapWorkflowEvent(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer workflow events. customerId={}", customerId, e);
        }

        return events;
    }

    private CustomerWorkflowEvent mapWorkflowEvent(ResultSet resultSet) throws SQLException {
        CustomerWorkflowEvent event = new CustomerWorkflowEvent();

        int workflowEventId = resultSet.getInt("WorkflowEventId");
        event.setWorkflowEventId(resultSet.wasNull() ? null : workflowEventId);

        int workflowId = resultSet.getInt("WorkflowId");
        event.setWorkflowId(resultSet.wasNull() ? null : workflowId);

        int customerId = resultSet.getInt("CustomerId");
        event.setCustomerId(resultSet.wasNull() ? null : customerId);

        event.setEventTypeCode(resultSet.getString("EventType"));
        event.setEventCategory(resultSet.getString("EventCategory"));
        event.setFromStateCode(resultSet.getString("FromState"));
        event.setToStateCode(resultSet.getString("ToState"));
        event.setDescription(resultSet.getString("Description"));
        event.setPayloadJson(resultSet.getString("PayloadJson"));
        event.setCreatedAt(resultSet.getTimestamp("CreatedAt"));

        int createdByUserId = resultSet.getInt("CreatedByUserId");
        event.setCreatedByUserId(resultSet.wasNull() ? null : createdByUserId);

        return event;
    }
}