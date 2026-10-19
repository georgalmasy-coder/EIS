package com.bepa.eis.server.api.security;

import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.providers.security.MfaConfig;
import java.util.List;
import java.util.function.IntFunction;

/** Resolve login policy across all accessible customers before one is selected. */
final class CustomerMfaPolicyResolver {
    private CustomerMfaPolicyResolver() { }

    static MfaConfig.CustomerMfaPolicy resolve(List<CustomerRecord> customers, IntFunction<String> policyLookup) {
        if (customers == null || customers.isEmpty()) return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        boolean hasOptional = false;
        boolean hasDisabled = false;
        for (CustomerRecord customer : customers) {
            if (customer == null || customer.getCustomerId() == null) continue;
            var policy = MfaConfig.parseCustomerMfaPolicy(policyLookup.apply(customer.getCustomerId()));
            if (policy == MfaConfig.CustomerMfaPolicy.REQUIRED) return policy;
            hasOptional |= policy == MfaConfig.CustomerMfaPolicy.OPTIONAL;
            hasDisabled |= policy == MfaConfig.CustomerMfaPolicy.DISABLED;
        }
        if (hasOptional) return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        return hasDisabled ? MfaConfig.CustomerMfaPolicy.DISABLED : MfaConfig.CustomerMfaPolicy.OPTIONAL;
    }
}
