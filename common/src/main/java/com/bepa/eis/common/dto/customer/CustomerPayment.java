package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CustomerPayment {

    private Integer paymentId;

    private Integer customerId;
    private Integer subscriptionId;

    private CustomerPaymentStatus paymentStatus;

    private String paymentProvider;
    private String paymentProviderReference;

    private BigDecimal amount;
    private String currency;

    private Timestamp paymentDueAt;
    private Timestamp gracePeriodEndsAt;

    private Timestamp requestedAt;
    private Timestamp authorizedAt;
    private Timestamp capturedAt;
    private Timestamp succeededAt;
    private Timestamp failedAt;
    private Timestamp cancelledAt;

    private String failureReason;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CustomerPayment() {
        paymentId = null;
        customerId = null;
        subscriptionId = null;
        paymentStatus = CustomerPaymentStatus.NONE;
        paymentProvider = "";
        paymentProviderReference = "";
        amount = BigDecimal.ZERO;
        currency = "EUR";
        paymentDueAt = null;
        gracePeriodEndsAt = null;
        requestedAt = null;
        authorizedAt = null;
        capturedAt = null;
        succeededAt = null;
        failedAt = null;
        cancelledAt = null;
        failureReason = "";
        createdAt = null;
        updatedAt = null;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public CustomerPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(CustomerPaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus == null ? CustomerPaymentStatus.NONE : paymentStatus;
    }

    public String getPaymentStatusCode() {
        return paymentStatus == null ? "" : paymentStatus.getCode();
    }

    public void setPaymentStatusCode(String paymentStatusCode) {
        CustomerPaymentStatus parsedStatus = CustomerPaymentStatus.fromCode(paymentStatusCode);
        this.paymentStatus = parsedStatus == null ? CustomerPaymentStatus.NONE : parsedStatus;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = safeText(paymentProvider);
    }

    public String getPaymentProviderReference() {
        return paymentProviderReference;
    }

    public void setPaymentProviderReference(String paymentProviderReference) {
        this.paymentProviderReference = safeText(paymentProviderReference);
    }

    public BigDecimal getAmount() {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            this.amount = BigDecimal.ZERO;
            return;
        }

        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = safeText(currency).toUpperCase();

        if (this.currency.isEmpty()) {
            this.currency = "EUR";
        }
    }

    public Timestamp getPaymentDueAt() {
        return paymentDueAt;
    }

    public void setPaymentDueAt(Timestamp paymentDueAt) {
        this.paymentDueAt = paymentDueAt;
    }

    public Timestamp getGracePeriodEndsAt() {
        return gracePeriodEndsAt;
    }

    public void setGracePeriodEndsAt(Timestamp gracePeriodEndsAt) {
        this.gracePeriodEndsAt = gracePeriodEndsAt;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Timestamp getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Timestamp authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public Timestamp getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Timestamp capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Timestamp getSucceededAt() {
        return succeededAt;
    }

    public void setSucceededAt(Timestamp succeededAt) {
        this.succeededAt = succeededAt;
    }

    public Timestamp getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Timestamp failedAt) {
        this.failedAt = failedAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = safeText(failureReason);
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
        return paymentId == null;
    }

    public boolean isSuccessful() {
        return paymentStatus != null && paymentStatus.isSuccessfulStatus();
    }

    public boolean isPending() {
        return paymentStatus != null && paymentStatus.isPendingStatus();
    }

    public boolean isFailed() {
        return paymentStatus != null && paymentStatus.isFailedStatus();
    }

    public boolean isTerminal() {
        return paymentStatus != null && paymentStatus.isTerminalStatus();
    }

    public boolean requiresManualAttention() {
        return paymentStatus != null && paymentStatus.requiresManualAttention();
    }

    public boolean isDue(Timestamp now) {
        if (paymentDueAt == null) {
            return false;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !paymentDueAt.after(safeNow);
    }

    public boolean isGracePeriodExpired(Timestamp now) {
        if (gracePeriodEndsAt == null) {
            return false;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !gracePeriodEndsAt.after(safeNow);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerPayment [paymentId=" + paymentId
                + ", customerId=" + customerId
                + ", subscriptionId=" + subscriptionId
                + ", paymentStatus=" + getPaymentStatusCode()
                + ", paymentProvider=" + paymentProvider
                + ", paymentProviderReference=" + paymentProviderReference
                + ", amount=" + amount
                + ", currency=" + currency
                + ", paymentDueAt=" + paymentDueAt
                + ", gracePeriodEndsAt=" + gracePeriodEndsAt
                + "]";
    }
}