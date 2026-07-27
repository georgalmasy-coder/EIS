package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.enums.customer.Subscription;

public class CustomerBasisInfo {

    private final Integer customerId;
    private final String customerName;
    private final Subscription customerSubscription;

    public CustomerBasisInfo(Integer customerId) {
        this(customerId, "", null);
    }

    public CustomerBasisInfo(
            Integer customerId,
            String customerName,
            Subscription customerSubscription
    ) {
        this.customerId = customerId;
        this.customerName = customerName == null ? "" : customerName.trim();
        this.customerSubscription = customerSubscription;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Subscription getCustomerSubscription() {
        return customerSubscription;
    }
}
