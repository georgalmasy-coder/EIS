package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerToken;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerTokenType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

public class CustomerWorkflowActionProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowActionProvider.class);

    private static final String DEFAULT_PAYMENT_PROVIDER = "MANUAL";

    private final CustomerTokenProvider tokenProvider;
    private final CustomerWorkflowProvider workflowProvider;
    private final CustomerAccessProvider customerAccessProvider;
    private final CustomerWorkflowTimingProvider timingProvider;
    private final CustomerPaymentRequestProvider paymentRequestProvider;

    public CustomerWorkflowActionProvider() {
        this(null);
    }

    public CustomerWorkflowActionProvider(WebSession webSession) {
        this(
                new CustomerTokenProvider(webSession),
                new CustomerWorkflowProvider(webSession),
                new CustomerAccessProvider(webSession),
                new CustomerWorkflowTimingProvider(),
                new CustomerPaymentRequestProvider()
        );
    }

    public CustomerWorkflowActionProvider(
            CustomerTokenProvider tokenProvider,
            CustomerWorkflowProvider workflowProvider,
            CustomerAccessProvider customerAccessProvider,
            CustomerWorkflowTimingProvider timingProvider,
            CustomerPaymentRequestProvider paymentRequestProvider
    ) {
        this.tokenProvider = tokenProvider == null ? new CustomerTokenProvider(null) : tokenProvider;
        this.workflowProvider = workflowProvider == null ? new CustomerWorkflowProvider(null) : workflowProvider;
        this.customerAccessProvider = customerAccessProvider == null ? new CustomerAccessProvider(null) : customerAccessProvider;
        this.timingProvider = timingProvider == null ? new CustomerWorkflowTimingProvider() : timingProvider;
        this.paymentRequestProvider = paymentRequestProvider == null ? new CustomerPaymentRequestProvider() : paymentRequestProvider;
    }

    public CustomerWorkflowActionResult confirmCustomerEmail(String rawToken) {
        return processTokenAction(
                rawToken,
                CustomerTokenType.EMAIL_CONFIRMATION,
                CustomerWorkflowEventType.CUSTOMER_EMAIL_CONFIRMED,
                CustomerWorkflowState.EMAIL_CONFIRMED,
                CustomerStatus.TRIAL_ACTIVE,
                "Customer email confirmed.",
                false
        );
    }

    public CustomerWorkflowActionResult confirmSubscriptionContinuation(String rawToken) {
        return processTokenAction(
                rawToken,
                CustomerTokenType.SUBSCRIPTION_CONTINUATION,
                CustomerWorkflowEventType.SUBSCRIPTION_CONTINUATION_CONFIRMED,
                CustomerWorkflowState.PAYMENT_PENDING,
                CustomerStatus.PAYMENT_PENDING,
                "Customer confirmed subscription continuation.",
                true
        );
    }

    public CustomerWorkflowActionResult confirmSubscriptionRenewal(String rawToken) {
        return processTokenAction(
                rawToken,
                CustomerTokenType.SUBSCRIPTION_RENEWAL,
                CustomerWorkflowEventType.SUBSCRIPTION_RENEWAL_CONFIRMED,
                CustomerWorkflowState.PAYMENT_PENDING,
                CustomerStatus.PAYMENT_PENDING,
                "Customer confirmed subscription renewal.",
                true
        );
    }

    public CustomerWorkflowActionResult reactivateCustomer(String rawToken) {
        CustomerWorkflowActionResult result = processTokenAction(
                rawToken,
                CustomerTokenType.REACTIVATION,
                CustomerWorkflowEventType.CUSTOMER_REACTIVATED,
                CustomerWorkflowState.SUBSCRIPTION_ACTIVE,
                CustomerStatus.SUBSCRIPTION_ACTIVE,
                "Customer reactivated.",
                false
        );

        if (result.isSuccess() && result.getCustomerId() != null) {
            customerAccessProvider.reactivateCustomer(result.getCustomerId());
        }

        return result;
    }

    private CustomerWorkflowActionResult processTokenAction(
            String rawToken,
            CustomerTokenType expectedTokenType,
            CustomerWorkflowEventType eventType,
            CustomerWorkflowState toState,
            CustomerStatus toCustomerStatus,
            String description,
            boolean requestPayment
    ) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return CustomerWorkflowActionResult.failed("Token is missing.");
        }

        CustomerToken token = tokenProvider.getTokenByRawToken(rawToken);

        if (token == null) {
            return CustomerWorkflowActionResult.failed("Token was not found.");
        }

        if (expectedTokenType != null && token.getTokenType() != expectedTokenType) {
            return CustomerWorkflowActionResult.failed("Token type is invalid.");
        }

        Timestamp now = timingProvider.now();

        if (!token.isValid(now)) {
            return CustomerWorkflowActionResult.failed("Token is expired or already used.");
        }

        CustomerWorkflow workflow = workflowProvider.getWorkflowById(token.getWorkflowId());

        if (workflow == null) {
            return CustomerWorkflowActionResult.failed("Workflow was not found.");
        }

        CustomerWorkflowState fromState = workflow.getCurrentState();

        if (requestPayment) {
            tokenProvider.markTokenUsed(token.getTokenId());

            Integer paymentId = paymentRequestProvider.requestPaymentForWorkflow(
                    workflow,
                    null,
                    null,
                    DEFAULT_PAYMENT_PROVIDER,
                    ""
            );

            if (paymentId == null) {
                return CustomerWorkflowActionResult.failed("Payment request could not be created.");
            }

            updateCustomerStatus(
                    workflow.getCustomerId(),
                    toCustomerStatus
            );

            CustomerWorkflow updatedWorkflow = workflowProvider.getWorkflowById(workflow.getWorkflowId());

            return CustomerWorkflowActionResult.success(
                    workflow.getWorkflowId(),
                    workflow.getCustomerId(),
                    updatedWorkflow == null ? CustomerWorkflowState.PAYMENT_PENDING : updatedWorkflow.getCurrentState(),
                    description
            );
        }

        workflow.setCurrentState(toState);
        workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
        workflow.setNextActionAt(now);
        workflow.setLastError("");

        boolean updated = workflowProvider.updateWorkflowState(
                workflow,
                eventType,
                ""
        );

        if (!updated) {
            return CustomerWorkflowActionResult.failed("Workflow could not be updated.");
        }

        updateCustomerStatus(
                workflow.getCustomerId(),
                toCustomerStatus
        );

        boolean tokenUsed = tokenProvider.markTokenUsed(token.getTokenId());

        if (!tokenUsed) {
            log.warn(
                    "Token action updated workflow, but token could not be marked as used. tokenId={}, workflowId={}",
                    token.getTokenId(),
                    workflow.getWorkflowId()
            );
        }

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                eventType,
                fromState,
                toState,
                description,
                "{}",
                null
        );

        workflowProvider.createWorkflowEvent(event);

        return CustomerWorkflowActionResult.success(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                toState,
                description
        );
    }

    private void updateCustomerStatus(
            Integer customerId,
            CustomerStatus customerStatus
    ) {
        if (customerId == null || customerStatus == null) {
            return;
        }

        boolean updated = customerAccessProvider.setCustomerStatus(
                customerId,
                customerStatus
        );

        if (!updated) {
            log.warn(
                    "Customer status could not be updated from workflow action. customerId={}, customerStatus={}",
                    customerId,
                    customerStatus.getCode()
            );
        }
    }

    public static class CustomerWorkflowActionResult {

        private final boolean success;
        private final String message;
        private final Integer workflowId;
        private final Integer customerId;
        private final CustomerWorkflowState workflowState;

        private CustomerWorkflowActionResult(
                boolean success,
                String message,
                Integer workflowId,
                Integer customerId,
                CustomerWorkflowState workflowState
        ) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.workflowId = workflowId;
            this.customerId = customerId;
            this.workflowState = workflowState;
        }

        public static CustomerWorkflowActionResult success(
                Integer workflowId,
                Integer customerId,
                CustomerWorkflowState workflowState,
                String message
        ) {
            return new CustomerWorkflowActionResult(
                    true,
                    message,
                    workflowId,
                    customerId,
                    workflowState
            );
        }

        public static CustomerWorkflowActionResult failed(String message) {
            return new CustomerWorkflowActionResult(
                    false,
                    message,
                    null,
                    null,
                    null
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Integer getWorkflowId() {
            return workflowId;
        }

        public Integer getCustomerId() {
            return customerId;
        }

        public CustomerWorkflowState getWorkflowState() {
            return workflowState;
        }

        @Override
        public String toString() {
            return "CustomerWorkflowActionResult [success=" + success
                    + ", message=" + message
                    + ", workflowId=" + workflowId
                    + ", customerId=" + customerId
                    + ", workflowState=" + workflowState
                    + "]";
        }
    }
}