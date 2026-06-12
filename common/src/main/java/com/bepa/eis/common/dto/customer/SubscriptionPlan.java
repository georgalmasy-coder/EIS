package com.bepa.eis.common.dto.customer;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SubscriptionPlan {

    private Integer subscriptionPlanId;

    private String moduleCode;
    private String moduleName;
    private String planName;

    private String description;

    private BigDecimal priceAmount;
    private String currency;

    private Integer billingPeriodMonths;
    private Integer trialDays;

    private Boolean active;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public SubscriptionPlan() {
        subscriptionPlanId = null;

        moduleCode = "";
        moduleName = "";
        planName = "";

        description = "";

        priceAmount = BigDecimal.ZERO;
        currency = "EUR";

        billingPeriodMonths = 1;
        trialDays = 14;

        active = true;

        createdAt = null;
        updatedAt = null;
    }

    public Integer getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(Integer subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = safeText(moduleCode).toUpperCase();
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = safeText(moduleName);
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = safeText(planName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = safeText(description);
    }

    public BigDecimal getPriceAmount() {
        return priceAmount == null ? BigDecimal.ZERO : priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.priceAmount = BigDecimal.ZERO;
            return;
        }

        this.priceAmount = priceAmount;
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

    public Integer getBillingPeriodMonths() {
        return billingPeriodMonths == null || billingPeriodMonths < 1 ? 1 : billingPeriodMonths;
    }

    public void setBillingPeriodMonths(Integer billingPeriodMonths) {
        if (billingPeriodMonths == null || billingPeriodMonths < 1) {
            this.billingPeriodMonths = 1;
            return;
        }

        this.billingPeriodMonths = billingPeriodMonths;
    }

    public Integer getTrialDays() {
        return trialDays == null || trialDays < 0 ? 0 : trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        if (trialDays == null || trialDays < 0) {
            this.trialDays = 0;
            return;
        }

        this.trialDays = trialDays;
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
        return subscriptionPlanId == null;
    }

    public boolean isActive() {
        return active != null && active;
    }

    public boolean hasModuleCode() {
        return moduleCode != null && !moduleCode.trim().isEmpty();
    }

    public boolean hasPlanName() {
        return planName != null && !planName.trim().isEmpty();
    }

    public String getDisplayName() {
        if (moduleName != null && !moduleName.trim().isEmpty()
                && planName != null && !planName.trim().isEmpty()) {
            return moduleName.trim() + " - " + planName.trim();
        }

        if (planName != null && !planName.trim().isEmpty()) {
            return planName.trim();
        }

        if (moduleName != null && !moduleName.trim().isEmpty()) {
            return moduleName.trim();
        }

        return "";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "SubscriptionPlan [subscriptionPlanId=" + subscriptionPlanId
                + ", moduleCode=" + moduleCode
                + ", moduleName=" + moduleName
                + ", planName=" + planName
                + ", priceAmount=" + priceAmount
                + ", currency=" + currency
                + ", billingPeriodMonths=" + billingPeriodMonths
                + ", trialDays=" + trialDays
                + ", active=" + active
                + "]";
    }
}