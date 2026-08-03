package com.bepa.eis.common.dto.customer;

import com.bepa.eis.common.enums.customer.CustomerModuleStatus;

import java.sql.Timestamp;

public class CustomerModule {

    private Integer customerModuleId;

    private Integer customerId;
    private Integer subscriptionPlanId;
    private Integer subscriptionPlanBillingPeriodId;

    private String moduleCode;
    private String moduleName;

    private CustomerModuleStatus customerModuleStatus;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private Boolean latest;

    public CustomerModule() {
        customerModuleId = null;

        customerId = null;
        subscriptionPlanId = null;
        subscriptionPlanBillingPeriodId = null;

        moduleCode = "";
        moduleName = "";

        customerModuleStatus = CustomerModuleStatus.ACTIVE;

        createdAt = null;
        updatedAt = null;

        latest = true;
    }

    public Integer getCustomerModuleId() {
        return customerModuleId;
    }

    public void setCustomerModuleId(Integer customerModuleId) {
        this.customerModuleId = customerModuleId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(Integer subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public Integer getSubscriptionPlanBillingPeriodId() {
        return subscriptionPlanBillingPeriodId;
    }

    public void setSubscriptionPlanBillingPeriodId(Integer subscriptionPlanBillingPeriodId) {
        this.subscriptionPlanBillingPeriodId = subscriptionPlanBillingPeriodId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = safeText(moduleCode).toUpperCase();
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = safeText(moduleName);
    }

    public CustomerModuleStatus getCustomerModuleStatus() {
        return customerModuleStatus == null
                ? CustomerModuleStatus.ACTIVE
                : customerModuleStatus;
    }

    public void setCustomerModuleStatus(CustomerModuleStatus customerModuleStatus) {
        this.customerModuleStatus = customerModuleStatus == null
                ? CustomerModuleStatus.ACTIVE
                : customerModuleStatus;
    }

    public Integer getCustomerModuleStatusId() {
        return getCustomerModuleStatus().getId();
    }

    public void setCustomerModuleStatusId(Integer customerModuleStatusId) {
        this.customerModuleStatus = CustomerModuleStatus.fromIdOrDefault(
                customerModuleStatusId,
                CustomerModuleStatus.ACTIVE
        );
    }

    public String getCustomerModuleStatusCode() {
        return getCustomerModuleStatus().getCode();
    }

    public void setCustomerModuleStatusCode(String customerModuleStatusCode) {
        this.customerModuleStatus = CustomerModuleStatus.fromCodeOrDefault(
                customerModuleStatusCode,
                CustomerModuleStatus.ACTIVE
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
        return customerModuleId == null;
    }

    public boolean isLatest() {
        return latest != null && latest;
    }

    public boolean isActive() {
        return getCustomerModuleStatus().isActive();
    }

    public boolean isTrial() {
        return getCustomerModuleStatus().isTrial();
    }

    public boolean isSuspended() {
        return getCustomerModuleStatus().isSuspended();
    }

    public boolean isCancelled() {
        return getCustomerModuleStatus().isCancelled();
    }

    public boolean isExpired() {
        return getCustomerModuleStatus().isExpired();
    }

    public boolean isAccessAllowedByDefault() {
        return getCustomerModuleStatus().isAccessAllowedByDefault();
    }

    public boolean hasCustomerId() {
        return customerId != null && customerId > 0;
    }

    public boolean hasSubscriptionPlanId() {
        return subscriptionPlanId != null && subscriptionPlanId > 0;
    }

    public boolean hasModuleCode() {
        return moduleCode != null && !moduleCode.trim().isEmpty();
    }

    public String getDisplayName() {
        if (moduleName != null && !moduleName.trim().isEmpty()) {
            return moduleName.trim();
        }

        if (moduleCode != null && !moduleCode.trim().isEmpty()) {
            return moduleCode.trim();
        }

        return "";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "CustomerModule [customerModuleId=" + customerModuleId
                + ", customerId=" + customerId
                + ", subscriptionPlanId=" + subscriptionPlanId
                + ", subscriptionPlanBillingPeriodId=" + subscriptionPlanBillingPeriodId
                + ", moduleCode=" + moduleCode
                + ", moduleName=" + moduleName
                + ", customerModuleStatus=" + getCustomerModuleStatusCode()
                + ", latest=" + latest
                + "]";
    }
}
