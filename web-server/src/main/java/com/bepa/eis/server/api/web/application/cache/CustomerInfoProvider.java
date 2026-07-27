package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.enums.customer.Subscription;
import com.bepa.eis.common.providers.customer.CustomerModuleProvider;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;

import java.util.Comparator;
import java.util.List;

public class CustomerInfoProvider {

    private final CustomerRecordProvider customerRecordProvider;
    private final CustomerSubscriptionProvider customerSubscriptionProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;
    private final CustomerModuleProvider customerModuleProvider;

    public CustomerInfoProvider() {
        this(
                new CustomerRecordProvider(null),
                new CustomerSubscriptionProvider(null),
                new SubscriptionPlanProvider(null),
                new CustomerModuleProvider(null)
        );
    }

    public CustomerInfoProvider(
            CustomerRecordProvider customerRecordProvider,
            CustomerSubscriptionProvider customerSubscriptionProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            CustomerModuleProvider customerModuleProvider
    ) {
        this.customerRecordProvider = customerRecordProvider == null
                ? new CustomerRecordProvider(null)
                : customerRecordProvider;
        this.customerSubscriptionProvider = customerSubscriptionProvider == null
                ? new CustomerSubscriptionProvider(null)
                : customerSubscriptionProvider;
        this.subscriptionPlanProvider = subscriptionPlanProvider == null
                ? new SubscriptionPlanProvider(null)
                : subscriptionPlanProvider;
        this.customerModuleProvider = customerModuleProvider == null
                ? new CustomerModuleProvider(null)
                : customerModuleProvider;
    }

    public CustomerBasisInfo getCustomerInfo(Integer customerId) {
        if (customerId == null) {
            return new CustomerBasisInfo(null);
        }

        String customerName = resolveCustomerName(customerId);
        Subscription customerSubscription = resolveCustomerSubscription(customerId);

        return new CustomerBasisInfo(
                customerId,
                customerName,
                customerSubscription
        );
    }

    private String resolveCustomerName(Integer customerId) {
        CustomerRecord customerRecord = customerRecordProvider.getLatestCustomerByCustomerId(customerId);

        if (customerRecord == null || customerRecord.getCustomerName() == null) {
            return "";
        }

        return customerRecord.getCustomerName().trim();
    }

    private Subscription resolveCustomerSubscription(Integer customerId) {
        CustomerSubscription latestSubscription = customerSubscriptionProvider.getLatestSubscriptionByCustomerId(customerId);

        Subscription subscription = resolveSubscriptionFromCustomerSubscription(latestSubscription);
        if (subscription != null) {
            return subscription;
        }

        CustomerModule latestCustomerModule = resolveLatestCustomerModule(customerId);
        if (latestCustomerModule == null || latestCustomerModule.getSubscriptionPlanId() == null) {
            return null;
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanProvider.getPlanById(latestCustomerModule.getSubscriptionPlanId());
        if (subscriptionPlan == null) {
            return null;
        }

        return Subscription.fromModuleCode(subscriptionPlan.getModuleCode());
    }

    private Subscription resolveSubscriptionFromCustomerSubscription(CustomerSubscription customerSubscription) {
        if (customerSubscription == null || customerSubscription.getSubscriptionPlanId() == null) {
            return null;
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanProvider.getPlanById(customerSubscription.getSubscriptionPlanId());
        if (subscriptionPlan == null) {
            return null;
        }

        return Subscription.fromModuleCode(subscriptionPlan.getModuleCode());
    }

    private CustomerModule resolveLatestCustomerModule(Integer customerId) {
        List<CustomerModule> modules = customerModuleProvider.getLatestCustomerModules(customerId);

        if (modules == null || modules.isEmpty()) {
            return null;
        }

        return modules.stream()
                .filter(module -> module != null && module.getCustomerModuleId() != null)
                .max(Comparator
                        .comparing(CustomerModule::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CustomerModule::getCustomerModuleId))
                .orElse(modules.get(0));
    }
}
