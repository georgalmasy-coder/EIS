package com.bepa.eis.common.enums.mail;

/*
/opt/eis/conf/eis/mail-templates/password-reset.mail
mail.template.folder=eis/mail-templates
 */
public enum MailTemplateType {

    PASSWORD_RESET("password-reset.mail", "Password Reset"),
    MFA_RESET("mfa-reset.mail", "MFA Reset"),
    USER_CREATED("user-created.mail", "User Created"),
    USER_INVITED("user-invited.mail", "User Invited"),
    REQUIREMENT_ASSIGNED("requirement-assigned.mail", "Requirement Assigned"),
    REQUIREMENT_CHANGED("requirement-changed.mail", "Requirement Changed"),
    SYSTEM_NOTIFICATION("system-notification.mail", "System Notification"),

    CUSTOMER_CONFIRMATION("customer-confirmation.mail", "Customer Confirmation"),
    TRIAL_EXPIRING("trial-expiring.mail", "Trial Expiring"),
    SUBSCRIPTION_CONTINUATION_CONFIRMATION("subscription-continuation-confirmation.mail", "Subscription Continuation Confirmation"),
    PAYMENT_REQUESTED("payment-requested.mail", "Payment Requested"),
    PAYMENT_FAILED("payment-failed.mail", "Payment Failed"),
    PAYMENT_OVERDUE("payment-overdue.mail", "Payment Overdue"),
    SUBSCRIPTION_EXPIRING("subscription-expiring.mail", "Subscription Expiring"),
    SUBSCRIPTION_RENEWAL_CONFIRMATION("subscription-renewal-confirmation.mail", "Subscription Renewal Confirmation"),
    CUSTOMER_SUSPENDED("customer-suspended.mail", "Customer Suspended"),
    CUSTOMER_REACTIVATED("customer-reactivated.mail", "Customer Reactivated");

    private final String fileName;
    private final String description;

    MailTemplateType(String fileName, String description) {
        this.fileName = fileName;
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCustomerWorkflowTemplate() {
        return this == CUSTOMER_CONFIRMATION
                || this == TRIAL_EXPIRING
                || this == SUBSCRIPTION_CONTINUATION_CONFIRMATION
                || this == PAYMENT_REQUESTED
                || this == PAYMENT_FAILED
                || this == PAYMENT_OVERDUE
                || this == SUBSCRIPTION_EXPIRING
                || this == SUBSCRIPTION_RENEWAL_CONFIRMATION
                || this == CUSTOMER_SUSPENDED
                || this == CUSTOMER_REACTIVATED;
    }

    public static MailTemplateType mapToType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SYSTEM_NOTIFICATION;
        }

        String normalizedValue = value.trim();

        for (MailTemplateType templateType : MailTemplateType.values()) {
            if (templateType.name().equalsIgnoreCase(normalizedValue)
                    || templateType.fileName.equalsIgnoreCase(normalizedValue)
                    || templateType.description.equalsIgnoreCase(normalizedValue)) {
                return templateType;
            }
        }

        return SYSTEM_NOTIFICATION;
    }
}