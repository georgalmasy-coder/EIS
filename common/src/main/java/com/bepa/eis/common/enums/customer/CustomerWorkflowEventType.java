package com.bepa.eis.common.enums.customer;

public enum CustomerWorkflowEventType {

    CUSTOMER_CREATED(
            "Customer created",
            "A customer has been created and the customer onboarding workflow should start.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    CUSTOMER_UPDATED(
            "Customer updated",
            "Customer master data has been updated.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    CUSTOMER_PAYMENT_INFO_UPDATED(
            "Customer payment info updated",
            "Customer payment information has been updated.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    CUSTOMER_EMAIL_CONFIRMATION_REQUESTED(
            "Customer email confirmation requested",
            "A customer email confirmation has been requested.",
            CustomerWorkflowEventCategory.MAIL
    ),

    CUSTOMER_EMAIL_CONFIRMATION_SENT(
            "Customer email confirmation sent",
            "Customer email confirmation mail has been queued or sent.",
            CustomerWorkflowEventCategory.MAIL
    ),

    CUSTOMER_EMAIL_CONFIRMED(
            "Customer email confirmed",
            "Customer has confirmed the account email address.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    CUSTOMER_EMAIL_CONFIRMATION_EXPIRED(
            "Customer email confirmation expired",
            "Customer did not confirm the account email address before the confirmation deadline.",
            CustomerWorkflowEventCategory.TIMER
    ),

    TRIAL_STARTED(
            "Trial started",
            "Customer trial period has started.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    TRIAL_EXPIRING_SOON(
            "Trial expiring soon",
            "Customer trial period is close to expiry and a reminder should be sent.",
            CustomerWorkflowEventCategory.TIMER
    ),

    TRIAL_EXPIRATION_MAIL_SENT(
            "Trial expiration mail sent",
            "Trial expiration reminder mail has been queued or sent.",
            CustomerWorkflowEventCategory.MAIL
    ),

    TRIAL_EXPIRED(
            "Trial expired",
            "Customer trial period has expired.",
            CustomerWorkflowEventCategory.TIMER
    ),

    SUBSCRIPTION_CONTINUATION_CONFIRMED(
            "Subscription continuation confirmed",
            "Customer has confirmed that the subscription should continue after the trial period.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    SUBSCRIPTION_CONTINUATION_CONFIRMATION_EXPIRED(
            "Subscription continuation confirmation expired",
            "Customer did not confirm continuation of the subscription before the trial ended.",
            CustomerWorkflowEventCategory.TIMER
    ),

    PAYMENT_REQUESTED(
            "Payment requested",
            "A payment request has been created.",
            CustomerWorkflowEventCategory.PAYMENT
    ),

    PAYMENT_SUCCEEDED(
            "Payment succeeded",
            "Payment has been completed successfully.",
            CustomerWorkflowEventCategory.PAYMENT
    ),

    PAYMENT_FAILED(
            "Payment failed",
            "Payment failed or was rejected.",
            CustomerWorkflowEventCategory.PAYMENT
    ),

    PAYMENT_CANCELLED(
            "Payment cancelled",
            "Payment was cancelled.",
            CustomerWorkflowEventCategory.PAYMENT
    ),

    PAYMENT_TIMED_OUT(
            "Payment timed out",
            "Payment was not completed before the payment timeout.",
            CustomerWorkflowEventCategory.PAYMENT
    ),

    PAYMENT_OVERDUE(
            "Payment overdue",
            "Payment has not been received before the due date.",
            CustomerWorkflowEventCategory.TIMER
    ),

    PAYMENT_GRACE_PERIOD_EXPIRED(
            "Payment grace period expired",
            "Payment has not been received before the grace period expired.",
            CustomerWorkflowEventCategory.TIMER
    ),

    SUBSCRIPTION_ACTIVATED(
            "Subscription activated",
            "Customer subscription has been activated.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    SUBSCRIPTION_EXPIRING_SOON(
            "Subscription expiring soon",
            "Customer subscription is close to expiry and a renewal reminder should be sent.",
            CustomerWorkflowEventCategory.TIMER
    ),

    SUBSCRIPTION_EXPIRATION_MAIL_SENT(
            "Subscription expiration mail sent",
            "Subscription expiration reminder mail has been queued or sent.",
            CustomerWorkflowEventCategory.MAIL
    ),

    SUBSCRIPTION_EXPIRED(
            "Subscription expired",
            "Customer subscription has expired.",
            CustomerWorkflowEventCategory.TIMER
    ),

    SUBSCRIPTION_RENEWAL_CONFIRMED(
            "Subscription renewal confirmed",
            "Customer has confirmed renewal of the subscription.",
            CustomerWorkflowEventCategory.CUSTOMER
    ),

    CUSTOMER_SUSPENDED(
            "Customer suspended",
            "Customer has been suspended by the workflow.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    CUSTOMER_SUSPENSION_MAIL_SENT(
            "Customer suspension mail sent",
            "Customer suspension mail has been queued or sent.",
            CustomerWorkflowEventCategory.MAIL
    ),

    CUSTOMER_REACTIVATED(
            "Customer reactivated",
            "Customer has been reactivated.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    CUSTOMER_CANCELLED(
            "Customer cancelled",
            "Customer has been cancelled.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    CUSTOMER_MANUALLY_SUSPENDED(
            "Customer manually suspended",
            "Customer has been manually suspended by an administrator.",
            CustomerWorkflowEventCategory.ADMIN
    ),

    CUSTOMER_MANUALLY_REACTIVATED(
            "Customer manually reactivated",
            "Customer has been manually reactivated by an administrator.",
            CustomerWorkflowEventCategory.ADMIN
    ),

    CUSTOMER_MANUALLY_CANCELLED(
            "Customer manually cancelled",
            "Customer has been manually cancelled by an administrator.",
            CustomerWorkflowEventCategory.ADMIN
    ),

    WORKFLOW_RETRY_REQUESTED(
            "Workflow retry requested",
            "A retry has been requested for the customer workflow.",
            CustomerWorkflowEventCategory.ADMIN
    ),

    WORKFLOW_ERROR(
            "Workflow error",
            "An error occurred while processing the customer workflow.",
            CustomerWorkflowEventCategory.SYSTEM
    ),

    WORKFLOW_MANUAL_ATTENTION_REQUIRED(
            "Workflow manual attention required",
            "The customer workflow requires manual attention.",
            CustomerWorkflowEventCategory.SYSTEM
    );

    private final String label;
    private final String description;
    private final CustomerWorkflowEventCategory category;

    CustomerWorkflowEventType(
            String label,
            String description,
            CustomerWorkflowEventCategory category
    ) {
        this.label = label;
        this.description = description;
        this.category = category;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public CustomerWorkflowEventCategory getCategory() {
        return category;
    }

    public boolean isCustomerEvent() {
        return category == CustomerWorkflowEventCategory.CUSTOMER;
    }

    public boolean isTimerEvent() {
        return category == CustomerWorkflowEventCategory.TIMER;
    }

    public boolean isPaymentEvent() {
        return category == CustomerWorkflowEventCategory.PAYMENT;
    }

    public boolean isMailEvent() {
        return category == CustomerWorkflowEventCategory.MAIL;
    }

    public boolean isAdminEvent() {
        return category == CustomerWorkflowEventCategory.ADMIN;
    }

    public boolean isSystemEvent() {
        return category == CustomerWorkflowEventCategory.SYSTEM;
    }

    public static CustomerWorkflowEventType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerWorkflowEventType eventType : values()) {
            if (eventType.name().equalsIgnoreCase(normalizedCode)) {
                return eventType;
            }
        }

        return null;
    }

    public enum CustomerWorkflowEventCategory {
        CUSTOMER,
        TIMER,
        PAYMENT,
        MAIL,
        ADMIN,
        SYSTEM
    }
}