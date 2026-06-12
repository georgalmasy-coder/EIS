package com.bepa.eis.common.enums.customer;

import com.bepa.eis.common.enums.mail.MailTemplateType;

public enum CustomerWorkflowMailType {

    CUSTOMER_CONFIRMATION(
            "Customer confirmation",
            "Mail asking the customer to confirm account creation.",
            "customer-confirmation.mail",
            MailTemplateType.CUSTOMER_CONFIRMATION
    ),

    TRIAL_EXPIRING(
            "Trial expiring",
            "Mail informing the customer that the trial period is close to expiry.",
            "trial-expiring.mail",
            MailTemplateType.TRIAL_EXPIRING
    ),

    SUBSCRIPTION_CONTINUATION_CONFIRMATION(
            "Subscription continuation confirmation",
            "Mail asking the customer to confirm continuation of the subscription after the trial period.",
            "subscription-continuation-confirmation.mail",
            MailTemplateType.SUBSCRIPTION_CONTINUATION_CONFIRMATION
    ),

    PAYMENT_REQUESTED(
            "Payment requested",
            "Mail informing the customer that payment has been requested.",
            "payment-requested.mail",
            MailTemplateType.PAYMENT_REQUESTED
    ),

    PAYMENT_FAILED(
            "Payment failed",
            "Mail informing the customer that payment failed or could not be completed.",
            "payment-failed.mail",
            MailTemplateType.PAYMENT_FAILED
    ),

    PAYMENT_OVERDUE(
            "Payment overdue",
            "Mail informing the customer that payment is overdue.",
            "payment-overdue.mail",
            MailTemplateType.PAYMENT_OVERDUE
    ),

    SUBSCRIPTION_EXPIRING(
            "Subscription expiring",
            "Mail informing the customer that the subscription is close to expiry.",
            "subscription-expiring.mail",
            MailTemplateType.SUBSCRIPTION_EXPIRING
    ),

    SUBSCRIPTION_RENEWAL_CONFIRMATION(
            "Subscription renewal confirmation",
            "Mail asking the customer to confirm renewal of the subscription.",
            "subscription-renewal-confirmation.mail",
            MailTemplateType.SUBSCRIPTION_RENEWAL_CONFIRMATION
    ),

    CUSTOMER_SUSPENDED(
            "Customer suspended",
            "Mail informing the customer that the account has been suspended.",
            "customer-suspended.mail",
            MailTemplateType.CUSTOMER_SUSPENDED
    ),

    CUSTOMER_REACTIVATED(
            "Customer reactivated",
            "Mail informing the customer that the account has been reactivated.",
            "customer-reactivated.mail",
            MailTemplateType.CUSTOMER_REACTIVATED
    );

    private final String label;
    private final String description;
    private final String templateName;
    private final MailTemplateType mailTemplateType;

    CustomerWorkflowMailType(
            String label,
            String description,
            String templateName,
            MailTemplateType mailTemplateType
    ) {
        this.label = label;
        this.description = description;
        this.templateName = templateName;
        this.mailTemplateType = mailTemplateType == null
                ? MailTemplateType.SYSTEM_NOTIFICATION
                : mailTemplateType;
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

    public String getTemplateName() {
        return templateName;
    }

    public MailTemplateType getMailTemplateType() {
        return mailTemplateType;
    }

    public String getDeduplicationPrefix() {
        return "CUSTOMER_WORKFLOW_" + name();
    }

    public boolean requiresConfirmationLink() {
        return this == CUSTOMER_CONFIRMATION
                || this == SUBSCRIPTION_CONTINUATION_CONFIRMATION
                || this == SUBSCRIPTION_RENEWAL_CONFIRMATION;
    }

    public boolean requiresPaymentLink() {
        return this == PAYMENT_REQUESTED
                || this == PAYMENT_FAILED
                || this == PAYMENT_OVERDUE;
    }

    public boolean isReminderMail() {
        return this == TRIAL_EXPIRING
                || this == SUBSCRIPTION_EXPIRING
                || this == PAYMENT_OVERDUE;
    }

    public boolean isAccountStatusMail() {
        return this == CUSTOMER_SUSPENDED
                || this == CUSTOMER_REACTIVATED;
    }

    public static CustomerWorkflowMailType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();

        for (CustomerWorkflowMailType mailType : values()) {
            if (mailType.name().equalsIgnoreCase(normalizedCode)) {
                return mailType;
            }
        }

        return null;
    }

    public static CustomerWorkflowMailType fromTemplateName(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            return null;
        }

        String normalizedTemplateName = templateName.trim();

        for (CustomerWorkflowMailType mailType : values()) {
            if (mailType.templateName.equalsIgnoreCase(normalizedTemplateName)) {
                return mailType;
            }
        }

        return null;
    }

    public static CustomerWorkflowMailType fromMailTemplateType(MailTemplateType mailTemplateType) {
        if (mailTemplateType == null) {
            return null;
        }

        for (CustomerWorkflowMailType mailType : values()) {
            if (mailType.mailTemplateType == mailTemplateType) {
                return mailType;
            }
        }

        return null;
    }
}