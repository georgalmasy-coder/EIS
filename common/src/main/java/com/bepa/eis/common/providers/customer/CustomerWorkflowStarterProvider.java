package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

public class CustomerWorkflowStarterProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerWorkflowStarterProvider.class);

    private final CustomerWorkflowProvider workflowProvider;
    private final CustomerWorkflowTimingProvider timingProvider;
    private final CustomerRecordProvider customerRecordProvider;

    public CustomerWorkflowStarterProvider() {
        this(null);
    }

    public CustomerWorkflowStarterProvider(WebSession webSession) {
        this(
                new CustomerWorkflowProvider(webSession),
                new CustomerWorkflowTimingProvider(),
                new CustomerRecordProvider(webSession)
        );
    }

    public CustomerWorkflowStarterProvider(
            CustomerWorkflowProvider workflowProvider,
            CustomerWorkflowTimingProvider timingProvider,
            CustomerRecordProvider customerRecordProvider
    ) {
        this.workflowProvider = workflowProvider == null
                ? new CustomerWorkflowProvider(null)
                : workflowProvider;

        this.timingProvider = timingProvider == null
                ? new CustomerWorkflowTimingProvider()
                : timingProvider;

        this.customerRecordProvider = customerRecordProvider == null
                ? new CustomerRecordProvider(null)
                : customerRecordProvider;
    }

    public Integer startCustomerOnboardingWorkflow(Integer customerId) {
        return startCustomerOnboardingWorkflow(
                customerId,
                null,
                "Customer onboarding workflow started."
        );
    }

    public Integer startCustomerOnboardingWorkflow(
            Integer customerId,
            Integer createdByUserId
    ) {
        return startCustomerOnboardingWorkflow(
                customerId,
                createdByUserId,
                "Customer onboarding workflow started."
        );
    }

    public Integer startCustomerOnboardingWorkflow(
            Integer customerId,
            Integer createdByUserId,
            String description
    ) {
        if (customerId == null) {
            log.warn("Customer workflow could not be started because customerId is missing.");
            return null;
        }

        CustomerWorkflow existingWorkflow = workflowProvider.getActiveWorkflowByCustomerId(customerId);

        if (existingWorkflow != null && existingWorkflow.getWorkflowId() != null) {
            log.info(
                    "Customer already has an active workflow. customerId={}, workflowId={}",
                    customerId,
                    existingWorkflow.getWorkflowId()
            );

            return existingWorkflow.getWorkflowId();
        }

        customerRecordProvider.updateCustomerStatus(
                customerId,
                CustomerStatus.PENDING_CONFIRMATION,
                createdByUserId
        );

        CustomerWorkflow workflow = createInitialWorkflow(customerId);
        Integer workflowId = workflowProvider.createWorkflow(workflow);

        if (workflowId == null) {
            log.warn("Customer workflow could not be created. customerId={}", customerId);
            return null;
        }

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflowId,
                customerId,
                CustomerWorkflowEventType.CUSTOMER_CREATED,
                null,
                CustomerWorkflowState.CREATED,
                safeText(description, "Customer onboarding workflow started."),
                "{}",
                createdByUserId
        );

        workflowProvider.createWorkflowEvent(event);

        log.info(
                "Customer onboarding workflow created. customerId={}, workflowId={}",
                customerId,
                workflowId
        );

        return workflowId;
    }

    public boolean hasActiveCustomerWorkflow(Integer customerId) {
        if (customerId == null) {
            return false;
        }

        CustomerWorkflow workflow = workflowProvider.getActiveWorkflowByCustomerId(customerId);

        return workflow != null && workflow.getWorkflowId() != null;
    }

    private CustomerWorkflow createInitialWorkflow(Integer customerId) {
        CustomerWorkflow workflow = new CustomerWorkflow();

        workflow.setCustomerId(customerId);
        workflow.setWorkflowType("CUSTOMER_ONBOARDING");
        workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
        workflow.setCurrentState(CustomerWorkflowState.CREATED);
        workflow.setSubscriptionId(null);
        workflow.setPaymentId(null);
        workflow.setRetryCount(0);
        workflow.setLastEventType(CustomerWorkflowEventType.CUSTOMER_CREATED.getCode());
        workflow.setLastEventAt(new Timestamp(System.currentTimeMillis()));
        workflow.setLastError("");

        /*
         * The workflow is due immediately. The integration server job will pick it up
         * and move it from CREATED to PENDING_EMAIL_CONFIRMATION.
         */
        workflow.setNextActionAt(timingProvider.now());

        return workflow;
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
}