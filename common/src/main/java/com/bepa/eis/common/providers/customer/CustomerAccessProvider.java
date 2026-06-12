package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.utilities.ValueUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerAccessProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerAccessProvider.class);

    private final WebSession webSession;
    private final CustomerRecordProvider customerRecordProvider;

    public CustomerAccessProvider(WebSession webSession) {
        this(
                webSession,
                new CustomerRecordProvider(webSession)
        );
    }

    public CustomerAccessProvider(
            WebSession webSession,
            CustomerRecordProvider customerRecordProvider
    ) {
        this.webSession = webSession;
        this.customerRecordProvider = customerRecordProvider == null
                ? new CustomerRecordProvider(webSession)
                : customerRecordProvider;
    }

    public boolean suspendCustomer(
            Integer customerId,
            String suspensionReason
    ) {
        if (customerId == null) {
            return false;
        }

        boolean updated = customerRecordProvider.updateCustomerStatus(
                customerId,
                CustomerStatus.SUSPENDED,
                getChangedByUserId()
        );

        if (updated) {
            log.info(
                    "Customer suspended. customerId={}, reason={}",
                    customerId,
                    ValueUtil.safeText(suspensionReason)
            );
        } else {
            log.warn(
                    "Customer could not be suspended. customerId={}, reason={}",
                    customerId,
                    ValueUtil.safeText(suspensionReason)
            );
        }

        return updated;
    }

    public boolean reactivateCustomer(Integer customerId) {
        if (customerId == null) {
            return false;
        }

        boolean updated = customerRecordProvider.updateCustomerStatus(
                customerId,
                CustomerStatus.SUBSCRIPTION_ACTIVE,
                getChangedByUserId()
        );

        if (updated) {
            log.info("Customer reactivated. customerId={}", customerId);
        } else {
            log.warn("Customer could not be reactivated. customerId={}", customerId);
        }

        return updated;
    }

    public boolean setCustomerStatus(
            Integer customerId,
            CustomerStatus customerStatus
    ) {
        if (customerId == null || customerStatus == null) {
            return false;
        }

        boolean updated = customerRecordProvider.updateCustomerStatus(
                customerId,
                customerStatus,
                getChangedByUserId()
        );

        if (updated) {
            log.info(
                    "Customer status updated. customerId={}, customerStatus={}",
                    customerId,
                    customerStatus.getCode()
            );
        } else {
            log.warn(
                    "Customer status could not be updated. customerId={}, customerStatus={}",
                    customerId,
                    customerStatus.getCode()
            );
        }

        return updated;
    }

    public boolean isCustomerLoginAllowed(Integer customerId) {
        if (customerId == null) {
            return false;
        }

        CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(customerId);

        if (customer == null) {
            return false;
        }

        CustomerStatus customerStatus = customer.getCustomerStatus();

        if (customerStatus == null) {
            return false;
        }

        return customerStatus.isLoginAllowedByDefault();
    }

    public CustomerStatus getCustomerStatus(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(customerId);

        if (customer == null) {
            return null;
        }

        return customer.getCustomerStatus();
    }

    public boolean isCustomerSuspended(Integer customerId) {
        CustomerStatus customerStatus = getCustomerStatus(customerId);

        return customerStatus != null && customerStatus.isSuspended();
    }

    public boolean isCustomerCancelled(Integer customerId) {
        CustomerStatus customerStatus = getCustomerStatus(customerId);

        return customerStatus != null && customerStatus.isCancelled();
    }

    private Integer getChangedByUserId() {
        if (webSession == null) {
            return null;
        }

        return webSession.getUserId();
    }

}