package com.bepa.eis.server.api.security;

import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.providers.security.MfaConfig;
import com.bepa.eis.server.api.web.application.settings.CustomerSettings;
import java.util.List;
import java.util.Map;

public final class CustomerMfaPolicyResolverTest {
    public static void main(String[] args) {
        CustomerRecord first = new CustomerRecord(); first.setCustomerId(1);
        CustomerRecord second = new CustomerRecord(); second.setCustomerId(2);
        first.setCustomerMfaPolicy("REQUIRED"); // The legacy column must not decide login anymore.
        var empty = new CustomerSettings(null);
        check(CustomerMfaPolicyResolver.resolve(List.of(first), empty::getCustomerMfaPolicy), "OPTIONAL");
        var disabled = empty.update(1, null, Map.of("mfaPolicy", "DISABLED"));
        check(CustomerMfaPolicyResolver.resolve(List.of(first), disabled::getCustomerMfaPolicy), "DISABLED");
        check(CustomerMfaPolicyResolver.resolve(List.of(first, second), disabled::getCustomerMfaPolicy), "OPTIONAL");
        var required = disabled.update(2, null, Map.of("mfaPolicy", "REQUIRED"));
        check(CustomerMfaPolicyResolver.resolve(List.of(first, second), required::getCustomerMfaPolicy), "REQUIRED");
        check(CustomerMfaPolicyResolver.resolve(List.of(second, first), required::getCustomerMfaPolicy), "REQUIRED");
        check(CustomerMfaPolicyResolver.resolve(List.of(), required::getCustomerMfaPolicy), "OPTIONAL");
        try {
            CustomerMfaPolicyResolver.resolve(List.of(first), id -> { throw new IllegalStateException("Database unavailable"); });
            throw new AssertionError("Settings load failure must not silently disable required MFA");
        } catch (IllegalStateException expected) { }
        System.out.println("Customer MFA policy regression checks passed");
    }

    private static void check(MfaConfig.CustomerMfaPolicy policy, String expected) {
        if (!policy.name().equals(expected)) throw new AssertionError("Expected " + expected + ", got " + policy);
    }
}
