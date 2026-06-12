package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;

import java.sql.Timestamp;

public class CustomerSubscription {

    private Integer subscriptionId;
    private Integer customerId;

    private CustomerSubscriptionStatus subscriptionStatus;

    private Integer subscriptionPlanId;
    private String subscriptionPlanName;

    private Timestamp trialStartAt;
    private Timestamp trialEndAt;
    private Timestamp trialReminderSentAt;

    private Timestamp periodStartAt;
    private Timestamp periodEndAt;
    private Timestamp renewalReminderSentAt;

    private Timestamp continuationConfirmedAt;
    private Timestamp renewalConfirmedAt;

    private Timestamp gracePeriodEndsAt;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CustomerSubscription() {
        subscriptionId = null;
        customerId = null;
        subscriptionStatus = CustomerSubscriptionStatus.NONE;
        subscriptionPlanId = null;
        subscriptionPlanName = "";
        trialStartAt = null;
        trialEndAt = null;
        trialReminderSentAt = null;
        periodStartAt = null;
        periodEndAt = null;
        renewalReminderSentAt = null;
        continuationConfirmedAt = null;
        renewalConfirmedAt = null;
        gracePeriodEndsAt = null;
        createdAt = null;
        updatedAt = null;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public CustomerSubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(CustomerSubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus == null
                ? CustomerSubscriptionStatus.NONE
                : subscriptionStatus;
    }

    public String getSubscriptionStatusCode() {
        return subscriptionStatus == null ? "" : subscriptionStatus.getCode();
    }

    public void setSubscriptionStatusCode(String subscriptionStatusCode) {
        CustomerSubscriptionStatus parsedStatus = CustomerSubscriptionStatus.fromCode(subscriptionStatusCode);
        this.subscriptionStatus = parsedStatus == null ? CustomerSubscriptionStatus.NONE : parsedStatus;
    }

    public Integer getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(Integer subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public String getSubscriptionPlanName() {
        return subscriptionPlanName;
    }

    public void setSubscriptionPlanName(String subscriptionPlanName) {
        this.subscriptionPlanName = safeText(subscriptionPlanName);
    }

    public Timestamp getTrialStartAt() {
        return trialStartAt;
    }

    public void setTrialStartAt(Timestamp trialStartAt) {
        this.trialStartAt = trialStartAt;
    }

    public Timestamp getTrialEndAt() {
        return trialEndAt;
    }

    public void setTrialEndAt(Timestamp trialEndAt) {
        this.trialEndAt = trialEndAt;
    }

    public Timestamp getTrialReminderSentAt() {
        return trialReminderSentAt;
    }

    public void setTrialReminderSentAt(Timestamp trialReminderSentAt) {
        this.trialReminderSentAt = trialReminderSentAt;
    }

    public Timestamp getPeriodStartAt() {
        return periodStartAt;
    }

    public void setPeriodStartAt(Timestamp periodStartAt) {
        this.periodStartAt = periodStartAt;
    }

    public Timestamp getPeriodEndAt() {
        return periodEndAt;
    }

    public void setPeriodEndAt(Timestamp periodEndAt) {
        this.periodEndAt = periodEndAt;
    }

    public Timestamp getRenewalReminderSentAt() {
        return renewalReminderSentAt;
    }

    public void setRenewalReminderSentAt(Timestamp renewalReminderSentAt) {
        this.renewalReminderSentAt = renewalReminderSentAt;
    }

    public Timestamp getContinuationConfirmedAt() {
        return continuationConfirmedAt;
    }

    public void setContinuationConfirmedAt(Timestamp continuationConfirmedAt) {
        this.continuationConfirmedAt = continuationConfirmedAt;
    }

    public Timestamp getRenewalConfirmedAt() {
        return renewalConfirmedAt;
    }

    public void setRenewalConfirmedAt(Timestamp renewalConfirmedAt) {
        this.renewalConfirmedAt = renewalConfirmedAt;
    }

    public Timestamp getGracePeriodEndsAt() {
        return gracePeriodEndsAt;
    }

    public void setGracePeriodEndsAt(Timestamp gracePeriodEndsAt) {
        this.gracePeriodEndsAt = gracePeriodEndsAt;
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
        return subscriptionId == null;
    }

    public boolean isTrial() {
        return subscriptionStatus != null && subscriptionStatus.isTrialStatus();
    }

    public boolean isActive() {
        return subscriptionStatus != null && subscriptionStatus.isActiveStatus();
    }

    public boolean isPaymentRequired() {
        return subscriptionStatus != null && subscriptionStatus.isPaymentRequiredStatus();
    }

    public boolean isSuspended() {
        return subscriptionStatus != null && subscriptionStatus.isSuspendedStatus();
    }

    public boolean isLoginAllowedByDefault() {
        return subscriptionStatus != null && subscriptionStatus.isLoginAllowedByDefault();
    }

    public boolean hasTrialPeriod() {
        return trialStartAt != null || trialEndAt != null;
    }

    public boolean hasSubscriptionPeriod() {
        return periodStartAt != null || periodEndAt != null;
    }

    public boolean isTrialExpired(Timestamp now) {
        if (trialEndAt == null) {
            return false;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !trialEndAt.after(safeNow);
    }

    public boolean isSubscriptionExpired(Timestamp now) {
        if (periodEndAt == null) {
            return false;
        }

        Timestamp safeNow = now == null ? new Timestamp(System.currentTimeMillis()) : now;

        return !periodEndAt.after(safeNow);
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
        return "CustomerSubscription [subscriptionId=" + subscriptionId
                + ", customerId=" + customerId
                + ", subscriptionStatus=" + getSubscriptionStatusCode()
                + ", subscriptionPlanId=" + subscriptionPlanId
                + ", subscriptionPlanName=" + subscriptionPlanName
                + ", trialStartAt=" + trialStartAt
                + ", trialEndAt=" + trialEndAt
                + ", periodStartAt=" + periodStartAt
                + ", periodEndAt=" + periodEndAt
                + ", gracePeriodEndsAt=" + gracePeriodEndsAt
                + "]";
    }
}