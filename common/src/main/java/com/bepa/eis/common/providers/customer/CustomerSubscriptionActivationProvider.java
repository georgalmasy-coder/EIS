package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CustomerSubscriptionActivationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerSubscriptionActivationProvider.class);

    private final CustomerSubscriptionProvider subscriptionProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;
    private final CustomerWorkflowTimingProvider timingProvider;
    private final CustomerModuleProvider customerModuleProvider;

    public CustomerSubscriptionActivationProvider() {
        this(null);
    }

    public CustomerSubscriptionActivationProvider(WebSession webSession) {
        this(
                new CustomerSubscriptionProvider(webSession),
                new SubscriptionPlanProvider(webSession),
                new CustomerWorkflowTimingProvider(),
                new CustomerModuleProvider(webSession)
        );
    }

    public CustomerSubscriptionActivationProvider(
            CustomerSubscriptionProvider subscriptionProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            CustomerWorkflowTimingProvider timingProvider
    ) {
        this(
                subscriptionProvider,
                subscriptionPlanProvider,
                timingProvider,
                new CustomerModuleProvider(null)
        );
    }

    public CustomerSubscriptionActivationProvider(
            CustomerSubscriptionProvider subscriptionProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            CustomerWorkflowTimingProvider timingProvider,
            CustomerModuleProvider customerModuleProvider
    ) {
        this.subscriptionProvider = subscriptionProvider == null
                ? new CustomerSubscriptionProvider(null)
                : subscriptionProvider;

        this.subscriptionPlanProvider = subscriptionPlanProvider == null
                ? new SubscriptionPlanProvider(null)
                : subscriptionPlanProvider;

        this.timingProvider = timingProvider == null
                ? new CustomerWorkflowTimingProvider()
                : timingProvider;

        this.customerModuleProvider = customerModuleProvider == null
                ? new CustomerModuleProvider(null)
                : customerModuleProvider;
    }

    public boolean activateSubscription(Integer subscriptionId) {
        if (subscriptionId == null) {
            return false;
        }

        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(subscriptionId);

        if (subscription == null) {
            log.warn("Subscription could not be activated because it was not found. subscriptionId={}", subscriptionId);
            return false;
        }

        return activateSubscription(subscription);
    }

    public boolean activateSubscription(CustomerSubscription subscription) {
        if (subscription == null || subscription.getSubscriptionId() == null) {
            return false;
        }

        Timestamp now = timingProvider.now();
        Timestamp periodStartAt = now;
        Timestamp periodEndAt = calculatePeriodEndAt(
                now,
                subscription.getSubscriptionPlanId()
        );

        subscription.setSubscriptionStatus(CustomerSubscriptionStatus.ACTIVE);
        subscription.setPeriodStartAt(periodStartAt);
        subscription.setPeriodEndAt(periodEndAt);
        subscription.setGracePeriodEndsAt(null);

        boolean updated = subscriptionProvider.updateSubscription(subscription);

        if (updated) {
            updateCustomerModuleStatus(
                    subscription,
                    CustomerModuleStatus.ACTIVE
            );

            log.info(
                    "Subscription activated. subscriptionId={}, customerId={}, periodStartAt={}, periodEndAt={}",
                    subscription.getSubscriptionId(),
                    subscription.getCustomerId(),
                    periodStartAt,
                    periodEndAt
            );
        } else {
            log.warn(
                    "Subscription could not be activated. subscriptionId={}, customerId={}",
                    subscription.getSubscriptionId(),
                    subscription.getCustomerId()
            );
        }

        return updated;
    }

    public boolean markSubscriptionPaymentPending(Integer subscriptionId) {
        if (subscriptionId == null) {
            return false;
        }

        return subscriptionProvider.updateSubscriptionStatus(
                subscriptionId,
                CustomerSubscriptionStatus.PAYMENT_PENDING
        );
    }

    public boolean markSubscriptionPaymentOverdue(Integer subscriptionId) {
        if (subscriptionId == null) {
            return false;
        }

        return subscriptionProvider.updateSubscriptionStatus(
                subscriptionId,
                CustomerSubscriptionStatus.PAYMENT_OVERDUE
        );
    }

    public boolean suspendSubscription(Integer subscriptionId) {
        if (subscriptionId == null) {
            return false;
        }

        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(subscriptionId);

        if (subscription == null) {
            return false;
        }

        boolean updated = subscriptionProvider.updateSubscriptionStatus(
                subscriptionId,
                CustomerSubscriptionStatus.SUSPENDED
        );

        if (updated) {
            updateCustomerModuleStatus(
                    subscription,
                    CustomerModuleStatus.SUSPENDED
            );
        }

        return updated;
    }

    public boolean cancelSubscription(Integer subscriptionId) {
        if (subscriptionId == null) {
            return false;
        }

        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(subscriptionId);

        if (subscription == null) {
            return false;
        }

        boolean updated = subscriptionProvider.updateSubscriptionStatus(
                subscriptionId,
                CustomerSubscriptionStatus.CANCELLED
        );

        if (updated) {
            updateCustomerModuleStatus(
                    subscription,
                    CustomerModuleStatus.CANCELLED
            );
        }

        return updated;
    }

    private void updateCustomerModuleStatus(
            CustomerSubscription subscription,
            CustomerModuleStatus moduleStatus
    ) {
        if (subscription == null
                || subscription.getCustomerId() == null
                || subscription.getSubscriptionPlanId() == null
                || moduleStatus == null) {
            return;
        }

        SubscriptionPlan plan = subscriptionPlanProvider.getPlanById(subscription.getSubscriptionPlanId());

        if (plan == null || plan.getModuleCode() == null || plan.getModuleCode().trim().isEmpty()) {
            return;
        }

        CustomerModule customerModule = customerModuleProvider.getLatestCustomerModule(
                subscription.getCustomerId(),
                plan.getModuleCode()
        );

        if (customerModule == null || customerModule.getCustomerModuleId() == null) {
            return;
        }

        boolean updated = customerModuleProvider.updateCustomerModuleStatus(
                customerModule.getCustomerModuleId(),
                moduleStatus
        );

        if (!updated) {
            log.warn(
                    "Customer module status could not be updated. customerId={}, moduleCode={}, status={}",
                    subscription.getCustomerId(),
                    plan.getModuleCode(),
                    moduleStatus.getCode()
            );
        }
    }

    private Timestamp calculatePeriodEndAt(
            Timestamp periodStartAt,
            Integer subscriptionPlanId
    ) {
        int billingPeriodMonths = resolveBillingPeriodMonths(subscriptionPlanId);

        Instant startInstant = periodStartAt == null
                ? Instant.now()
                : periodStartAt.toInstant();

        return Timestamp.from(startInstant.plus(estimateDaysFromMonths(billingPeriodMonths), ChronoUnit.DAYS));
    }

    private int resolveBillingPeriodMonths(Integer subscriptionPlanId) {
        if (subscriptionPlanId == null) {
            return 1;
        }

        SubscriptionPlan plan = subscriptionPlanProvider.getPlanById(subscriptionPlanId);

        if (plan == null) {
            return 1;
        }

        return plan.getBillingPeriodMonths();
    }

    private int estimateDaysFromMonths(int months) {
        int safeMonths = Math.max(1, months);

        /*
         * This uses a simple 30-day month approximation.
         * If exact calendar-month handling is required later, this method should be changed
         * to use LocalDateTime plusMonths(...) based on the desired timezone/calendar rules.
         */
        return safeMonths * 30;
    }
}