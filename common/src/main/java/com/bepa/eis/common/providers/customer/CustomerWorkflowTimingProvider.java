package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.GlobalConfiguration;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CustomerWorkflowTimingProvider {

    public CustomerWorkflowTimingProvider() {
    }

    public Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    public int getTrialDays() {
        return GlobalConfiguration.getCustomerWorkflowTrialDays();
    }

    public int getTrialReminderDaysBeforeExpiry() {
        return GlobalConfiguration.getCustomerWorkflowTrialReminderDaysBeforeExpiry();
    }

    public int getPaymentGracePeriodDays() {
        return GlobalConfiguration.getCustomerWorkflowPaymentGracePeriodDays();
    }

    public int getConfirmationTokenValidDays() {
        return GlobalConfiguration.getCustomerWorkflowConfirmationTokenValidDays();
    }

    public int getPaymentTokenValidDays() {
        return GlobalConfiguration.getCustomerWorkflowPaymentTokenValidDays();
    }

    public int getReactivationTokenValidDays() {
        return GlobalConfiguration.getCustomerWorkflowReactivationTokenValidDays();
    }

    public int getSubscriptionRenewalReminderDaysBeforeExpiry() {
        return GlobalConfiguration.getCustomerWorkflowSubscriptionRenewalReminderDaysBeforeExpiry();
    }

    public Timestamp trialStartAt() {
        return now();
    }

    public Timestamp trialEndAt() {
        return plusDays(now(), getTrialDays());
    }

    public Timestamp trialEndAt(Timestamp trialStartAt) {
        return plusDays(
                safeTimestamp(trialStartAt),
                getTrialDays()
        );
    }

    public Timestamp trialReminderAt(Timestamp trialEndAt) {
        return minusDays(
                safeTimestamp(trialEndAt),
                getTrialReminderDaysBeforeExpiry()
        );
    }

    public Timestamp confirmationTokenExpiresAt() {
        return plusDays(now(), getConfirmationTokenValidDays());
    }

    public Timestamp paymentTokenExpiresAt() {
        return plusDays(now(), getPaymentTokenValidDays());
    }

    public Timestamp reactivationTokenExpiresAt() {
        return plusDays(now(), getReactivationTokenValidDays());
    }

    public Timestamp paymentGracePeriodEndsAt(Timestamp paymentDueAt) {
        return plusDays(
                safeTimestamp(paymentDueAt),
                getPaymentGracePeriodDays()
        );
    }

    public Timestamp subscriptionRenewalReminderAt(Timestamp subscriptionEndAt) {
        return minusDays(
                safeTimestamp(subscriptionEndAt),
                getSubscriptionRenewalReminderDaysBeforeExpiry()
        );
    }

    public Timestamp plusDays(Timestamp timestamp, int days) {
        Instant instant = toInstant(timestamp);
        return Timestamp.from(instant.plus(days, ChronoUnit.DAYS));
    }

    public Timestamp minusDays(Timestamp timestamp, int days) {
        Instant instant = toInstant(timestamp);
        return Timestamp.from(instant.minus(days, ChronoUnit.DAYS));
    }

    public boolean isDue(Timestamp timestamp) {
        return isDue(timestamp, now());
    }

    public boolean isDue(Timestamp timestamp, Timestamp now) {
        if (timestamp == null) {
            return true;
        }

        Timestamp safeNow = safeTimestamp(now);

        return !timestamp.after(safeNow);
    }

    public boolean isBeforeNow(Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }

        return timestamp.before(now());
    }

    public boolean isAfterNow(Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }

        return timestamp.after(now());
    }

    public long daysBetween(Timestamp from, Timestamp to) {
        Instant fromInstant = toInstant(safeTimestamp(from));
        Instant toInstant = toInstant(safeTimestamp(to));

        return ChronoUnit.DAYS.between(fromInstant, toInstant);
    }

    public long hoursBetween(Timestamp from, Timestamp to) {
        Instant fromInstant = toInstant(safeTimestamp(from));
        Instant toInstant = toInstant(safeTimestamp(to));

        return ChronoUnit.HOURS.between(fromInstant, toInstant);
    }

    public Timestamp safeTimestamp(Timestamp timestamp) {
        return timestamp == null ? now() : timestamp;
    }

    private Instant toInstant(Timestamp timestamp) {
        return safeTimestamp(timestamp).toInstant();
    }
}