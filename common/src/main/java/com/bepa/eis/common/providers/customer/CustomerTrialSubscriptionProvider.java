package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CustomerTrialSubscriptionProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerTrialSubscriptionProvider.class);

    private final CustomerSubscriptionProvider subscriptionProvider;
    private final CustomerModuleProvider customerModuleProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;
    private final SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider;
    private final CustomerWorkflowTimingProvider timingProvider;

    public CustomerTrialSubscriptionProvider() {
        this(null);
    }

    public CustomerTrialSubscriptionProvider(WebSession webSession) {
        this(
                new CustomerSubscriptionProvider(webSession),
                new CustomerModuleProvider(webSession),
                new SubscriptionPlanProvider(webSession),
                new SubscriptionPlanBillingPeriodProvider(webSession),
                new CustomerWorkflowTimingProvider()
        );
    }

    public CustomerTrialSubscriptionProvider(
            CustomerSubscriptionProvider subscriptionProvider,
            CustomerModuleProvider customerModuleProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider,
            CustomerWorkflowTimingProvider timingProvider
    ) {
        this.subscriptionProvider = subscriptionProvider == null
                ? new CustomerSubscriptionProvider(null)
                : subscriptionProvider;

        this.customerModuleProvider = customerModuleProvider == null
                ? new CustomerModuleProvider(null)
                : customerModuleProvider;

        this.subscriptionPlanProvider = subscriptionPlanProvider == null
                ? new SubscriptionPlanProvider(null)
                : subscriptionPlanProvider;

        this.subscriptionPlanBillingPeriodProvider = subscriptionPlanBillingPeriodProvider == null
                ? new SubscriptionPlanBillingPeriodProvider(null)
                : subscriptionPlanBillingPeriodProvider;

        this.timingProvider = timingProvider == null
                ? new CustomerWorkflowTimingProvider()
                : timingProvider;
    }

    public CustomerSubscription createTrialSubscription(Integer customerId) {
        if (customerId == null) {
            log.warn("Trial subscription could not be created because customerId is missing.");
            return null;
        }

        CustomerModule customerModule = findPrimaryCustomerModule(customerId);

        if (customerModule == null || customerModule.getSubscriptionPlanId() == null) {
            log.warn("Trial subscription could not be created because customer module/plan was not found. customerId={}", customerId);
            return null;
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanProvider.getPlanById(customerModule.getSubscriptionPlanId());

        if (subscriptionPlan == null) {
            log.warn(
                    "Trial subscription could not be created because subscription plan was not found. customerId={}, subscriptionPlanId={}",
                    customerId,
                    customerModule.getSubscriptionPlanId()
            );
            return null;
        }

        Timestamp trialStartAt = timingProvider.trialStartAt();
        Timestamp trialEndAt = calculateTrialEndAt(
                trialStartAt,
                subscriptionPlan
        );
        SubscriptionPlanBillingPeriod billingPeriod = resolveBillingPeriod(customerModule, subscriptionPlan);

        CustomerSubscription subscription = new CustomerSubscription();

        subscription.setCustomerId(customerId);
        subscription.setSubscriptionStatus(CustomerSubscriptionStatus.TRIAL);
        subscription.setSubscriptionPlanId(subscriptionPlan.getSubscriptionPlanId());
        subscription.setSubscriptionPlanBillingPeriodId(billingPeriod == null ? null : billingPeriod.getSubscriptionPlanBillingPeriodId());
        subscription.setSubscriptionPlanName(subscriptionPlan.getDisplayName());
        subscription.setTrialStartAt(trialStartAt);
        subscription.setTrialEndAt(trialEndAt);
        subscription.setTrialReminderSentAt(null);
        subscription.setPeriodStartAt(null);
        subscription.setPeriodEndAt(null);
        subscription.setRenewalReminderSentAt(null);
        subscription.setContinuationConfirmedAt(null);
        subscription.setRenewalConfirmedAt(null);
        subscription.setGracePeriodEndsAt(null);

        Integer subscriptionId = subscriptionProvider.createSubscription(subscription);

        if (subscriptionId == null) {
            log.warn("Trial subscription could not be persisted. customerId={}", customerId);
            return null;
        }

        subscription.setSubscriptionId(subscriptionId);

        customerModuleProvider.updateCustomerModuleStatus(
                customerModule.getCustomerModuleId(),
                CustomerModuleStatus.TRIAL
        );

        log.info(
                "Trial subscription created. customerId={}, subscriptionId={}, subscriptionPlanId={}, subscriptionPlanBillingPeriodId={}, trialStartAt={}, trialEndAt={}",
                customerId,
                subscriptionId,
                subscriptionPlan.getSubscriptionPlanId(),
                subscription.getSubscriptionPlanBillingPeriodId(),
                trialStartAt,
                trialEndAt
        );

        return subscription;
    }

    public Timestamp getTrialReminderAt(CustomerSubscription subscription) {
        if (subscription == null || subscription.getTrialEndAt() == null) {
            return null;
        }

        return timingProvider.trialReminderAt(subscription.getTrialEndAt());
    }

    private CustomerModule findPrimaryCustomerModule(Integer customerId) {
        List<CustomerModule> modules = customerModuleProvider.getLatestCustomerModules(customerId);

        if (modules == null || modules.isEmpty()) {
            return null;
        }

        for (CustomerModule module : modules) {
            if (module != null && module.isTrial()) {
                return module;
            }
        }

        for (CustomerModule module : modules) {
            if (module != null && module.isActive()) {
                return module;
            }
        }

        return modules.get(0);
    }

    private Timestamp calculateTrialEndAt(
            Timestamp trialStartAt,
            SubscriptionPlan subscriptionPlan
    ) {
        int trialDays = subscriptionPlan == null
                ? timingProvider.getTrialDays()
                : subscriptionPlan.getTrialDays();

        if (trialDays <= 0) {
            trialDays = timingProvider.getTrialDays();
        }

        Instant startInstant = trialStartAt == null
                ? Instant.now()
                : trialStartAt.toInstant();

        return Timestamp.from(startInstant.plus(trialDays, ChronoUnit.DAYS));
    }

    private SubscriptionPlanBillingPeriod resolveBillingPeriod(
            CustomerModule customerModule,
            SubscriptionPlan subscriptionPlan
    ) {
        if (customerModule != null && customerModule.getSubscriptionPlanBillingPeriodId() != null) {
            SubscriptionPlanBillingPeriod billingPeriod = subscriptionPlanBillingPeriodProvider.getBillingPeriodById(
                    customerModule.getSubscriptionPlanBillingPeriodId()
            );

            if (billingPeriod != null) {
                return billingPeriod;
            }
        }

        if (subscriptionPlan == null || subscriptionPlan.getSubscriptionPlanId() == null) {
            return null;
        }

        List<SubscriptionPlanBillingPeriod> billingPeriods = subscriptionPlanBillingPeriodProvider.getBillingPeriodsByPlanId(
                subscriptionPlan.getSubscriptionPlanId()
        );

        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            if (billingPeriod != null && billingPeriod.getBillingPeriodMonths() == subscriptionPlan.getBillingPeriodMonths()) {
                return billingPeriod;
            }
        }

        return billingPeriods.isEmpty() ? null : billingPeriods.get(0);
    }
}
