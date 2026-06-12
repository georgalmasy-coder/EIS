package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerContactInfo;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerContactInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerContactInfoProvider.class);

    private final WebSession webSession;
    private final CustomerRecordProvider customerRecordProvider;

    public CustomerContactInfoProvider(WebSession webSession) {
        this(
                webSession,
                new CustomerRecordProvider(webSession)
        );
    }

    public CustomerContactInfoProvider(
            WebSession webSession,
            CustomerRecordProvider customerRecordProvider
    ) {
        this.webSession = webSession;
        this.customerRecordProvider = customerRecordProvider == null
                ? new CustomerRecordProvider(webSession)
                : customerRecordProvider;
    }

    public CustomerContactInfo getCustomerContactInfo(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(customerId);

        if (customer == null) {
            log.warn("Customer contact info could not be loaded because customer was not found. customerId={}", customerId);
            return null;
        }

        return toCustomerContactInfo(customer);
    }

    public CustomerContactInfo getCustomerContactInfoFromSession() {
        if (webSession == null || webSession.getCustomerId() == null) {
            return null;
        }

        return getCustomerContactInfo(webSession.getCustomerId());
    }

    private CustomerContactInfo toCustomerContactInfo(CustomerRecord customer) {
        CustomerContactInfo info = new CustomerContactInfo();

        info.setCustomerId(customer.getCustomerId());
        info.setCustomerName(customer.getCustomerName());
        info.setContactName(customer.getSafeContactName());
        info.setContactEmail(customer.getContactEmail());

        CustomerStatus customerStatus = customer.getCustomerStatus();

        if (customerStatus == null) {
            customerStatus = CustomerStatus.CREATED;
        }

        info.setCustomerStatus(customerStatus);

        return info;
    }
}