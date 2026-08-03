package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanBillingPeriodProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CustomerPaymentRequestProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerPaymentRequestProvider.class);

    private static final String DEFAULT_PAYMENT_PROVIDER = "MANUAL";
    private static final String DEFAULT_CURRENCY = "EUR";

    private final CustomerPaymentProvider paymentProvider;
    private final CustomerSubscriptionProvider subscriptionProvider;
    private final CustomerWorkflowProvider workflowProvider;
    private final CustomerWorkflowTimingProvider timingProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;
    private final SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider;

    public CustomerPaymentRequestProvider() {
        this(
                new CustomerPaymentProvider(null),
                new CustomerSubscriptionProvider(null),
                new CustomerWorkflowProvider(null),
                new CustomerWorkflowTimingProvider(),
                new SubscriptionPlanProvider(null),
                new SubscriptionPlanBillingPeriodProvider(null)
        );
    }

    public CustomerPaymentRequestProvider(
            CustomerPaymentProvider paymentProvider,
            CustomerSubscriptionProvider subscriptionProvider,
            CustomerWorkflowProvider workflowProvider,
            CustomerWorkflowTimingProvider timingProvider
    ) {
        this(
                paymentProvider,
                subscriptionProvider,
                workflowProvider,
                timingProvider,
                new SubscriptionPlanProvider(null),
                new SubscriptionPlanBillingPeriodProvider(null)
        );
    }

    public CustomerPaymentRequestProvider(
            CustomerPaymentProvider paymentProvider,
            CustomerSubscriptionProvider subscriptionProvider,
            CustomerWorkflowProvider workflowProvider,
            CustomerWorkflowTimingProvider timingProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider
    ) {
        this.paymentProvider = paymentProvider == null ? new CustomerPaymentProvider(null) : paymentProvider;
        this.subscriptionProvider = subscriptionProvider == null ? new CustomerSubscriptionProvider(null) : subscriptionProvider;
        this.workflowProvider = workflowProvider == null ? new CustomerWorkflowProvider(null) : workflowProvider;
        this.timingProvider = timingProvider == null ? new CustomerWorkflowTimingProvider() : timingProvider;
        this.subscriptionPlanProvider = subscriptionPlanProvider == null ? new SubscriptionPlanProvider(null) : subscriptionPlanProvider;
        this.subscriptionPlanBillingPeriodProvider = subscriptionPlanBillingPeriodProvider == null
                ? new SubscriptionPlanBillingPeriodProvider(null)
                : subscriptionPlanBillingPeriodProvider;
    }

    public Integer requestPaymentForWorkflow(
            Integer workflowId,
            BigDecimal amount,
            String currency,
            String paymentProviderName,
            String paymentProviderReference
    ) {
        if (workflowId == null) {
            log.warn("Payment request could not be created because workflowId is missing.");
            return null;
        }

        CustomerWorkflow workflow = workflowProvider.getWorkflowById(workflowId);

        if (workflow == null) {
            log.warn("Payment request could not be created because workflow was not found. workflowId={}", workflowId);
            return null;
        }

        return requestPaymentForWorkflow(
                workflow,
                amount,
                currency,
                paymentProviderName,
                paymentProviderReference
        );
    }

    public Integer requestPaymentForWorkflow(
            CustomerWorkflow workflow,
            BigDecimal amount,
            String currency,
            String paymentProviderName,
            String paymentProviderReference
    ) {
        if (workflow == null || workflow.getWorkflowId() == null || workflow.getCustomerId() == null) {
            log.warn("Payment request could not be created because workflow is invalid.");
            return null;
        }

        CustomerSubscription subscription = workflow.getSubscriptionId() == null
                ? null
                : subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        SubscriptionPlan subscriptionPlan = loadSubscriptionPlan(subscription);
        SubscriptionPlanBillingPeriod billingPeriod = loadBillingPeriod(subscription);

        BigDecimal resolvedAmount = resolveAmount(
                amount,
                subscriptionPlan,
                billingPeriod
        );

        String resolvedCurrency = resolveCurrency(
                currency,
                subscriptionPlan,
                billingPeriod
        );

        Timestamp now = timingProvider.now();
        Timestamp paymentDueAt = now;
        Timestamp gracePeriodEndsAt = timingProvider.paymentGracePeriodEndsAt(paymentDueAt);

        CustomerPayment payment = new CustomerPayment();

        payment.setCustomerId(workflow.getCustomerId());
        payment.setSubscriptionId(subscription == null ? null : subscription.getSubscriptionId());
        payment.setPaymentStatus(CustomerPaymentStatus.REQUESTED);
        payment.setPaymentProvider(safeText(paymentProviderName, DEFAULT_PAYMENT_PROVIDER));
        payment.setPaymentProviderReference(safeText(paymentProviderReference, ""));
        payment.setAmount(resolvedAmount);
        payment.setCurrency(resolvedCurrency);
        payment.setPaymentDueAt(paymentDueAt);
        payment.setGracePeriodEndsAt(gracePeriodEndsAt);
        payment.setRequestedAt(now);

        Integer paymentId = paymentProvider.createPayment(payment);

        if (paymentId == null) {
            log.warn("Payment request could not be persisted. workflowId={}", workflow.getWorkflowId());
            return null;
        }

        if (subscription != null && subscription.getSubscriptionId() != null) {
            subscriptionProvider.updateSubscriptionStatus(
                    subscription.getSubscriptionId(),
                    CustomerSubscriptionStatus.PAYMENT_PENDING
            );
        }

        workflow.setPaymentId(paymentId);
        workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
        workflow.setCurrentState(CustomerWorkflowState.PAYMENT_PENDING);
        workflow.setNextActionAt(gracePeriodEndsAt);
        workflow.setLastError("");

        boolean workflowUpdated = workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.PAYMENT_REQUESTED,
                ""
        );

        if (!workflowUpdated) {
            log.warn(
                    "Payment was created but workflow could not be updated. workflowId={}, paymentId={}",
                    workflow.getWorkflowId(),
                    paymentId
            );
        }

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                CustomerWorkflowEventType.PAYMENT_REQUESTED,
                workflow.getCurrentState(),
                CustomerWorkflowState.PAYMENT_PENDING,
                "Payment requested.",
                buildPaymentPayloadJson(paymentId, resolvedAmount, resolvedCurrency),
                null
        );

        workflowProvider.createWorkflowEvent(event);

        log.info(
                "Payment requested. workflowId={}, customerId={}, subscriptionId={}, paymentId={}, amount={}, currency={}",
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                subscription == null ? null : subscription.getSubscriptionId(),
                paymentId,
                payment.getAmount(),
                payment.getCurrency()
        );

        return paymentId;
    }

    private SubscriptionPlan loadSubscriptionPlan(CustomerSubscription subscription) {
        if (subscription == null || subscription.getSubscriptionPlanId() == null) {
            return null;
        }

        return subscriptionPlanProvider.getPlanById(subscription.getSubscriptionPlanId());
    }

    private SubscriptionPlanBillingPeriod loadBillingPeriod(CustomerSubscription subscription) {
        if (subscription == null || subscription.getSubscriptionPlanBillingPeriodId() == null) {
            return null;
        }

        return subscriptionPlanBillingPeriodProvider.getBillingPeriodById(subscription.getSubscriptionPlanBillingPeriodId());
    }

    private BigDecimal resolveAmount(
            BigDecimal requestedAmount,
            SubscriptionPlan subscriptionPlan,
            SubscriptionPlanBillingPeriod billingPeriod
    ) {
        if (requestedAmount != null && requestedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return requestedAmount;
        }

        if (billingPeriod != null && billingPeriod.getPriceAmount() != null) {
            return billingPeriod.getPriceAmount();
        }

        if (subscriptionPlan != null && subscriptionPlan.getPriceAmount() != null) {
            return subscriptionPlan.getPriceAmount();
        }

        return BigDecimal.ZERO;
    }

    private String resolveCurrency(
            String requestedCurrency,
            SubscriptionPlan subscriptionPlan,
            SubscriptionPlanBillingPeriod billingPeriod
    ) {
        if (requestedCurrency != null && !requestedCurrency.trim().isEmpty()) {
            return requestedCurrency.trim().toUpperCase();
        }

        if (billingPeriod != null && billingPeriod.getCurrency() != null && !billingPeriod.getCurrency().trim().isEmpty()) {
            return billingPeriod.getCurrency().trim().toUpperCase();
        }

        if (subscriptionPlan != null
                && subscriptionPlan.getCurrency() != null
                && !subscriptionPlan.getCurrency().trim().isEmpty()) {
            return subscriptionPlan.getCurrency().trim().toUpperCase();
        }

        return DEFAULT_CURRENCY;
    }

    private String buildPaymentPayloadJson(
            Integer paymentId,
            BigDecimal amount,
            String currency
    ) {
        return "{"
                + "\"paymentId\":\"" + safeJson(paymentId == null ? "" : paymentId.toString()) + "\","
                + "\"amount\":\"" + safeJson(amount == null ? "0" : amount.toPlainString()) + "\","
                + "\"currency\":\"" + safeJson(currency) + "\""
                + "}";
    }

    private String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
