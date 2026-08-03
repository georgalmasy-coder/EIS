package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerStatus;

import java.sql.Timestamp;

public class CustomerRecord {

    public static final String DEFAULT_CUSTOMER_MFA_POLICY = "OPTIONAL";

    private Integer customerPK;
    private Integer customerId;
    private Integer version;

    private String customerName;
    private String cvrNumber;
    private String vatNumber;
    private String phone;

    private String address;
    private String zipCode;
    private String city;
    private String country;

    private String contactName;
    private String contactEmail;

    private CustomerStatus customerStatus;
    private String customerMfaPolicy;

    private Integer changedByUserId;
    private Timestamp changedDateTime;
    private Timestamp createdDateTime;

    private Boolean latest;

    public CustomerRecord() {
        customerPK = null;
        customerId = null;
        version = 1;

        customerName = "";
        cvrNumber = "";
        vatNumber = "";
        phone = "";

        address = "";
        zipCode = "";
        city = "";
        country = "";

        contactName = "";
        contactEmail = "";

        customerStatus = CustomerStatus.CREATED;
        customerMfaPolicy = DEFAULT_CUSTOMER_MFA_POLICY;

        changedByUserId = null;
        changedDateTime = null;
        createdDateTime = null;

        latest = true;
    }

    public Integer getCustomerPK() {
        return customerPK;
    }

    public void setCustomerPK(Integer customerPK) {
        this.customerPK = customerPK;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getVersion() {
        return version == null ? 1 : version;
    }

    public void setVersion(Integer version) {
        this.version = version == null || version < 1 ? 1 : version;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = safeText(customerName);
    }

    public String getCvrNumber() {
        return cvrNumber;
    }

    public void setCvrNumber(String cvrNumber) {
        this.cvrNumber = safeText(cvrNumber);
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = safeText(vatNumber).toUpperCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = safeText(phone);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = safeText(address);
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = safeText(zipCode);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = safeText(city);
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = safeText(country);
    }

    public String getContactName() {
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
        return customerStatus;
    }

    public void setCustomerStatus(CustomerStatus customerStatus) {
        this.customerStatus = customerStatus == null ? CustomerStatus.CREATED : customerStatus;
    }

    public Integer getCustomerStatusId() {
        return customerStatus == null ? CustomerStatus.CREATED.getId() : customerStatus.getId();
    }

    public void setCustomerStatusId(Integer customerStatusId) {
        this.customerStatus = CustomerStatus.fromIdOrDefault(
                customerStatusId,
                CustomerStatus.CREATED
        );
    }

    public String getCustomerStatusCode() {
        return customerStatus == null ? CustomerStatus.CREATED.getCode() : customerStatus.getCode();
    }

    public String getCustomerMfaPolicy() {
        if (customerMfaPolicy == null || customerMfaPolicy.isBlank()) {
            return DEFAULT_CUSTOMER_MFA_POLICY;
        }

        return customerMfaPolicy;
    }

    public void setCustomerMfaPolicy(String customerMfaPolicy) {
        String safeCustomerMfaPolicy = safeText(customerMfaPolicy).toUpperCase();

        if (safeCustomerMfaPolicy.isBlank()) {
            this.customerMfaPolicy = DEFAULT_CUSTOMER_MFA_POLICY;
            return;
        }

        this.customerMfaPolicy = safeCustomerMfaPolicy;
    }

    public Integer getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Integer changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public Timestamp getChangedDateTime() {
        return changedDateTime;
    }

    public void setChangedDateTime(Timestamp changedDateTime) {
        this.changedDateTime = changedDateTime;
    }

    public Timestamp getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Timestamp createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Boolean getLatest() {
        return latest != null && latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest != null && latest;
    }

    public boolean isNew() {
        return customerPK == null;
    }

    public boolean isLatest() {
        return latest != null && latest;
    }

    public boolean isLoginAllowedByDefault() {
        return customerStatus != null && customerStatus.isLoginAllowedByDefault();
    }

    public boolean isSuspended() {
        return customerStatus != null && customerStatus.isSuspended();
    }

    public boolean isCancelled() {
        return customerStatus != null && customerStatus.isCancelled();
    }

    public boolean hasValidCustomerId() {
        return customerId != null && customerId > 0;
    }

    public boolean hasValidVersion() {
        return version != null && version > 0;
    }

    public boolean hasCustomerName() {
        return customerName != null && !customerName.trim().isEmpty();
    }

    public boolean hasContactEmail() {
        return contactEmail != null && !contactEmail.trim().isEmpty();
    }

    public String getSafeContactName() {
        if (contactName != null && !contactName.trim().isEmpty()) {
            return contactName.trim();
        }

        if (customerName != null && !customerName.trim().isEmpty()) {
            return customerName.trim();
        }

        return "Customer";
    }

    public CustomerContactInfo toContactInfo() {
        CustomerContactInfo contactInfo = new CustomerContactInfo();

        contactInfo.setCustomerId(customerId);
        contactInfo.setCustomerName(customerName);
        contactInfo.setContactName(getSafeContactName());
        contactInfo.setContactEmail(contactEmail);
        contactInfo.setCustomerStatus(customerStatus == null ? CustomerStatus.CREATED : customerStatus);

        return contactInfo;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerRecord [customerPK=" + customerPK
                + ", customerId=" + customerId
                + ", version=" + version
                + ", customerName=" + customerName
                + ", cvrNumber=" + cvrNumber
                + ", vatNumber=" + vatNumber
                + ", contactEmail=" + contactEmail
                + ", customerStatus=" + getCustomerStatusCode()
                + ", customerMfaPolicy=" + getCustomerMfaPolicy()
                + ", changedByUserId=" + changedByUserId
                + ", changedDateTime=" + changedDateTime
                + ", createdDateTime=" + createdDateTime
                + ", latest=" + latest
                + "]";
    }
}
