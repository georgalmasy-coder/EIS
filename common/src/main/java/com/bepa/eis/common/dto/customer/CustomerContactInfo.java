package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerStatus;

public class CustomerContactInfo {

    private Integer customerId;
    private String customerName;
    private String contactName;
    private String contactEmail;

    private CustomerStatus customerStatus;

    public CustomerContactInfo() {
        customerId = null;
        customerName = "";
        contactName = "";
        contactEmail = "";
        customerStatus = CustomerStatus.CREATED;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = safeText(customerName);
    }

    public String getContactName() {
        if (contactName == null || contactName.trim().isEmpty()) {
            return customerName == null ? "" : customerName.trim();
        }

        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = safeText(contactName);
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = safeText(contactEmail).toLowerCase();
    }

    public CustomerStatus getCustomerStatus() {
        return customerStatus == null ? CustomerStatus.CREATED : customerStatus;
    }

    public void setCustomerStatus(CustomerStatus customerStatus) {
        this.customerStatus = customerStatus == null ? CustomerStatus.CREATED : customerStatus;
    }

    public Integer getCustomerStatusId() {
        return getCustomerStatus().getId();
    }

    public void setCustomerStatusId(Integer customerStatusId) {
        this.customerStatus = CustomerStatus.fromIdOrDefault(
                customerStatusId,
                CustomerStatus.CREATED
        );
    }

    public String getCustomerStatusCode() {
        return getCustomerStatus().getCode();
    }

    public void setCustomerStatusCode(String customerStatusCode) {
        this.customerStatus = CustomerStatus.fromCodeOrDefault(
                customerStatusCode,
                CustomerStatus.CREATED
        );
    }

    public String getStatus() {
        return getCustomerStatusCode();
    }

    public void setStatus(String status) {
        setCustomerStatusCode(status);
    }

    public boolean isActive() {
        return getCustomerStatus().isLoginAllowedByDefault();
    }

    public void setActive(boolean active) {
        /*
         * Kept for backward compatibility.
         * CustomerStatus is now the source of truth.
         */
        if (!active && !isSuspended() && !isCancelled()) {
            customerStatus = CustomerStatus.CANCELLED;
        }
    }

    public void setActive(Boolean active) {
        setActive(active == null || active);
    }

    public boolean isSuspended() {
        return getCustomerStatus().isSuspended();
    }

    public void setSuspended(boolean suspended) {
        /*
         * Kept for backward compatibility.
         * CustomerStatus is now the source of truth.
         */
        if (suspended) {
            customerStatus = CustomerStatus.SUSPENDED;
        }
    }

    public void setSuspended(Boolean suspended) {
        setSuspended(suspended != null && suspended);
    }

    public boolean isCancelled() {
        return getCustomerStatus().isCancelled();
    }

    public boolean hasCustomerId() {
        return customerId != null;
    }

    public boolean hasCustomerName() {
        return customerName != null && !customerName.trim().isEmpty();
    }

    public boolean hasContactName() {
        return getContactName() != null && !getContactName().trim().isEmpty();
    }

    public boolean hasContactEmail() {
        return contactEmail != null && !contactEmail.trim().isEmpty();
    }

    public boolean isValidMailRecipient() {
        return hasContactEmail()
                && contactEmail.contains("@")
                && contactEmail.indexOf("@") > 0
                && contactEmail.indexOf("@") < contactEmail.length() - 1;
    }

    public boolean canReceiveWorkflowMail() {
        return hasCustomerId()
                && !isCancelled()
                && isValidMailRecipient();
    }

    public boolean isLoginAllowedByDefault() {
        return getCustomerStatus().isLoginAllowedByDefault();
    }

    public String getDisplayName() {
        if (hasContactName()) {
            return getContactName();
        }

        if (hasCustomerName()) {
            return getCustomerName();
        }

        if (customerId != null) {
            return "Customer " + customerId;
        }

        return "Customer";
    }

    public String getSafeCustomerName() {
        if (hasCustomerName()) {
            return customerName;
        }

        if (customerId != null) {
            return "Customer " + customerId;
        }

        return "Customer";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerContactInfo [customerId=" + customerId
                + ", customerName=" + customerName
                + ", contactName=" + contactName
                + ", contactEmail=" + contactEmail
                + ", customerStatus=" + getCustomerStatusCode()
                + "]";
    }
}