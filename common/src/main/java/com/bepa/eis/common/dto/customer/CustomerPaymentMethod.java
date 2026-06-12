package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerPaymentMethodStatus;

import java.sql.Timestamp;

public class CustomerPaymentMethod {

    private Integer customerPaymentMethodId;
    private Integer customerId;

    private String paymentProvider;
    private String providerPaymentMethodReference;

    private String cardholderName;
    private String cardBrand;
    private String maskedCardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String billingZipCode;

    private CustomerPaymentMethodStatus paymentMethodStatus;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private Boolean latest;

    public CustomerPaymentMethod() {
        customerPaymentMethodId = null;
        customerId = null;

        paymentProvider = "";
        providerPaymentMethodReference = "";

        cardholderName = "";
        cardBrand = "";
        maskedCardNumber = "";
        expiryMonth = null;
        expiryYear = null;
        billingZipCode = "";

        paymentMethodStatus = CustomerPaymentMethodStatus.ACTIVE;

        createdAt = null;
        updatedAt = null;

        latest = true;
    }

    public Integer getCustomerPaymentMethodId() {
        return customerPaymentMethodId;
    }

    public void setCustomerPaymentMethodId(Integer customerPaymentMethodId) {
        this.customerPaymentMethodId = customerPaymentMethodId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = safeText(paymentProvider);
    }

    public String getProviderPaymentMethodReference() {
        return providerPaymentMethodReference;
    }

    public void setProviderPaymentMethodReference(String providerPaymentMethodReference) {
        this.providerPaymentMethodReference = safeText(providerPaymentMethodReference);
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = safeText(cardholderName);
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = safeText(cardBrand);
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = safeText(maskedCardNumber);
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        if (expiryMonth == null || expiryMonth < 1 || expiryMonth > 12) {
            this.expiryMonth = null;
            return;
        }

        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        if (expiryYear == null || expiryYear < 2000) {
            this.expiryYear = null;
            return;
        }

        this.expiryYear = expiryYear;
    }

    public String getBillingZipCode() {
        return billingZipCode;
    }

    public void setBillingZipCode(String billingZipCode) {
        this.billingZipCode = safeText(billingZipCode);
    }

    public CustomerPaymentMethodStatus getPaymentMethodStatus() {
        return paymentMethodStatus == null
                ? CustomerPaymentMethodStatus.ACTIVE
                : paymentMethodStatus;
    }

    public void setPaymentMethodStatus(CustomerPaymentMethodStatus paymentMethodStatus) {
        this.paymentMethodStatus = paymentMethodStatus == null
                ? CustomerPaymentMethodStatus.ACTIVE
                : paymentMethodStatus;
    }

    public Integer getPaymentMethodStatusId() {
        return getPaymentMethodStatus().getId();
    }

    public void setPaymentMethodStatusId(Integer paymentMethodStatusId) {
        this.paymentMethodStatus = CustomerPaymentMethodStatus.fromIdOrDefault(
                paymentMethodStatusId,
                CustomerPaymentMethodStatus.ACTIVE
        );
    }

    public String getPaymentMethodStatusCode() {
        return getPaymentMethodStatus().getCode();
    }

    public void setPaymentMethodStatusCode(String paymentMethodStatusCode) {
        this.paymentMethodStatus = CustomerPaymentMethodStatus.fromCodeOrDefault(
                paymentMethodStatusCode,
                CustomerPaymentMethodStatus.ACTIVE
        );
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getLatest() {
        return latest != null && latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest != null && latest;
    }

    public boolean isNew() {
        return customerPaymentMethodId == null;
    }

    public boolean isLatest() {
        return latest != null && latest;
    }

    public boolean isActive() {
        return getPaymentMethodStatus().isActive();
    }

    public boolean isExpired() {
        return getPaymentMethodStatus().isExpired();
    }

    public boolean isDisabled() {
        return getPaymentMethodStatus().isDisabled();
    }

    public boolean isDeleted() {
        return getPaymentMethodStatus().isDeleted();
    }

    public boolean canBeUsedForPayment() {
        return getPaymentMethodStatus().canBeUsedForPayment();
    }

    public boolean hasProviderReference() {
        return providerPaymentMethodReference != null && !providerPaymentMethodReference.trim().isEmpty();
    }

    public boolean hasCustomerId() {
        return customerId != null && customerId > 0;
    }

    public boolean hasValidPaymentProvider() {
        return paymentProvider != null && !paymentProvider.trim().isEmpty();
    }

    public String getSafeMaskedCardNumber() {
        if (maskedCardNumber != null && !maskedCardNumber.trim().isEmpty()) {
            return maskedCardNumber.trim();
        }

        return "";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerPaymentMethod [customerPaymentMethodId=" + customerPaymentMethodId
                + ", customerId=" + customerId
                + ", paymentProvider=" + paymentProvider
                + ", providerPaymentMethodReference=" + providerPaymentMethodReference
                + ", cardholderName=" + cardholderName
                + ", cardBrand=" + cardBrand
                + ", maskedCardNumber=" + maskedCardNumber
                + ", expiryMonth=" + expiryMonth
                + ", expiryYear=" + expiryYear
                + ", billingZipCode=" + billingZipCode
                + ", paymentMethodStatus=" + getPaymentMethodStatusCode()
                + ", latest=" + latest
                + "]";
    }
}