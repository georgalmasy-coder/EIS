package com.bepa.eis.common.dto.customer;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SubscriptionPlanBillingPeriod {

    private Integer subscriptionPlanBillingPeriodId;
    private Integer subscriptionPlanId;

    private String billingPeriodCode;
    private String billingPeriodName;
    private String description;

    private Integer billingPeriodMonths;
    private BigDecimal priceAmount;
    private String currency;

    private Boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public SubscriptionPlanBillingPeriod() {
        subscriptionPlanBillingPeriodId = null;
        subscriptionPlanId = null;
        billingPeriodCode = "";
        billingPeriodName = "";
        description = "";
        billingPeriodMonths = 1;
        priceAmount = BigDecimal.ZERO;
        currency = "EUR";
        active = true;
        createdAt = null;
        updatedAt = null;
    }

    public Integer getSubscriptionPlanBillingPeriodId() {
        return subscriptionPlanBillingPeriodId;
    }

    public void setSubscriptionPlanBillingPeriodId(Integer subscriptionPlanBillingPeriodId) {
        this.subscriptionPlanBillingPeriodId = subscriptionPlanBillingPeriodId;
    }

    public Integer getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(Integer subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public String getBillingPeriodCode() {
        return billingPeriodCode;
    }

    public void setBillingPeriodCode(String billingPeriodCode) {
        this.billingPeriodCode = safeText(billingPeriodCode).toUpperCase();
    }

    public String getBillingPeriodName() {
        return billingPeriodName;
    }

    public void setBillingPeriodName(String billingPeriodName) {
        this.billingPeriodName = safeText(billingPeriodName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = safeText(description);
    }

    public Integer getBillingPeriodMonths() {
        return billingPeriodMonths == null || billingPeriodMonths < 1 ? 1 : billingPeriodMonths;
    }

    public void setBillingPeriodMonths(Integer billingPeriodMonths) {
        this.billingPeriodMonths = billingPeriodMonths == null || billingPeriodMonths < 1
                ? 1
                : billingPeriodMonths;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount == null ? BigDecimal.ZERO : priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : priceAmount;
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

    public Boolean getActive() {
        return active != null && active;
    }

    public void setActive(Boolean active) {
        this.active = active == null || active;
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
        return subscriptionPlanBillingPeriodId == null;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
