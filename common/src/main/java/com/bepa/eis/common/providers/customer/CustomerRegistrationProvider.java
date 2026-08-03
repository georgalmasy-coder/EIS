package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.CustomerPaymentMethod;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.enums.customer.BillingPeriod;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.enums.customer.CustomerPaymentMethodStatus;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.providers.CustomerRegistrationUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomerRegistrationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationProvider.class);

    private static final String DEFAULT_PAYMENT_PROVIDER = "MANUAL";
    private static final String DEFAULT_CUSTOMER_MFA_POLICY = "OPTIONAL";

    private final WebSession webSession;
    private final CustomerRecordProvider customerRecordProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;
    private final SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider;
    private final CustomerModuleProvider customerModuleProvider;
    private final CustomerPaymentMethodProvider customerPaymentMethodProvider;
    private final CustomerWorkflowStarterProvider workflowStarterProvider;
    private final CustomerRegistrationUserProvider customerRegistrationUserProvider;

    public CustomerRegistrationProvider(WebSession webSession) {
        this(
                webSession,
                new CustomerRecordProvider(webSession),
                new SubscriptionPlanProvider(webSession),
                new SubscriptionPlanBillingPeriodProvider(webSession),
                new CustomerModuleProvider(webSession),
                new CustomerPaymentMethodProvider(webSession),
                new CustomerWorkflowStarterProvider(webSession),
                new CustomerRegistrationUserProvider(webSession)
        );
    }

    public CustomerRegistrationProvider(
            WebSession webSession,
            CustomerRecordProvider customerRecordProvider,
            SubscriptionPlanProvider subscriptionPlanProvider,
            SubscriptionPlanBillingPeriodProvider subscriptionPlanBillingPeriodProvider,
            CustomerModuleProvider customerModuleProvider,
            CustomerPaymentMethodProvider customerPaymentMethodProvider,
            CustomerWorkflowStarterProvider workflowStarterProvider,
            CustomerRegistrationUserProvider customerRegistrationUserProvider
    ) {
        this.webSession = webSession;
        this.customerRecordProvider = customerRecordProvider == null ? new CustomerRecordProvider(webSession) : customerRecordProvider;
        this.subscriptionPlanProvider = subscriptionPlanProvider == null ? new SubscriptionPlanProvider(webSession) : subscriptionPlanProvider;
        this.subscriptionPlanBillingPeriodProvider = subscriptionPlanBillingPeriodProvider == null
                ? new SubscriptionPlanBillingPeriodProvider(webSession)
                : subscriptionPlanBillingPeriodProvider;
        this.customerModuleProvider = customerModuleProvider == null ? new CustomerModuleProvider(webSession) : customerModuleProvider;
        this.customerPaymentMethodProvider = customerPaymentMethodProvider == null ? new CustomerPaymentMethodProvider(webSession) : customerPaymentMethodProvider;
        this.workflowStarterProvider = workflowStarterProvider == null ? new CustomerWorkflowStarterProvider(webSession) : workflowStarterProvider;
        this.customerRegistrationUserProvider = customerRegistrationUserProvider == null ? new CustomerRegistrationUserProvider(webSession) : customerRegistrationUserProvider;
    }

    public CustomerRegistrationResult registerCustomer(CustomerRegistrationData data) {
        if (data == null) {
            return CustomerRegistrationResult.failed("Customer registration data is required.");
        }

        String validationError = validate(data);

        if (validationError != null) {
            return CustomerRegistrationResult.failed(validationError);
        }

        if (customerRegistrationUserProvider.activeUserExistsByEmail(data.getContactEmail())) {
            return CustomerRegistrationResult.failed("Email is already used by an active user.");
        }

        SubscriptionPlan subscriptionPlan = resolveSubscriptionPlan(data);

        if (subscriptionPlan == null) {
            return CustomerRegistrationResult.failed("No active subscription plan was found for the selected subscription.");
        }

        if (!isSelectableBillingPeriod(subscriptionPlan, data.getBillingPeriodCode())) {
            return CustomerRegistrationResult.failed("Selected billing period is not available for the chosen subscription.");
        }

        SubscriptionPlanBillingPeriod selectedBillingPeriod = resolveBillingPeriod(
                subscriptionPlan,
                data.getBillingPeriodCode()
        );

        if (selectedBillingPeriod == null || selectedBillingPeriod.getSubscriptionPlanBillingPeriodId() == null) {
            return CustomerRegistrationResult.failed("Selected billing period could not be resolved.");
        }

        data.setSubscriptionPlanBillingPeriodId(selectedBillingPeriod.getSubscriptionPlanBillingPeriodId());

        CustomerRecord customerRecord = buildCustomerRecord(data);
        Integer customerId = customerRecordProvider.createCustomer(customerRecord);

        if (customerId == null) {
            return CustomerRegistrationResult.failed("Customer could not be created.");
        }

        Integer userId = customerRegistrationUserProvider.createActiveUser(
                data.getContactName(),
                data.getContactEmail(),
                data.getPhone()
        );

        if (userId == null) {
            log.warn(
                    "Customer was created, but user could not be created. customerId={}, email={}",
                    customerId,
                    data.getContactEmail()
            );

            return CustomerRegistrationResult.failed(
                    "Customer was created, but user could not be created for email: " + data.getContactEmail()
            );
        }

        boolean userCustomerLinked = customerRegistrationUserProvider.linkUserToCustomer(
                userId,
                customerId
        );

        if (!userCustomerLinked) {
            log.warn(
                    "Customer and user were created, but user could not be linked to customer. customerId={}, userId={}",
                    customerId,
                    userId
            );

            return CustomerRegistrationResult.failed(
                    "Customer and user were created, but user could not be linked to customer."
            );
        }

        CustomerModule customerModule = buildCustomerModule(
                customerId,
                subscriptionPlan,
                data.getSubscriptionPlanBillingPeriodId()
        );

        Integer customerModuleId = customerModuleProvider.createCustomerModule(customerModule);

        if (customerModuleId == null) {
            log.warn(
                    "Customer was created, but customer module could not be created. customerId={}, moduleCode={}",
                    customerId,
                    data.getModuleCode()
            );
        }

        CustomerPaymentMethod paymentMethod = buildPaymentMethod(
                customerId,
                data
        );

        Integer paymentMethodId = customerPaymentMethodProvider.createPaymentMethod(paymentMethod);

        if (paymentMethodId == null) {
            log.warn(
                    "Customer was created, but payment method could not be created. customerId={}",
                    customerId
            );
        }

        Integer workflowId = workflowStarterProvider.startCustomerOnboardingWorkflow(
                customerId,
                getChangedByUserId(),
                "Customer onboarding workflow started after customer registration."
        );

        if (workflowId == null) {
            log.warn(
                    "Customer was created, but onboarding workflow could not be started. customerId={}",
                    customerId
            );
        }

        return CustomerRegistrationResult.success(
                customerId,
                customerRecord.getCustomerPK(),
                customerModuleId,
                paymentMethodId,
                workflowId,
                userId,
                "Customer registration completed."
        );
    }

    private CustomerRecord buildCustomerRecord(CustomerRegistrationData data) {
        CustomerRecord customer = new CustomerRecord();

        customer.setCustomerName(data.getCustomerName());
        customer.setCvrNumber(data.getCvrNumber());
        customer.setVatNumber(data.getVatNumber());
        customer.setPhone(data.getPhone());

        customer.setAddress(data.getAddress());
        customer.setZipCode(data.getZipCode());
        customer.setCity(data.getCity());
        customer.setCountry(data.getCountry());

        customer.setContactName(data.getContactName());
        customer.setContactEmail(data.getContactEmail());

        customer.setCustomerStatus(CustomerStatus.CREATED);
        customer.setCustomerMfaPolicy(DEFAULT_CUSTOMER_MFA_POLICY);
        customer.setChangedByUserId(getChangedByUserId());
        customer.setLatest(true);

        return customer;
    }

    private CustomerModule buildCustomerModule(
            Integer customerId,
            SubscriptionPlan subscriptionPlan,
            Integer subscriptionPlanBillingPeriodId
    ) {
        CustomerModule customerModule = new CustomerModule();

        customerModule.setCustomerId(customerId);
        customerModule.setSubscriptionPlanId(subscriptionPlan.getSubscriptionPlanId());
        customerModule.setSubscriptionPlanBillingPeriodId(subscriptionPlanBillingPeriodId);
        customerModule.setModuleCode(subscriptionPlan.getModuleCode());
        customerModule.setModuleName(subscriptionPlan.getModuleName());
        customerModule.setCustomerModuleStatus(CustomerModuleStatus.TRIAL);
        customerModule.setLatest(true);

        return customerModule;
    }

    private CustomerPaymentMethod buildPaymentMethod(
            Integer customerId,
            CustomerRegistrationData data
    ) {
        CustomerPaymentMethod paymentMethod = new CustomerPaymentMethod();

        paymentMethod.setCustomerId(customerId);
        paymentMethod.setPaymentProvider(DEFAULT_PAYMENT_PROVIDER);
        paymentMethod.setProviderPaymentMethodReference("");
        paymentMethod.setCardholderName(data.getCardholderName());
        paymentMethod.setCardBrand("");
        paymentMethod.setMaskedCardNumber(maskCardNumber(data.getCardNumber()));
        paymentMethod.setExpiryMonth(parseInteger(data.getExpiryMonth()));
        paymentMethod.setExpiryYear(parseInteger(data.getExpiryYear()));
        paymentMethod.setBillingZipCode(data.getBillingZipCode());
        paymentMethod.setPaymentMethodStatus(CustomerPaymentMethodStatus.ACTIVE);
        paymentMethod.setLatest(true);

        return paymentMethod;
    }

    private String validate(CustomerRegistrationData data) {
        if (resolveSubscriptionPlan(data) == null) {
            return "Subscription is required.";
        }

        if (isBlank(data.getBillingPeriodCode())) {
            return "Billing period is required.";
        }

        if (isBlank(data.getCustomerName())) {
            return "Customer name is required.";
        }

        if (isBlank(data.getAddress())) {
            return "Address is required.";
        }

        if (isBlank(data.getZipCode())) {
            return "Zip code is required.";
        }

        if (isBlank(data.getCity())) {
            return "City is required.";
        }

        if (isBlank(data.getCountry())) {
            return "Country is required.";
        }

        if (isBlank(data.getPhone())) {
            return "Phone is required.";
        }

        if (isBlank(data.getContactEmail())) {
            return "Contact email is required.";
        }

        if (isBlank(data.getCardholderName())) {
            return "Cardholder name is required.";
        }

        if (isBlank(data.getCardNumber())) {
            return "Card number is required.";
        }

        if (isBlank(data.getExpiryMonth())) {
            return "Expiry month is required.";
        }

        if (isBlank(data.getExpiryYear())) {
            return "Expiry year is required.";
        }

        if (isBlank(data.getBillingZipCode())) {
            return "Billing zip code is required.";
        }

        return null;
    }

    private SubscriptionPlan resolveSubscriptionPlan(CustomerRegistrationData data) {
        if (data == null) {
            return null;
        }

        if (data.getSubscriptionPlanId() != null) {
            SubscriptionPlan planById = subscriptionPlanProvider.getPlanById(data.getSubscriptionPlanId());
            if (isSelectableSubscriptionPlan(planById)) {
                return planById;
            }
        }

        if (!isBlank(data.getModuleCode())) {
            SubscriptionPlan planByModuleCode = subscriptionPlanProvider.getActivePlanByModuleCode(data.getModuleCode());
            if (isSelectableSubscriptionPlan(planByModuleCode)) {
                return planByModuleCode;
            }
        }

        return null;
    }

    private boolean isSelectableSubscriptionPlan(SubscriptionPlan plan) {
        return plan != null
                && plan.getSubscriptionPlanId() != null
                && plan.getActive()
                && plan.getValidFrom() != null
                && !plan.getValidFrom().isAfter(java.time.LocalDate.now())
                && (plan.getValidTo() == null || !plan.getValidTo().isBefore(java.time.LocalDate.now()));
    }

    private boolean isSelectableBillingPeriod(
            SubscriptionPlan subscriptionPlan,
            String billingPeriodCode
    ) {
        if (subscriptionPlan == null || subscriptionPlan.getSubscriptionPlanId() == null || isBlank(billingPeriodCode)) {
            return false;
        }

        List<SubscriptionPlanBillingPeriod> billingPeriods = subscriptionPlanBillingPeriodProvider.getBillingPeriodsByPlanId(
                subscriptionPlan.getSubscriptionPlanId()
        );

        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            BillingPeriod enumValue = BillingPeriod.fromCode(billingPeriod.getBillingPeriodCode());

            if (enumValue == null || !enumValue.isActive()) {
                continue;
            }

            if (billingPeriodCode.trim().equalsIgnoreCase(billingPeriod.getBillingPeriodCode())) {
                return true;
            }
        }

        return false;
    }

    private SubscriptionPlanBillingPeriod resolveBillingPeriod(
            SubscriptionPlan subscriptionPlan,
            String billingPeriodCode
    ) {
        if (subscriptionPlan == null || subscriptionPlan.getSubscriptionPlanId() == null || isBlank(billingPeriodCode)) {
            return null;
        }

        List<SubscriptionPlanBillingPeriod> billingPeriods = subscriptionPlanBillingPeriodProvider.getBillingPeriodsByPlanId(
                subscriptionPlan.getSubscriptionPlanId()
        );

        for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
            BillingPeriod enumValue = BillingPeriod.fromCode(billingPeriod.getBillingPeriodCode());

            if (enumValue == null || !enumValue.isActive()) {
                continue;
            }

            if (billingPeriodCode.trim().equalsIgnoreCase(billingPeriod.getBillingPeriodCode())) {
                return billingPeriod;
            }
        }

        return null;
    }

    private Integer getChangedByUserId() {
        if (webSession == null) {
            return null;
        }

        return webSession.getUserId();
    }

    private String maskCardNumber(String cardNumber) {
        String digits = onlyDigits(cardNumber);

        if (digits.length() <= 4) {
            return digits;
        }

        String lastFour = digits.substring(digits.length() - 4);

        return "**** **** **** " + lastFour;
    }

    private String onlyDigits(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D", "");
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class CustomerRegistrationData {

        private Integer subscriptionPlanId;
        private Integer subscriptionPlanBillingPeriodId;
        private String billingPeriodCode;
        private String moduleCode;
        private String customerName;
        private String vatNumber;
        private String cvrNumber;
        private String phone;
        private String address;
        private String zipCode;
        private String city;
        private String country;
        private String contactName;
        private String contactEmail;

        private String cardholderName;
        private String cardNumber;
        private String expiryMonth;
        private String expiryYear;
        private String billingZipCode;

        public CustomerRegistrationData() {
            subscriptionPlanId = null;
            subscriptionPlanBillingPeriodId = null;
            billingPeriodCode = "";
            moduleCode = "";
            customerName = "";
            vatNumber = "";
            cvrNumber = "";
            phone = "";
            address = "";
            zipCode = "";
            city = "";
            country = "";
            contactName = "";
            contactEmail = "";

            cardholderName = "";
            cardNumber = "";
            expiryMonth = "";
            expiryYear = "";
            billingZipCode = "";
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

        public String getBillingPeriodCode() {
            return billingPeriodCode;
        }

        public void setBillingPeriodCode(String billingPeriodCode) {
            this.billingPeriodCode = safeText(billingPeriodCode).toUpperCase();
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = safeText(moduleCode).toUpperCase();
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = safeText(customerName);
        }

        public String getVatNumber() {
            return vatNumber;
        }

        public void setVatNumber(String vatNumber) {
            this.vatNumber = safeText(vatNumber).toUpperCase();
        }

        public String getCvrNumber() {
            return cvrNumber;
        }

        public void setCvrNumber(String cvrNumber) {
            this.cvrNumber = safeText(cvrNumber);
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

        public String getCardholderName() {
            return cardholderName;
        }

        public void setCardholderName(String cardholderName) {
            this.cardholderName = safeText(cardholderName);
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = safeText(cardNumber);
        }

        public String getExpiryMonth() {
            return expiryMonth;
        }

        public void setExpiryMonth(String expiryMonth) {
            this.expiryMonth = safeText(expiryMonth);
        }

        public String getExpiryYear() {
            return expiryYear;
        }

        public void setExpiryYear(String expiryYear) {
            this.expiryYear = safeText(expiryYear);
        }

        public String getBillingZipCode() {
            return billingZipCode;
        }

        public void setBillingZipCode(String billingZipCode) {
            this.billingZipCode = safeText(billingZipCode);
        }

        private static String safeText(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public static class CustomerRegistrationResult {

        private final boolean success;
        private final String message;
        private final Integer customerId;
        private final Integer customerPK;
        private final Integer customerModuleId;
        private final Integer customerPaymentMethodId;
        private final Integer workflowId;
        private final Integer userId;

        private CustomerRegistrationResult(
                boolean success,
                String message,
                Integer customerId,
                Integer customerPK,
                Integer customerModuleId,
                Integer customerPaymentMethodId,
                Integer workflowId,
                Integer userId
        ) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.customerId = customerId;
            this.customerPK = customerPK;
            this.customerModuleId = customerModuleId;
            this.customerPaymentMethodId = customerPaymentMethodId;
            this.workflowId = workflowId;
            this.userId = userId;
        }

        public static CustomerRegistrationResult success(
                Integer customerId,
                Integer customerPK,
                Integer customerModuleId,
                Integer customerPaymentMethodId,
                Integer workflowId,
                Integer userId,
                String message
        ) {
            return new CustomerRegistrationResult(
                    true,
                    message,
                    customerId,
                    customerPK,
                    customerModuleId,
                    customerPaymentMethodId,
                    workflowId,
                    userId
            );
        }

        public static CustomerRegistrationResult failed(String message) {
            return new CustomerRegistrationResult(
                    false,
                    message,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Integer getCustomerId() {
            return customerId;
        }

        public Integer getCustomerPK() {
            return customerPK;
        }

        public Integer getCustomerModuleId() {
            return customerModuleId;
        }

        public Integer getCustomerPaymentMethodId() {
            return customerPaymentMethodId;
        }

        public Integer getWorkflowId() {
            return workflowId;
        }

        public Integer getUserId() {
            return userId;
        }
    }
}
