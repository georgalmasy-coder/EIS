package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerStatus;

import java.sql.Timestamp;

public class CustomerWorkflowSummary {

    private Integer workflowId;
    private Integer customerId;
    private String customerName;
    private String contactEmail;

    private Integer customerStatusId;
    private String customerStatusCode;
    private String customerStatusLabel;

    private String workflowType;
    private String workflowStatus;
    private String currentState;

    private Integer subscriptionId;
    private String subscriptionStatus;
    private Timestamp trialEndAt;
    private Timestamp periodEndAt;

    private Integer paymentId;
    private String paymentStatus;
    private Timestamp paymentDueAt;
    private Timestamp paymentGracePeriodEndsAt;

    private Timestamp nextActionAt;
    private Integer retryCount;
    private String lastEventType;
    private Timestamp lastEventAt;
    private String lastError;

    private Timestamp lockedAt;
    private String lockedBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CustomerWorkflowSummary() {
        workflowId = null;
        customerId = null;
        customerName = "";
        contactEmail = "";

        customerStatusId = null;
        customerStatusCode = "";
        customerStatusLabel = "";

        workflowType = "";
        workflowStatus = "";
        currentState = "";

        subscriptionId = null;
        subscriptionStatus = "";
        trialEndAt = null;
        periodEndAt = null;

        paymentId = null;
        paymentStatus = "";
        paymentDueAt = null;
        paymentGracePeriodEndsAt = null;

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = safeText(customerName);
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = safeText(contactEmail);
    }

    public Integer getCustomerStatusId() {
        return customerStatusId;
    }

    public void setCustomerStatusId(Integer customerStatusId) {
        this.customerStatusId = customerStatusId;

        CustomerStatus customerStatus = CustomerStatus.fromId(customerStatusId);

        if (customerStatus == null) {
            customerStatusCode = "";
            customerStatusLabel = "";
            return;
        }

        customerStatusCode = customerStatus.getCode();
        customerStatusLabel = customerStatus.getLabel();
    }

    public String getCustomerStatusCode() {
        return customerStatusCode;
    }

    public void setCustomerStatusCode(String customerStatusCode) {
        this.customerStatusCode = safeText(customerStatusCode);

        CustomerStatus customerStatus = CustomerStatus.fromCode(this.customerStatusCode);

        if (customerStatus == null) {
            customerStatusId = null;
            customerStatusLabel = "";
            return;
        }

        customerStatusId = customerStatus.getId();
        customerStatusLabel = customerStatus.getLabel();
    }

    public String getCustomerStatusLabel() {
        return customerStatusLabel;
    }

    public void setCustomerStatusLabel(String customerStatusLabel) {
        this.customerStatusLabel = safeText(customerStatusLabel);
    }

    public CustomerStatus getCustomerStatus() {
        return CustomerStatus.fromId(customerStatusId);
    }

    public void setCustomerStatus(CustomerStatus customerStatus) {
        if (customerStatus == null) {
            customerStatusId = null;
            customerStatusCode = "";
            customerStatusLabel = "";
            return;
        }

        customerStatusId = customerStatus.getId();
        customerStatusCode = customerStatus.getCode();
        customerStatusLabel = customerStatus.getLabel();
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = safeText(workflowType);
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = safeText(workflowStatus);
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = safeText(currentState);
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = safeText(subscriptionStatus);
    }

    public Timestamp getTrialEndAt() {
        return trialEndAt;
    }

    public void setTrialEndAt(Timestamp trialEndAt) {
        this.trialEndAt = trialEndAt;
    }

    public Timestamp getPeriodEndAt() {
        return periodEndAt;
    }

    public void setPeriodEndAt(Timestamp periodEndAt) {
        this.periodEndAt = periodEndAt;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = safeText(paymentStatus);
    }

    public Timestamp getPaymentDueAt() {
        return paymentDueAt;
    }

    public void setPaymentDueAt(Timestamp paymentDueAt) {
        this.paymentDueAt = paymentDueAt;
    }

    public Timestamp getPaymentGracePeriodEndsAt() {
        return paymentGracePeriodEndsAt;
    }

    public void setPaymentGracePeriodEndsAt(Timestamp paymentGracePeriodEndsAt) {
        this.paymentGracePeriodEndsAt = paymentGracePeriodEndsAt;
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

    public boolean hasError() {
        return lastError != null && !lastError.trim().isEmpty();
    }

    public boolean isLocked() {
        return lockedAt != null && lockedBy != null && !lockedBy.trim().isEmpty();
    }

    public boolean requiresManualAttention() {
        return "WAITING_FOR_MANUAL_ATTENTION".equalsIgnoreCase(workflowStatus)
                || "WAITING_FOR_MANUAL_ATTENTION".equalsIgnoreCase(currentState);
    }

    public boolean isSuspended() {
        CustomerStatus customerStatus = getCustomerStatus();

        if (customerStatus != null && customerStatus.isSuspended()) {
            return true;
        }

        return "SUSPENDED".equalsIgnoreCase(workflowStatus)
                || "SUSPENDED".equalsIgnoreCase(currentState);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(workflowStatus);
    }

    public boolean isCustomerLoginAllowedByDefault() {
        CustomerStatus customerStatus = getCustomerStatus();

        return customerStatus != null && customerStatus.isLoginAllowedByDefault();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerWorkflowSummary [workflowId=" + workflowId
                + ", customerId=" + customerId
                + ", customerName=" + customerName
                + ", customerStatus=" + customerStatusCode
                + ", workflowStatus=" + workflowStatus
                + ", currentState=" + currentState
                + ", subscriptionStatus=" + subscriptionStatus
                + ", paymentStatus=" + paymentStatus
                + ", nextActionAt=" + nextActionAt
                + ", lastEventType=" + lastEventType
                + "]";
    }
}