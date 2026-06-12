package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerTokenType;

import java.sql.Timestamp;

public class CustomerToken {

    private Integer tokenId;

    private Integer customerId;
    private Integer workflowId;
    private Integer subscriptionId;
    private Integer paymentId;

    private CustomerTokenType tokenType;
    private String tokenHash;

    private Timestamp expiresAt;
    private Timestamp usedAt;

    private Timestamp createdAt;
    private Integer createdByUserId;

    public CustomerToken() {
        tokenId = null;
        customerId = null;
        workflowId = null;
        subscriptionId = null;
        paymentId = null;
        tokenType = CustomerTokenType.EMAIL_CONFIRMATION;
        tokenHash = "";
        expiresAt = null;
        usedAt = null;
        createdAt = null;
        createdByUserId = null;
    }

    public Integer getTokenId() {
        return tokenId;
    }

    public void setTokenId(Integer tokenId) {
        this.tokenId = tokenId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
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

    public CustomerTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(CustomerTokenType tokenType) {
        this.tokenType = tokenType == null ? CustomerTokenType.EMAIL_CONFIRMATION : tokenType;
    }

    public String getTokenTypeCode() {
        return tokenType == null ? "" : tokenType.getCode();
    }

    public void setTokenTypeCode(String tokenTypeCode) {
        this.tokenType = CustomerTokenType.fromCodeOrDefault(
                tokenTypeCode,
                CustomerTokenType.EMAIL_CONFIRMATION
        );
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = safeText(tokenHash);
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Timestamp getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Timestamp usedAt) {
        this.usedAt = usedAt;
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
        return tokenId == null;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Timestamp now) {
        if (expiresAt == null) {
            return true;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !expiresAt.after(safeNow);
    }

    public boolean isValid(Timestamp now) {
        return !isUsed() && !isExpired(now) && tokenHash != null && !tokenHash.trim().isEmpty();
    }

    public void markUsed() {
        usedAt = new Timestamp(System.currentTimeMillis());
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerToken [tokenId=" + tokenId
                + ", customerId=" + customerId
                + ", workflowId=" + workflowId
                + ", subscriptionId=" + subscriptionId
                + ", paymentId=" + paymentId
                + ", tokenType=" + getTokenTypeCode()
                + ", expiresAt=" + expiresAt
                + ", usedAt=" + usedAt
                + ", createdAt=" + createdAt
                + ", createdByUserId=" + createdByUserId
                + "]";
    }
}