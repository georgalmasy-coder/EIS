package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;

import java.sql.Timestamp;

public class CustomerWorkflow {

    private Integer workflowId;
    private Integer customerId;

    private String workflowType;
    private CustomerWorkflowStatus workflowStatus;
    private CustomerWorkflowState currentState;

    private Integer subscriptionId;
    private Integer paymentId;

    private Timestamp nextActionAt;

    private Integer retryCount;

    private String lastEventType;
    private Timestamp lastEventAt;

    private String lastError;

    private Timestamp lockedAt;
    private String lockedBy;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CustomerWorkflow() {
        workflowId = null;
        customerId = null;
        workflowType = "CUSTOMER_ONBOARDING";
        workflowStatus = CustomerWorkflowStatus.ACTIVE;
        currentState = CustomerWorkflowState.CREATED;
        subscriptionId = null;
        paymentId = null;
        nextActionAt = null;
        retryCount = 0;
        lastEventType = "";
        lastEventAt = null;
        lastError = "";
        lockedAt = null;
        lockedBy = "";
        createdAt = null;
        updatedAt = null;
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

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = safeText(workflowType);

        if (this.workflowType.isEmpty()) {
            this.workflowType = "CUSTOMER_ONBOARDING";
        }
    }

    public CustomerWorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(CustomerWorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus == null ? CustomerWorkflowStatus.ACTIVE : workflowStatus;
    }

    public String getWorkflowStatusCode() {
        return workflowStatus == null ? "" : workflowStatus.getCode();
    }

    public void setWorkflowStatusCode(String workflowStatusCode) {
        this.workflowStatus = CustomerWorkflowStatus.fromCodeOrDefault(
                workflowStatusCode,
                CustomerWorkflowStatus.ACTIVE
        );
    }

    public CustomerWorkflowState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CustomerWorkflowState currentState) {
        this.currentState = currentState == null ? CustomerWorkflowState.CREATED : currentState;
    }

    public String getCurrentStateCode() {
        return currentState == null ? "" : currentState.getCode();
    }

    public void setCurrentStateCode(String currentStateCode) {
        CustomerWorkflowState parsedState = CustomerWorkflowState.fromCode(currentStateCode);
        this.currentState = parsedState == null ? CustomerWorkflowState.CREATED : parsedState;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Timestamp getNextActionAt() {
        return nextActionAt;
    }

    public void setNextActionAt(Timestamp nextActionAt) {
        this.nextActionAt = nextActionAt;
    }

    public Integer getRetryCount() {
        return retryCount == null ? 0 : retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount == null || retryCount < 0 ? 0 : retryCount;
    }

    public String getLastEventType() {
        return lastEventType;
    }

    public void setLastEventType(String lastEventType) {
        this.lastEventType = safeText(lastEventType);
    }

    public Timestamp getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Timestamp lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = safeText(lastError);
    }

    public Timestamp getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Timestamp lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = safeText(lockedBy);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isNew() {
        return workflowId == null;
    }

    public boolean isActive() {
        return workflowStatus != null && workflowStatus.isActiveStatus();
    }

    public boolean canBeProcessedAutomatically() {
        return workflowStatus != null && workflowStatus.canBeProcessedAutomatically();
    }

    public boolean isLocked() {
        return lockedAt != null && lockedBy != null && !lockedBy.trim().isEmpty();
    }

    public boolean isDue(Timestamp now) {
        if (!canBeProcessedAutomatically()) {
            return false;
        }

        if (nextActionAt == null) {
            return true;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !nextActionAt.after(safeNow);
    }

    public void clearLock() {
        lockedAt = null;
        lockedBy = "";
    }

    public void markLocked(String workerId) {
        lockedAt = new Timestamp(System.currentTimeMillis());
        lockedBy = safeText(workerId);
    }

    public void incrementRetryCount() {
        setRetryCount(getRetryCount() + 1);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerWorkflow [workflowId=" + workflowId
                + ", customerId=" + customerId
                + ", workflowType=" + workflowType
                + ", workflowStatus=" + getWorkflowStatusCode()
                + ", currentState=" + getCurrentStateCode()
                + ", subscriptionId=" + subscriptionId
                + ", paymentId=" + paymentId
                + ", nextActionAt=" + nextActionAt
                + ", retryCount=" + retryCount
                + ", lastEventType=" + lastEventType
                + ", lastEventAt=" + lastEventAt
                + ", lockedAt=" + lockedAt
                + ", lockedBy=" + lockedBy
                + "]";
    }
}