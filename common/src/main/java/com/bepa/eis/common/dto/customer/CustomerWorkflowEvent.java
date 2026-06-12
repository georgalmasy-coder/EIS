package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;

import java.sql.Timestamp;

public class CustomerWorkflowEvent {

    private Integer workflowEventId;

    private Integer workflowId;
    private Integer customerId;

    private CustomerWorkflowEventType eventType;
    private String eventCategory;

    private CustomerWorkflowState fromState;
    private CustomerWorkflowState toState;

    private String description;
    private String payloadJson;

    private Timestamp createdAt;
    private Integer createdByUserId;

    public CustomerWorkflowEvent() {
        workflowEventId = null;
        workflowId = null;
        customerId = null;
        eventType = null;
        eventCategory = "";
        fromState = null;
        toState = null;
        description = "";
        payloadJson = "";
        createdAt = null;
        createdByUserId = null;
    }

    public Integer getWorkflowEventId() {
        return workflowEventId;
    }

    public void setWorkflowEventId(Integer workflowEventId) {
        this.workflowEventId = workflowEventId;
    }

    public Integer getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public CustomerWorkflowEventType getEventType() {
        return eventType;
    }

    public void setEventType(CustomerWorkflowEventType eventType) {
        this.eventType = eventType;

        if (eventType != null && eventType.getCategory() != null) {
            this.eventCategory = eventType.getCategory().name();
        }
    }

    public String getEventTypeCode() {
        return eventType == null ? "" : eventType.getCode();
    }

    public void setEventTypeCode(String eventTypeCode) {
        CustomerWorkflowEventType parsedEventType = CustomerWorkflowEventType.fromCode(eventTypeCode);
        setEventType(parsedEventType);
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = safeText(eventCategory);
    }

    public CustomerWorkflowState getFromState() {
        return fromState;
    }

    public void setFromState(CustomerWorkflowState fromState) {
        this.fromState = fromState;
    }

    public String getFromStateCode() {
        return fromState == null ? "" : fromState.getCode();
    }

    public void setFromStateCode(String fromStateCode) {
        this.fromState = CustomerWorkflowState.fromCode(fromStateCode);
    }

    public CustomerWorkflowState getToState() {
        return toState;
    }

    public void setToState(CustomerWorkflowState toState) {
        this.toState = toState;
    }

    public String getToStateCode() {
        return toState == null ? "" : toState.getCode();
    }

    public void setToStateCode(String toStateCode) {
        this.toState = CustomerWorkflowState.fromCode(toStateCode);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = safeText(description);
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = safeText(payloadJson);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public boolean isNew() {
        return workflowEventId == null;
    }

    public boolean hasPayload() {
        return payloadJson != null && !payloadJson.trim().isEmpty();
    }

    public boolean isStateTransition() {
        return fromState != null || toState != null;
    }

    public static CustomerWorkflowEvent create(
            Integer workflowId,
            Integer customerId,
            CustomerWorkflowEventType eventType,
            CustomerWorkflowState fromState,
            CustomerWorkflowState toState,
            String description,
            String payloadJson,
            Integer createdByUserId
    ) {
        CustomerWorkflowEvent event = new CustomerWorkflowEvent();

        event.setWorkflowId(workflowId);
        event.setCustomerId(customerId);
        event.setEventType(eventType);
        event.setFromState(fromState);
        event.setToState(toState);
        event.setDescription(description);
        event.setPayloadJson(payloadJson);
        event.setCreatedByUserId(createdByUserId);

        return event;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerWorkflowEvent [workflowEventId=" + workflowEventId
                + ", workflowId=" + workflowId
                + ", customerId=" + customerId
                + ", eventType=" + getEventTypeCode()
                + ", eventCategory=" + eventCategory
                + ", fromState=" + getFromStateCode()
                + ", toState=" + getToStateCode()
                + ", createdAt=" + createdAt
                + ", createdByUserId=" + createdByUserId
                + "]";
    }
}