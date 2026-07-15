package com.bepa.eis.server.dataprovider.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerPaymentMethod;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.providers.customer.CustomerPaymentMethodProvider;
import com.bepa.eis.common.providers.customer.CustomerPaymentProvider;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionProvider;
import com.bepa.eis.common.providers.customer.SubscriptionPlanProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.strings.email.ContactEmail;
import com.bepa.eis.server.dataprovider.fields.strings.phone.ContactPhone;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class CustomerInfo extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final EntityRequestType requestType;
    private final Integer customerId;

    private final CustomerRecord customerRecord;
    private final CustomerSubscription customerSubscription;
    private final CustomerPaymentMethod customerPaymentMethod;
    private final List<CustomerPayment> customerPayments;
    private final List<SubscriptionPlan> activePlans;

    public CustomerInfo(
            WebSession webSession,
            EntityRequestType requestType
    ) throws Exception {
        this(webSession, requestType, null, null);
    }

    public CustomerInfo(
            WebSession webSession,
            EntityRequestType requestType,
            Integer customerId
    ) throws Exception {
        this(webSession, requestType, customerId, null);
    }

    public CustomerInfo(
            WebSession webSession,
            EntityRequestType requestType,
            Integer customerId,
            Integer version
    ) throws Exception {
        super(webSession);

        this.requestType = requestType;
        this.customerId = resolveCustomerId(webSession, customerId);
        this.customerRecord = resolveCustomerRecord(webSession);
        this.customerSubscription = resolveCustomerSubscription(webSession);
        this.customerPaymentMethod = resolveCustomerPaymentMethod(webSession);
        this.customerPayments = resolveCustomerPayments(webSession);
        this.activePlans = resolveActivePlans(webSession);

        rootElement = initXmlDocument("CustomerInfo");

        appendTopPanel(webSession);
        appendLookups(webSession);
        appendCustomerDocument(webSession);
    }

    private Integer resolveCustomerId(
            WebSession webSession,
            Integer requestedCustomerId
    ) {
        return webSession == null ? null : webSession.getCustomerId();
    }

    private CustomerRecord resolveCustomerRecord(WebSession webSession) {
        CustomerRecordProvider provider = new CustomerRecordProvider(webSession);
        CustomerRecord record = provider.getLatestCustomerByCustomerId(customerId);

        return record == null ? new CustomerRecord() : record;
    }

    private CustomerSubscription resolveCustomerSubscription(WebSession webSession) {
        CustomerSubscriptionProvider provider = new CustomerSubscriptionProvider(webSession);
        CustomerSubscription subscription = provider.getLatestSubscriptionByCustomerId(customerId);

        return subscription == null ? new CustomerSubscription() : subscription;
    }

    private CustomerPaymentMethod resolveCustomerPaymentMethod(WebSession webSession) {
        CustomerPaymentMethodProvider provider = new CustomerPaymentMethodProvider(webSession);
        CustomerPaymentMethod paymentMethod = provider.getLatestPaymentMethodByCustomerId(customerId);

        return paymentMethod == null ? new CustomerPaymentMethod() : paymentMethod;
    }

    private List<CustomerPayment> resolveCustomerPayments(WebSession webSession) {
        CustomerPaymentProvider provider = new CustomerPaymentProvider(webSession);
        List<CustomerPayment> payments = provider.getPaymentsByCustomerId(customerId);

        return payments == null ? Collections.emptyList() : payments;
    }

    private List<SubscriptionPlan> resolveActivePlans(WebSession webSession) {
        SubscriptionPlanProvider provider = new SubscriptionPlanProvider(webSession);
        List<SubscriptionPlan> plans = provider.getActivePlans();

        return plans == null ? Collections.emptyList() : plans;
    }

    private void appendTopPanel(WebSession webSession) throws Exception {
        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession();

        if (topPanel != null && topPanel.getTopPanelElements() != null) {
            rootElement.addElement(topPanel.getTopPanelElements());
            return;
        }

        rootElement.addElement(new ListOfElements(webSession, "TopPanel"));
    }

    private void appendLookups(WebSession webSession) {
        ListOfElements lookups = new ListOfElements(webSession, "lookups");
        ListOfElements countryCodeLookup = new ListOfElements(webSession, "lookup");
        countryCodeLookup.addAttribute("name", "countryCode");

        for (CustomerLookupCache.PhoneCountryRule rule : CustomerLookupCache.getPhoneCountryRules()) {
            countryCodeLookup.addElement(phoneCountryOption(webSession, rule));
        }

        lookups.addElement(countryCodeLookup);
        rootElement.addElement(lookups);
    }

    private ListOfElements phoneCountryOption(
            WebSession webSession,
            CustomerLookupCache.PhoneCountryRule rule
    ) {
        ListOfElements option = new ListOfElements(webSession, "option");
        option.addAttribute("code", safeText(rule.code(), ""));
        option.addAttribute("country", safeText(rule.country(), ""));
        option.addAttribute("label", safeText(rule.country(), ""));
        option.addAttribute("min", String.valueOf(rule.minDigits()));
        option.addAttribute("max", String.valueOf(rule.maxDigits()));
        option.addAttribute("example", safeText(rule.example(), ""));
        return option;
    }

    private void appendCustomerDocument(WebSession webSession) {
        ListOfElements customerDocument = new ListOfElements(webSession, "customerDocument");
        ListOfElements customerBasis = new ListOfElements(webSession, "customerBasis");

        addCustomerBasisFields(webSession, customerBasis, customerRecord);
        customerDocument.addElement(customerBasis);
        addCustomerSecurityFields(webSession, customerDocument);
        addCustomerSubscriptionFields(webSession, customerDocument);
        addCustomerPaymentMethodFields(webSession, customerDocument);
        addCustomerPaymentsFields(webSession, customerDocument);

        rootElement.addElement(customerDocument);
    }

    private void addCustomerBasisFields(
            WebSession webSession,
            ListOfElements customer,
            CustomerRecord customerRecord
    ) {
//        ProjectRecord safeProjectRecord = projectRecord == null ? new ProjectRecord() : projectRecord;

        CustomerId customerIdField = new CustomerId(customerRecord.getCustomerId());
        customerIdField.setFieldNotVisible();
        customer.addElement(customerIdField);

        CustomerName customerName = new CustomerName(customerRecord.getCustomerName());
        customerName.setFieldEditable();
        customerName.setFieldRequired();
        customer.addElement(customerName);

        CvrNumber cvrNumber = new CvrNumber(customerRecord.getCvrNumber());
        cvrNumber.setFieldEditable();
        cvrNumber.setFieldNotRequired();
        customer.addElement(cvrNumber);

        ContactPhone contactPhone = new ContactPhone();
        contactPhone.setValue(customerRecord.getPhone());
        contactPhone.setFieldEditable();
        contactPhone.setFieldNotRequired();
        customer.addElement(contactPhone);

        ContactEmail contactEmail = new ContactEmail();
        contactEmail.setValue(customerRecord.getContactEmail());
        contactEmail.setFieldEditable();
        contactEmail.setFieldRequired();
        customer.addElement(contactEmail);

        ContactName contactName = new ContactName(customerRecord.getContactName());
        contactName.setFieldEditable();
        contactName.setFieldRequired();
        customer.addElement(contactName);

        Address address = new Address(customerRecord.getAddress());
        address.setFieldEditable();
        address.setFieldRequired();
        customer.addElement(address);

        ZipCode zipCode = new ZipCode(customerRecord.getZipCode());
        zipCode.setFieldEditable();
        zipCode.setFieldRequired();
        customer.addElement(zipCode);

        City city = new City(customerRecord.getCity());
        city.setFieldEditable();
        city.setFieldRequired();
        customer.addElement(city);

        Country country = new Country(customerRecord.getCountry());
        country.setFieldEditable();
        country.setFieldRequired();
        customer.addElement(country);


        if (customerRecord.getCustomerId() != null) {
            ChangedBy changedBy = new ChangedBy(getWebSession());
            changedBy.setValue(customerRecord.getChangedByUserId());
            changedBy.setFieldNotEditable();
            customer.addElement(changedBy);

            ChangedDateTime changedDateTime = new ChangedDateTime(customerRecord.getChangedDateTime());
            changedDateTime.setFieldVisible();
            changedDateTime.setFieldNotEditable();
            customer.addElement(changedDateTime);
        }
    }

    private void addCustomerSecurityFields(
            WebSession webSession,
            ListOfElements customerDocument
    ) {
        ListOfElements customerSecurity = new ListOfElements(webSession, "customerSecurity");

        // CustomerMfaPolicy: use CustomerRecordProvider.getLatestCustomerByCustomerId(customerId)
        // and map CustomerRecord.getCustomerMfaPolicy() into
        // com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerMfaPolicy.
        // Optional future security flags:
        // CustomerAccessProvider.isCustomerLoginAllowed(customerId)
        // CustomerAccessProvider.isCustomerSuspended(customerId)
        // CustomerAccessProvider.isCustomerCancelled(customerId)
        // if you want to expose support-only status badges later.

        customerDocument.addElement(customerSecurity);
    }

    private void addCustomerSubscriptionFields(
            WebSession webSession,
            ListOfElements customerDocument
    ) {
        ListOfElements customerSubscription = new ListOfElements(webSession, "customerSubscription");
        ListOfElements availablePlans = new ListOfElements(webSession, "availablePlans");

        // Subscription fields for the local administrator view:
        // SubscriptionStatus -> CustomerSubscriptionProvider.getLatestSubscriptionByCustomerId(customerId)
        //   and CustomerSubscription.getSubscriptionStatusCode().
        // SubscriptionPlanId / SubscriptionPlanName -> same provider and
        //   CustomerSubscription.getSubscriptionPlanId() / getSubscriptionPlanName().
        // TrialStartAt / TrialEndAt / TrialReminderSentAt -> CustomerSubscriptionProvider.getLatestSubscriptionByCustomerId(customerId).
        // PeriodStartAt / PeriodEndAt / RenewalReminderSentAt / ContinuationConfirmedAt / RenewalConfirmedAt / GracePeriodEndsAt
        //   -> CustomerSubscriptionProvider.getLatestSubscriptionByCustomerId(customerId).
        // Available plan selector for upgrade/downgrade -> SubscriptionPlanProvider.getActivePlans().
        // The data is loaded above into customerSubscription and activePlans for later rendering.

        customerSubscription.addElement(availablePlans);
        customerDocument.addElement(customerSubscription);
    }

    private void addCustomerPaymentMethodFields(
            WebSession webSession,
            ListOfElements customerDocument
    ) {
        ListOfElements customerPaymentMethod = new ListOfElements(webSession, "customerPaymentMethod");

        // Payment method fields for the local administrator view:
        // PaymentProvider / ProviderPaymentMethodReference /
        // CardholderName / CardBrand / MaskedCardNumber / ExpiryMonth / ExpiryYear / BillingZipCode /
        // PaymentMethodStatus -> CustomerPaymentMethodProvider.getLatestPaymentMethodByCustomerId(customerId).
        // The loaded customerPaymentMethod DTO is held ready for later XML mapping.
        customerDocument.addElement(customerPaymentMethod);
    }

    private void addCustomerPaymentsFields(
            WebSession webSession,
            ListOfElements customerDocument
    ) {
        ListOfElements customerPayments = new ListOfElements(webSession, "customerPayments");
        ListOfElements upcomingPayments = new ListOfElements(webSession, "upcomingPayments");
        ListOfElements completedPayments = new ListOfElements(webSession, "completedPayments");

        // Payment history fields for the local administrator view:
        // PaymentId / PaymentStatus / PaymentProvider / PaymentProviderReference / Amount / Currency /
        // PaymentDueAt / GracePeriodEndsAt / RequestedAt / AuthorizedAt / CapturedAt /
        // SucceededAt / FailedAt / CancelledAt / FailureReason.
        // The data should come from CustomerPaymentProvider.getPaymentsByCustomerId(customerId),
        // using CustomerPayment.isPending(), CustomerPayment.isDue(now),
        // CustomerPayment.isSuccessful(), CustomerPayment.isFailed() and CustomerPayment.isTerminal()
        // to split rows between upcomingPayments and completedPayments.
        customerPayments.addElement(upcomingPayments);
        customerPayments.addElement(completedPayments);
        customerDocument.addElement(customerPayments);
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

}
