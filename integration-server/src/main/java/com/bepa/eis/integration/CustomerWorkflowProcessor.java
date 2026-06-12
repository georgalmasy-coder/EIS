package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerContactInfo;
import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerTokenType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowMailType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerAccessProvider;
import com.bepa.eis.common.providers.customer.CustomerContactInfoProvider;
import com.bepa.eis.common.providers.customer.CustomerPaymentProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionActivationProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionProvider;
import com.bepa.eis.common.providers.customer.CustomerTokenProvider;
import com.bepa.eis.common.providers.customer.CustomerTrialSubscriptionProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowMailProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerWorkflowProcessor {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowProcessor.class);

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final CustomerWorkflowProvider workflowProvider;
    private final CustomerSubscriptionProvider subscriptionProvider;
    private final CustomerPaymentProvider paymentProvider;
    private final CustomerTokenProvider tokenProvider;
    private final CustomerWorkflowMailProvider workflowMailProvider;
    private final CustomerWorkflowTimingProvider timingProvider;
    private final CustomerContactInfoProvider customerContactInfoProvider;
    private final CustomerAccessProvider customerAccessProvider;
    private final CustomerSubscriptionActivationProvider subscriptionActivationProvider;
    private final CustomerTrialSubscriptionProvider trialSubscriptionProvider;

    private final String workerId;
    private final int batchSize;

    public CustomerWorkflowProcessor(String workerId) {
        this(
                new CustomerWorkflowProvider(null),
                new CustomerSubscriptionProvider(null),
                new CustomerPaymentProvider(null),
                new CustomerTokenProvider(null),
                new CustomerWorkflowMailProvider((WebSession) null),
                new CustomerWorkflowTimingProvider(),
                new CustomerContactInfoProvider(null),
                new CustomerAccessProvider(null),
                new CustomerSubscriptionActivationProvider(null),
                new CustomerTrialSubscriptionProvider(null),
                workerId,
                DEFAULT_BATCH_SIZE
        );
    }

    public CustomerWorkflowProcessor(
            CustomerWorkflowProvider workflowProvider,
            String workerId,
            int batchSize
    ) {
        this(
                workflowProvider,
                new CustomerSubscriptionProvider(null),
                new CustomerPaymentProvider(null),
                new CustomerTokenProvider(null),
                new CustomerWorkflowMailProvider((WebSession) null),
                new CustomerWorkflowTimingProvider(),
                new CustomerContactInfoProvider(null),
                new CustomerAccessProvider(null),
                new CustomerSubscriptionActivationProvider(null),
                new CustomerTrialSubscriptionProvider(null),
                workerId,
                batchSize
        );
    }

    public CustomerWorkflowProcessor(
            CustomerWorkflowProvider workflowProvider,
            CustomerSubscriptionProvider subscriptionProvider,
            CustomerPaymentProvider paymentProvider,
            CustomerTokenProvider tokenProvider,
            CustomerWorkflowMailProvider workflowMailProvider,
            CustomerWorkflowTimingProvider timingProvider,
            CustomerContactInfoProvider customerContactInfoProvider,
            CustomerAccessProvider customerAccessProvider,
            CustomerSubscriptionActivationProvider subscriptionActivationProvider,
            CustomerTrialSubscriptionProvider trialSubscriptionProvider,
            String workerId,
            int batchSize
    ) {
        this.workflowProvider = workflowProvider == null ? new CustomerWorkflowProvider(null) : workflowProvider;
        this.subscriptionProvider = subscriptionProvider == null ? new CustomerSubscriptionProvider(null) : subscriptionProvider;
        this.paymentProvider = paymentProvider == null ? new CustomerPaymentProvider(null) : paymentProvider;
        this.tokenProvider = tokenProvider == null ? new CustomerTokenProvider(null) : tokenProvider;
        this.workflowMailProvider = workflowMailProvider == null ? new CustomerWorkflowMailProvider((WebSession) null) : workflowMailProvider;
        this.timingProvider = timingProvider == null ? new CustomerWorkflowTimingProvider() : timingProvider;
        this.customerContactInfoProvider = customerContactInfoProvider == null ? new CustomerContactInfoProvider(null) : customerContactInfoProvider;
        this.customerAccessProvider = customerAccessProvider == null ? new CustomerAccessProvider(null) : customerAccessProvider;
        this.subscriptionActivationProvider = subscriptionActivationProvider == null ? new CustomerSubscriptionActivationProvider(null) : subscriptionActivationProvider;
        this.trialSubscriptionProvider = trialSubscriptionProvider == null ? new CustomerTrialSubscriptionProvider(null) : trialSubscriptionProvider;
        this.workerId = normalizeWorkerId(workerId);
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
    }

    public int processDueWorkflows() {
        List<CustomerWorkflow> dueWorkflows = workflowProvider.getDueWorkflows(batchSize);

        if (dueWorkflows.isEmpty()) {
            log.debug("No due customer workflows found.");
            return 0;
        }

        log.info("Found {} due customer workflow(s).", dueWorkflows.size());

        int processedCount = 0;

        for (CustomerWorkflow workflow : dueWorkflows) {
            if (processWorkflowSafely(workflow)) {
                processedCount++;
            }
        }

        log.info("Processed {} customer workflow(s).", processedCount);

        return processedCount;
    }

    private boolean processWorkflowSafely(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getWorkflowId() == null) {
            return false;
        }

        Integer workflowId = workflow.getWorkflowId();
        boolean locked = workflowProvider.lockWorkflow(workflowId, workerId);

        if (!locked) {
            log.debug("Customer workflow could not be locked. workflowId={}", workflowId);
            return false;
        }

        CustomerWorkflow lockedWorkflow = workflowProvider.getWorkflowById(workflowId);

        if (lockedWorkflow == null) {
            workflowProvider.releaseWorkflowLock(workflowId);
            log.warn("Customer workflow was locked but could not be reloaded. workflowId={}", workflowId);
            return false;
        }

        try {
            processWorkflow(lockedWorkflow);
            return true;
        } catch (Exception e) {
            log.error("Error processing customer workflow. workflowId={}", workflowId, e);

            workflowProvider.markWorkflowError(
                    workflowId,
                    lockedWorkflow.getCurrentState(),
                    CustomerWorkflowEventType.WORKFLOW_ERROR,
                    safeExceptionMessage(e),
                    shouldRequireManualAttention(lockedWorkflow)
            );

            createWorkflowEvent(
                    lockedWorkflow,
                    CustomerWorkflowEventType.WORKFLOW_ERROR,
                    lockedWorkflow.getCurrentState(),
                    lockedWorkflow.getCurrentState(),
                    "Customer workflow processing failed: " + safeExceptionMessage(e),
                    "{}"
            );

            return false;
        }
    }

    private void processWorkflow(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getWorkflowId() == null) {
            return;
        }

        if (!workflow.canBeProcessedAutomatically()) {
            workflowProvider.releaseWorkflowLock(workflow.getWorkflowId());
            log.debug(
                    "Customer workflow cannot be processed automatically. workflowId={}, status={}",
                    workflow.getWorkflowId(),
                    workflow.getWorkflowStatusCode()
            );
            return;
        }

        CustomerWorkflowState currentState = workflow.getCurrentState();

        if (currentState == null) {
            transitionToManualAttention(workflow, "Customer workflow has no current state.");
            return;
        }

        log.info(
                "Processing customer workflow. workflowId={}, customerId={}, state={}",
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                workflow.getCurrentStateCode()
        );

        switch (currentState) {
            case CREATED -> processCreated(workflow);
            case PENDING_EMAIL_CONFIRMATION -> processPendingEmailConfirmation(workflow);
            case EMAIL_CONFIRMED -> processEmailConfirmed(workflow);
            case TRIAL_ACTIVE -> processTrialActive(workflow);
            case TRIAL_EXPIRING -> processTrialExpiring(workflow);
            case PENDING_SUBSCRIPTION_CONFIRMATION -> processPendingSubscriptionConfirmation(workflow);
            case PAYMENT_PENDING -> processPaymentPending(workflow);
            case SUBSCRIPTION_ACTIVE -> processSubscriptionActive(workflow);
            case SUBSCRIPTION_EXPIRING -> processSubscriptionExpiring(workflow);
            case PAYMENT_OVERDUE -> processPaymentOverdue(workflow);
            case SUSPENDED -> processSuspended(workflow);
            case CANCELLED -> processCancelled(workflow);
            case WAITING_FOR_MANUAL_ATTENTION -> processWaitingForManualAttention(workflow);
            default -> transitionToManualAttention(
                    workflow,
                    "Unsupported customer workflow state: " + currentState
            );
        }
    }

    private void processCreated(CustomerWorkflow workflow) {
        CustomerContactInfo customerContactInfo = loadCustomerContactInfo(workflow);
        Timestamp confirmationExpiresAt = timingProvider.confirmationTokenExpiresAt();

        CustomerTokenProvider.CreatedCustomerToken token = tokenProvider.createToken(
                workflow.getCustomerId(),
                workflow.getWorkflowId(),
                workflow.getSubscriptionId(),
                workflow.getPaymentId(),
                CustomerTokenType.EMAIL_CONFIRMATION,
                confirmationExpiresAt,
                null
        );

        if (token == null) {
            throw new IllegalStateException("Email confirmation token could not be created.");
        }

        Map<String, Object> parameters = buildBaseMailParameters(workflow, customerContactInfo);
        parameters.put("trialDays", timingProvider.getTrialDays());
        parameters.put("confirmationExpiresAt", confirmationExpiresAt);
        parameters.put("confirmationToken", token.getRawToken());
        parameters.put("confirmationLink", buildCustomerActionLink("confirm-customer", token.getRawToken()));

        Integer mailId = createWorkflowMail(
                CustomerWorkflowMailType.CUSTOMER_CONFIRMATION,
                customerContactInfo,
                parameters
        );

        if (mailId == null) {
            log.warn(
                    "Customer confirmation mail was not queued. workflowId={}, customerId={}",
                    workflow.getWorkflowId(),
                    workflow.getCustomerId()
            );
        }

        updateCustomerStatus(workflow, CustomerStatus.PENDING_CONFIRMATION);

        transitionWorkflow(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_EMAIL_CONFIRMATION_REQUESTED,
                CustomerWorkflowState.PENDING_EMAIL_CONFIRMATION,
                confirmationExpiresAt,
                "Customer workflow started and email confirmation is required.",
                "{\"tokenType\":\"EMAIL_CONFIRMATION\"}"
        );
    }

    private void processPendingEmailConfirmation(CustomerWorkflow workflow) {
        if (workflow.getNextActionAt() != null && timingProvider.isDue(workflow.getNextActionAt())) {
            updateCustomerStatus(workflow, CustomerStatus.CANCELLED);

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.CUSTOMER_EMAIL_CONFIRMATION_EXPIRED,
                    CustomerWorkflowState.CANCELLED,
                    null,
                    "Customer email confirmation expired before the customer confirmed the account.",
                    "{}"
            );
            return;
        }

        releaseWithoutStateChange(workflow, "Customer workflow is waiting for email confirmation.");
    }

    private void processEmailConfirmed(CustomerWorkflow workflow) {
        CustomerSubscription subscription = trialSubscriptionProvider.createTrialSubscription(workflow.getCustomerId());

        if (subscription == null || subscription.getSubscriptionId() == null) {
            throw new IllegalStateException("Trial subscription could not be created.");
        }

        Timestamp trialReminderAt = trialSubscriptionProvider.getTrialReminderAt(subscription);

        workflow.setSubscriptionId(subscription.getSubscriptionId());

        updateCustomerStatus(workflow, CustomerStatus.TRIAL_ACTIVE);

        transitionWorkflow(
                workflow,
                CustomerWorkflowEventType.TRIAL_STARTED,
                CustomerWorkflowState.TRIAL_ACTIVE,
                trialReminderAt,
                "Customer email has been confirmed and trial has started.",
                "{\"subscriptionId\":\"" + subscription.getSubscriptionId() + "\"}"
        );
    }

    private void processTrialActive(CustomerWorkflow workflow) {
        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        if (subscription == null) {
            transitionToManualAttention(workflow, "Trial workflow has no subscription record.");
            return;
        }

        if (subscription.getTrialEndAt() != null && timingProvider.isDue(subscription.getTrialEndAt())) {
            suspendCustomer(workflow, "Trial expired before continuation was confirmed.");

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.TRIAL_EXPIRED,
                    CustomerWorkflowState.SUSPENDED,
                    null,
                    "Trial expired before continuation was confirmed.",
                    "{}"
            );
            return;
        }

        updateCustomerStatus(workflow, CustomerStatus.TRIAL_ACTIVE);

        transitionWorkflow(
                workflow,
                CustomerWorkflowEventType.TRIAL_EXPIRING_SOON,
                CustomerWorkflowState.TRIAL_EXPIRING,
                subscription.getTrialEndAt(),
                "Trial period is close to expiry.",
                "{}"
        );
    }

    private void processTrialExpiring(CustomerWorkflow workflow) {
        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        if (subscription == null) {
            transitionToManualAttention(workflow, "Trial expiring workflow has no subscription record.");
            return;
        }

        CustomerContactInfo customerContactInfo = loadCustomerContactInfo(workflow);

        CustomerTokenProvider.CreatedCustomerToken token = tokenProvider.createToken(
                workflow.getCustomerId(),
                workflow.getWorkflowId(),
                workflow.getSubscriptionId(),
                workflow.getPaymentId(),
                CustomerTokenType.SUBSCRIPTION_CONTINUATION,
                subscription.getTrialEndAt(),
                null
        );

        Map<String, Object> parameters = buildBaseMailParameters(workflow, customerContactInfo);
        parameters.put("trialStartDate", subscription.getTrialStartAt());
        parameters.put("trialEndDate", subscription.getTrialEndAt());
        parameters.put("subscriptionContinuationLink", token == null ? "" : buildCustomerActionLink("continue-subscription", token.getRawToken()));

        createWorkflowMail(CustomerWorkflowMailType.TRIAL_EXPIRING, customerContactInfo, parameters);

        subscription.setTrialReminderSentAt(timingProvider.now());
        subscriptionProvider.updateSubscription(subscription);

        updateCustomerStatus(workflow, CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION);

        transitionWorkflow(
                workflow,
                CustomerWorkflowEventType.TRIAL_EXPIRATION_MAIL_SENT,
                CustomerWorkflowState.PENDING_SUBSCRIPTION_CONFIRMATION,
                subscription.getTrialEndAt(),
                "Trial expiration reminder was processed and subscription continuation confirmation is required.",
                "{}"
        );
    }

    private void processPendingSubscriptionConfirmation(CustomerWorkflow workflow) {
        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        if (subscription != null && subscription.getTrialEndAt() != null && timingProvider.isDue(subscription.getTrialEndAt())) {
            suspendCustomer(workflow, "Subscription continuation was not confirmed before trial expiry.");

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.SUBSCRIPTION_CONTINUATION_CONFIRMATION_EXPIRED,
                    CustomerWorkflowState.SUSPENDED,
                    null,
                    "Subscription continuation was not confirmed before trial expiry.",
                    "{}"
            );
            return;
        }

        releaseWithoutStateChange(workflow, "Customer workflow is waiting for subscription continuation confirmation.");
    }

    private void processPaymentPending(CustomerWorkflow workflow) {
        CustomerPayment payment = paymentProvider.getPaymentById(workflow.getPaymentId());

        if (payment == null) {
            updateCustomerStatus(workflow, CustomerStatus.PAYMENT_PENDING);
            releaseWithoutStateChange(workflow, "Customer workflow is waiting for payment creation or callback.");
            return;
        }

        if (payment.isSuccessful()) {
            activateSubscriptionAfterSuccessfulPayment(workflow);

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.PAYMENT_SUCCEEDED,
                    CustomerWorkflowState.SUBSCRIPTION_ACTIVE,
                    nextSubscriptionRenewalActionAt(workflow),
                    "Payment succeeded and subscription has been activated.",
                    "{}"
            );
            return;
        }

        if (payment.isFailed()) {
            markSubscriptionPaymentOverdue(workflow);
            updateCustomerStatus(workflow, CustomerStatus.PAYMENT_OVERDUE);

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.PAYMENT_FAILED,
                    CustomerWorkflowState.PAYMENT_OVERDUE,
                    payment.getGracePeriodEndsAt(),
                    "Payment failed and is now overdue.",
                    "{}"
            );
            return;
        }

        updateCustomerStatus(workflow, CustomerStatus.PAYMENT_PENDING);
        releaseWithoutStateChange(workflow, "Customer workflow is waiting for payment confirmation.");
    }

    private void processSubscriptionActive(CustomerWorkflow workflow) {
        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        if (subscription == null || subscription.getPeriodEndAt() == null) {
            updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_ACTIVE);
            releaseWithoutStateChange(workflow, "Customer workflow is active but subscription period is not yet available.");
            return;
        }

        Timestamp renewalReminderAt = timingProvider.subscriptionRenewalReminderAt(subscription.getPeriodEndAt());

        if (timingProvider.isDue(renewalReminderAt)) {
            updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_EXPIRING);

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.SUBSCRIPTION_EXPIRING_SOON,
                    CustomerWorkflowState.SUBSCRIPTION_EXPIRING,
                    subscription.getPeriodEndAt(),
                    "Subscription is close to expiry.",
                    "{}"
            );
            return;
        }

        updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_ACTIVE);

        workflow.setNextActionAt(renewalReminderAt);
        workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.SUBSCRIPTION_ACTIVATED,
                ""
        );
    }

    private void processSubscriptionExpiring(CustomerWorkflow workflow) {
        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());
        CustomerContactInfo customerContactInfo = loadCustomerContactInfo(workflow);

        Map<String, Object> parameters = buildBaseMailParameters(workflow, customerContactInfo);

        if (subscription != null) {
            parameters.put("subscriptionId", subscription.getSubscriptionId());
            parameters.put("subscriptionPlanName", subscription.getSubscriptionPlanName());
            parameters.put("subscriptionEndDate", subscription.getPeriodEndAt());
            parameters.put("renewalConfirmationDeadline", subscription.getPeriodEndAt());
        }

        parameters.put("subscriptionRenewalLink", "");

        createWorkflowMail(CustomerWorkflowMailType.SUBSCRIPTION_EXPIRING, customerContactInfo, parameters);

        updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_EXPIRING);
        releaseWithoutStateChange(workflow, "Customer workflow is waiting for subscription renewal confirmation.");
    }

    private void processPaymentOverdue(CustomerWorkflow workflow) {
        CustomerPayment payment = paymentProvider.getPaymentById(workflow.getPaymentId());

        if (payment != null && payment.isSuccessful()) {
            activateSubscriptionAfterSuccessfulPayment(workflow);

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.PAYMENT_SUCCEEDED,
                    CustomerWorkflowState.SUBSCRIPTION_ACTIVE,
                    nextSubscriptionRenewalActionAt(workflow),
                    "Overdue payment was completed successfully and subscription has been activated.",
                    "{}"
            );
            return;
        }

        if (payment != null && payment.getGracePeriodEndsAt() != null && timingProvider.isDue(payment.getGracePeriodEndsAt())) {
            suspendCustomer(workflow, "Payment grace period expired.");

            transitionWorkflow(
                    workflow,
                    CustomerWorkflowEventType.PAYMENT_GRACE_PERIOD_EXPIRED,
                    CustomerWorkflowState.SUSPENDED,
                    null,
                    "Payment grace period expired.",
                    "{}"
            );
            return;
        }

        CustomerContactInfo customerContactInfo = loadCustomerContactInfo(workflow);
        Map<String, Object> parameters = buildBaseMailParameters(workflow, customerContactInfo);

        if (payment != null) {
            parameters.put("paymentId", payment.getPaymentId());
            parameters.put("paymentAmount", payment.getAmount());
            parameters.put("paymentCurrency", payment.getCurrency());
            parameters.put("paymentDueDate", payment.getPaymentDueAt());
            parameters.put("paymentGracePeriodEndsAt", payment.getGracePeriodEndsAt());
            parameters.put("paymentReference", payment.getPaymentProviderReference());
            parameters.put("paymentLink", "");
        }

        createWorkflowMail(CustomerWorkflowMailType.PAYMENT_OVERDUE, customerContactInfo, parameters);

        updateCustomerStatus(workflow, CustomerStatus.PAYMENT_OVERDUE);
        releaseWithoutStateChange(workflow, "Customer workflow is waiting for overdue payment to be completed.");
    }

    private void processSuspended(CustomerWorkflow workflow) {
        suspendCustomer(workflow, "Customer workflow is suspended.");

        CustomerContactInfo customerContactInfo = loadCustomerContactInfo(workflow);
        Map<String, Object> parameters = buildBaseMailParameters(workflow, customerContactInfo);
        parameters.put("suspendedAt", timingProvider.now());
        parameters.put("suspensionReason", "Customer workflow is suspended.");
        parameters.put("subscriptionStatus", "");
        parameters.put("paymentStatus", "");
        parameters.put("reactivationLink", "");

        createWorkflowMail(CustomerWorkflowMailType.CUSTOMER_SUSPENDED, customerContactInfo, parameters);

        workflow.setWorkflowStatus(CustomerWorkflowStatus.SUSPENDED);
        workflow.setNextActionAt(null);

        workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_SUSPENDED,
                ""
        );

        createWorkflowEvent(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_SUSPENDED,
                CustomerWorkflowState.SUSPENDED,
                CustomerWorkflowState.SUSPENDED,
                "Customer workflow is suspended.",
                "{}"
        );
    }

    private void processCancelled(CustomerWorkflow workflow) {
        updateCustomerStatus(workflow, CustomerStatus.CANCELLED);

        workflow.setWorkflowStatus(CustomerWorkflowStatus.CANCELLED);
        workflow.setNextActionAt(null);

        workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_CANCELLED,
                ""
        );

        createWorkflowEvent(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_CANCELLED,
                CustomerWorkflowState.CANCELLED,
                CustomerWorkflowState.CANCELLED,
                "Customer workflow is cancelled.",
                "{}"
        );
    }

    private void processWaitingForManualAttention(CustomerWorkflow workflow) {
        workflow.setWorkflowStatus(CustomerWorkflowStatus.WAITING_FOR_MANUAL_ATTENTION);
        workflow.setNextActionAt(null);

        workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.WORKFLOW_MANUAL_ATTENTION_REQUIRED,
                workflow.getLastError()
        );

        createWorkflowEvent(
                workflow,
                CustomerWorkflowEventType.WORKFLOW_MANUAL_ATTENTION_REQUIRED,
                CustomerWorkflowState.WAITING_FOR_MANUAL_ATTENTION,
                CustomerWorkflowState.WAITING_FOR_MANUAL_ATTENTION,
                "Customer workflow is waiting for manual attention.",
                "{}"
        );
    }

    private void activateSubscriptionAfterSuccessfulPayment(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getSubscriptionId() == null) {
            updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_ACTIVE);
            return;
        }

        boolean activated = subscriptionActivationProvider.activateSubscription(workflow.getSubscriptionId());

        if (!activated) {
            log.warn(
                    "Subscription could not be activated after payment success. workflowId={}, subscriptionId={}",
                    workflow.getWorkflowId(),
                    workflow.getSubscriptionId()
            );
        }

        updateCustomerStatus(workflow, CustomerStatus.SUBSCRIPTION_ACTIVE);
    }

    private void markSubscriptionPaymentOverdue(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getSubscriptionId() == null) {
            return;
        }

        boolean updated = subscriptionActivationProvider.markSubscriptionPaymentOverdue(workflow.getSubscriptionId());

        if (!updated) {
            log.warn(
                    "Subscription could not be marked payment overdue. workflowId={}, subscriptionId={}",
                    workflow.getWorkflowId(),
                    workflow.getSubscriptionId()
            );
        }
    }

    private Timestamp nextSubscriptionRenewalActionAt(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getSubscriptionId() == null) {
            return null;
        }

        CustomerSubscription subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());

        if (subscription == null || subscription.getPeriodEndAt() == null) {
            return null;
        }

        return timingProvider.subscriptionRenewalReminderAt(subscription.getPeriodEndAt());
    }

    private CustomerContactInfo loadCustomerContactInfo(CustomerWorkflow workflow) {
        if (workflow == null || workflow.getCustomerId() == null) {
            return new CustomerContactInfo();
        }

        CustomerContactInfo info = customerContactInfoProvider.getCustomerContactInfo(workflow.getCustomerId());

        if (info != null) {
            return info;
        }

        CustomerContactInfo fallback = new CustomerContactInfo();
        fallback.setCustomerId(workflow.getCustomerId());
        fallback.setCustomerName("Customer " + workflow.getCustomerId());
        fallback.setContactName("Customer");

        return fallback;
    }

    private Map<String, Object> buildBaseMailParameters(
            CustomerWorkflow workflow,
            CustomerContactInfo customerContactInfo
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("applicationName", "BEPA EIS");
        parameters.put("customerId", workflow == null ? "" : workflow.getCustomerId());
        parameters.put("workflowId", workflow == null ? "" : workflow.getWorkflowId());
        parameters.put("customerName", customerContactInfo == null ? "Customer" : customerContactInfo.getSafeCustomerName());
        parameters.put("customerContactName", customerContactInfo == null ? "Customer" : customerContactInfo.getDisplayName());

        return parameters;
    }

    private Integer createWorkflowMail(
            CustomerWorkflowMailType mailType,
            CustomerContactInfo customerContactInfo,
            Map<String, Object> parameters
    ) {
        if (customerContactInfo == null || !customerContactInfo.canReceiveWorkflowMail()) {
            log.warn(
                    "Customer workflow mail recipient is missing or invalid. mailType={}, customerId={}",
                    mailType == null ? "" : mailType.getCode(),
                    customerContactInfo == null ? null : customerContactInfo.getCustomerId()
            );
            return null;
        }

        return workflowMailProvider.createWorkflowMail(
                mailType,
                customerContactInfo.getDisplayName(),
                customerContactInfo.getContactEmail(),
                parameters
        );
    }

    private void suspendCustomer(
            CustomerWorkflow workflow,
            String reason
    ) {
        if (workflow == null || workflow.getCustomerId() == null) {
            return;
        }

        boolean suspended = customerAccessProvider.suspendCustomer(
                workflow.getCustomerId(),
                reason
        );

        if (!suspended) {
            log.warn(
                    "Customer access could not be suspended. customerId={}, reason={}",
                    workflow.getCustomerId(),
                    reason
            );
        }

        if (workflow.getSubscriptionId() != null) {
            subscriptionActivationProvider.suspendSubscription(workflow.getSubscriptionId());
        }
    }

    private void updateCustomerStatus(
            CustomerWorkflow workflow,
            CustomerStatus customerStatus
    ) {
        if (workflow == null || workflow.getCustomerId() == null || customerStatus == null) {
            return;
        }

        boolean updated = customerAccessProvider.setCustomerStatus(
                workflow.getCustomerId(),
                customerStatus
        );

        if (!updated) {
            log.warn(
                    "Customer status could not be updated. customerId={}, customerStatus={}",
                    workflow.getCustomerId(),
                    customerStatus.getCode()
            );
        }
    }

    private void transitionWorkflow(
            CustomerWorkflow workflow,
            CustomerWorkflowEventType eventType,
            CustomerWorkflowState toState,
            Timestamp nextActionAt,
            String description,
            String payloadJson
    ) {
        CustomerWorkflowState fromState = workflow.getCurrentState();

        workflow.setCurrentState(toState);
        workflow.setWorkflowStatus(toState == CustomerWorkflowState.CANCELLED
                ? CustomerWorkflowStatus.CANCELLED
                : toState == CustomerWorkflowState.SUSPENDED
                  ? CustomerWorkflowStatus.SUSPENDED
                  : CustomerWorkflowStatus.ACTIVE);
        workflow.setNextActionAt(nextActionAt);
        workflow.setLastError("");

        boolean updated = workflowProvider.updateWorkflowState(
                workflow,
                eventType,
                ""
        );

        if (!updated) {
            throw new IllegalStateException("Customer workflow state could not be updated. workflowId=" + workflow.getWorkflowId());
        }

        createWorkflowEvent(
                workflow,
                eventType,
                fromState,
                toState,
                description,
                payloadJson
        );
    }

    private void transitionToManualAttention(
            CustomerWorkflow workflow,
            String reason
    ) {
        CustomerWorkflowState fromState = workflow.getCurrentState();

        workflow.setWorkflowStatus(CustomerWorkflowStatus.WAITING_FOR_MANUAL_ATTENTION);
        workflow.setCurrentState(CustomerWorkflowState.WAITING_FOR_MANUAL_ATTENTION);
        workflow.setNextActionAt(null);
        workflow.setLastError(reason);

        boolean updated = workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.WORKFLOW_MANUAL_ATTENTION_REQUIRED,
                reason
        );

        if (!updated) {
            throw new IllegalStateException("Customer workflow could not be moved to manual attention. workflowId=" + workflow.getWorkflowId());
        }

        createWorkflowEvent(
                workflow,
                CustomerWorkflowEventType.WORKFLOW_MANUAL_ATTENTION_REQUIRED,
                fromState,
                CustomerWorkflowState.WAITING_FOR_MANUAL_ATTENTION,
                reason,
                "{}"
        );
    }

    private void releaseWithoutStateChange(
            CustomerWorkflow workflow,
            String reason
    ) {
        workflowProvider.releaseWorkflowLock(workflow.getWorkflowId());

        log.debug(
                "Customer workflow released without state change. workflowId={}, state={}, reason={}",
                workflow.getWorkflowId(),
                workflow.getCurrentStateCode(),
                reason
        );
    }

    private void createWorkflowEvent(
            CustomerWorkflow workflow,
            CustomerWorkflowEventType eventType,
            CustomerWorkflowState fromState,
            CustomerWorkflowState toState,
            String description,
            String payloadJson
    ) {
        if (workflow == null || workflow.getWorkflowId() == null || workflow.getCustomerId() == null) {
            return;
        }

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                eventType,
                fromState,
                toState,
                description,
                payloadJson,
                null
        );

        workflowProvider.createWorkflowEvent(event);
    }

    private String buildCustomerActionLink(
            String action,
            String rawToken
    ) {
        String baseUrl = GlobalConfiguration.getCustomerWorkflowPortalBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl
                + "/"
                + safeText(action, "customer-action")
                + "?token="
                + safeText(rawToken, "");
    }

    private boolean shouldRequireManualAttention(CustomerWorkflow workflow) {
        if (workflow == null) {
            return true;
        }

        return workflow.getRetryCount() >= 3;
    }

    private String safeExceptionMessage(Exception exception) {
        if (exception == null) {
            return "Unknown customer workflow error";
        }

        if (exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception.getClass().getName();
        }

        return exception.getMessage().trim();
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

    private String normalizeWorkerId(String workerId) {
        if (workerId == null || workerId.trim().isEmpty()) {
            return "unknown-customer-workflow-worker-" + Instant.now().toEpochMilli();
        }

        return workerId.trim();
    }
}