package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerPaymentMethod;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.enums.customer.CustomerPaymentMethodStatus;
import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerModuleProvider;
import com.bepa.eis.common.providers.customer.CustomerPaymentMethodProvider;
import com.bepa.eis.common.providers.customer.CustomerPaymentProvider;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowEventProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "CustomerAdministrationServlet", urlPatterns = {
        "/api/admin/customers"
})
public class CustomerAdministrationServlet extends AbstractAdminServlet {

    @Override
    public void processGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        Integer customerId = intValue(request.getParameter("customerId"));

        if (customerId == null) {
            writeXml(
                    response,
                    HttpServletResponse.SC_OK,
                    buildCustomerListXml()
            );
            return;
        }

        writeXml(
                response,
                HttpServletResponse.SC_OK,
                buildCustomerDetailXml(customerId)
        );
    }

    @Override
    public void processPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        SaveResult result = saveCustomerAdministration(request);

        writeXml(
                response,
                result.success()
                        ? HttpServletResponse.SC_OK
                        : HttpServletResponse.SC_BAD_REQUEST,
                buildSaveResultXml(result)
        );
    }

    private SaveResult saveCustomerAdministration(HttpServletRequest request) {
        try {
            Document document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(request.getInputStream());

            document.getDocumentElement().normalize();

            CustomerRecord customer = parseCustomer(document);
            CustomerSubscription subscription = parseSubscription(document);
            CustomerPayment payment = parseLatestPayment(document);
            CustomerPaymentMethod paymentMethod = parsePaymentMethod(document);
            List<CustomerModule> modules = parseModules(document);

            Integer customerId = customer == null ? null : customer.getCustomerId();

            if (customerId == null) {
                return SaveResult.failed(null, "customerId is required.");
            }

            CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(null);
            CustomerSubscriptionProvider subscriptionProvider = new CustomerSubscriptionProvider(null);
            CustomerPaymentProvider paymentProvider = new CustomerPaymentProvider(null);
            CustomerPaymentMethodProvider paymentMethodProvider = new CustomerPaymentMethodProvider(null);
            CustomerModuleProvider moduleProvider = new CustomerModuleProvider(null);

            if (customer != null) {
                Integer customerPK = customerRecordProvider.updateCustomer(customer);

                if (customerPK == null) {
                    return SaveResult.failed(customerId, "Customer could not be saved.");
                }
            }

            if (subscription != null && subscription.getSubscriptionId() != null) {
                boolean subscriptionSaved = subscriptionProvider.updateSubscription(subscription);

                if (!subscriptionSaved) {
                    return SaveResult.failed(customerId, "Subscription could not be saved.");
                }
            }

            if (payment != null && payment.getPaymentId() != null) {
                boolean paymentSaved = paymentProvider.updatePayment(payment);

                if (!paymentSaved) {
                    return SaveResult.failed(customerId, "Payment could not be saved.");
                }
            }

            if (paymentMethod != null && paymentMethod.getCustomerPaymentMethodId() != null) {
                boolean paymentMethodSaved = paymentMethodProvider.updatePaymentMethod(paymentMethod);

                if (!paymentMethodSaved) {
                    return SaveResult.failed(customerId, "Payment method could not be saved.");
                }
            }

            for (CustomerModule module : modules) {
                if (module.getCustomerModuleId() == null) {
                    continue;
                }

                boolean moduleSaved = moduleProvider.updateCustomerModule(module);

                if (!moduleSaved) {
                    return SaveResult.failed(
                            customerId,
                            "Customer module could not be saved. customerModuleId=" + module.getCustomerModuleId()
                    );
                }
            }

            return SaveResult.ok(customerId, "Customer administration data saved.");
        } catch (Exception e) {
            return SaveResult.failed(null, "Customer administration data could not be saved: " + e.getMessage());
        }
    }

    private String buildCustomerListXml() {
        CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(null);
        List<CustomerRecord> customers = customerRecordProvider.getAllLatestCustomers();

        StringBuilder xml = new StringBuilder();

        appendXmlHeader(xml);
        xml.append("<customerAdministration>");

        appendLookups(xml);

        xml.append("<customers>");

        if (customers != null) {
            for (CustomerRecord customer : customers) {
                appendCustomerListItem(xml, customer);
            }
        }

        xml.append("</customers>");
        xml.append("</customerAdministration>");

        return xml.toString();
    }

    private String buildCustomerDetailXml(Integer customerId) {
        CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(null);
        CustomerSubscriptionProvider subscriptionProvider = new CustomerSubscriptionProvider(null);
        CustomerPaymentProvider paymentProvider = new CustomerPaymentProvider(null);
        CustomerPaymentMethodProvider paymentMethodProvider = new CustomerPaymentMethodProvider(null);
        CustomerModuleProvider moduleProvider = new CustomerModuleProvider(null);
        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflowEventProvider workflowEventProvider = new CustomerWorkflowEventProvider(null);

        CustomerRecord customer = customerRecordProvider.getLatestCustomerByCustomerId(customerId);
        CustomerSubscription subscription = subscriptionProvider.getLatestSubscriptionByCustomerId(customerId);
        CustomerPayment latestPayment = paymentProvider.getLatestPaymentByCustomerId(customerId);
        CustomerPaymentMethod paymentMethod = paymentMethodProvider.getLatestPaymentMethodByCustomerId(customerId);
        List<CustomerModule> modules = moduleProvider.getLatestCustomerModules(customerId);
        CustomerWorkflow workflow = workflowProvider.getActiveWorkflowByCustomerId(customerId);
        List<CustomerWorkflowEvent> workflowEvents = workflowEventProvider.getWorkflowEventsByCustomerId(customerId);

        StringBuilder xml = new StringBuilder();

        appendXmlHeader(xml);
        xml.append("<customerAdministration>");

        appendLookups(xml);

        xml.append("<customerDetail>");

        appendCustomerDetail(xml, customer);
        appendSubscription(xml, subscription);
        appendLatestPayment(xml, latestPayment);
        appendPaymentMethod(xml, paymentMethod);
        appendModules(xml, modules);
        appendWorkflow(xml, workflow);
        appendWorkflowEvents(xml, workflowEvents);

        xml.append("</customerDetail>");
        xml.append("</customerAdministration>");

        return xml.toString();
    }

    private String buildSaveResultXml(SaveResult result) {
        StringBuilder xml = new StringBuilder();

        appendXmlHeader(xml);
        xml.append("<customerAdministrationSaveResult>");
        appendElement(xml, "success", result.success());
        appendElement(xml, "customerId", result.customerId());
        appendElement(xml, "message", result.message());
        xml.append("</customerAdministrationSaveResult>");

        return xml.toString();
    }

    private CustomerRecord parseCustomer(Document document) {
        Element customerElement = firstElement(document, "customer");

        if (customerElement == null) {
            return null;
        }

        CustomerRecord customer = new CustomerRecord();

        customer.setCustomerId(intValue(text(customerElement, "customerId")));
        customer.setCustomerPK(intValue(text(customerElement, "customerPK")));
        customer.setVersion(intValue(text(customerElement, "version")));
        customer.setCustomerName(text(customerElement, "customerName"));
        customer.setCvrNumber(text(customerElement, "cvrNumber"));
        customer.setPhone(text(customerElement, "phone"));
        customer.setAddress(text(customerElement, "address"));
        customer.setZipCode(text(customerElement, "zipCode"));
        customer.setCity(text(customerElement, "city"));
        customer.setCountry(text(customerElement, "country"));
        customer.setContactName(text(customerElement, "contactName"));
        customer.setContactEmail(text(customerElement, "contactEmail"));
        customer.setCustomerStatus(CustomerStatus.fromCodeOrDefault(
                text(customerElement, "customerStatus"),
                CustomerStatus.CREATED
        ));
        customer.setChangedByUserId(intValue(text(customerElement, "changedByUserId")));
        customer.setLatest(true);

        return customer;
    }

    private CustomerSubscription parseSubscription(Document document) {
        Element subscriptionElement = firstElement(document, "subscription");

        if (subscriptionElement == null) {
            return null;
        }

        CustomerSubscription subscription = new CustomerSubscription();

        subscription.setSubscriptionId(intValue(text(subscriptionElement, "subscriptionId")));
        subscription.setCustomerId(intValue(text(subscriptionElement, "customerId")));
        subscription.setSubscriptionStatus(CustomerSubscriptionStatus.fromCodeOrDefault(
                text(subscriptionElement, "subscriptionStatus"),
                CustomerSubscriptionStatus.NONE
        ));
        subscription.setSubscriptionPlanId(intValue(text(subscriptionElement, "subscriptionPlanId")));
        subscription.setSubscriptionPlanName(text(subscriptionElement, "subscriptionPlanName"));
        subscription.setTrialStartAt(timestampValue(text(subscriptionElement, "trialStartAt")));
        subscription.setTrialEndAt(timestampValue(text(subscriptionElement, "trialEndAt")));
        subscription.setTrialReminderSentAt(timestampValue(text(subscriptionElement, "trialReminderSentAt")));
        subscription.setPeriodStartAt(timestampValue(text(subscriptionElement, "periodStartAt")));
        subscription.setPeriodEndAt(timestampValue(text(subscriptionElement, "periodEndAt")));
        subscription.setRenewalReminderSentAt(timestampValue(text(subscriptionElement, "renewalReminderSentAt")));
        subscription.setContinuationConfirmedAt(timestampValue(text(subscriptionElement, "continuationConfirmedAt")));
        subscription.setRenewalConfirmedAt(timestampValue(text(subscriptionElement, "renewalConfirmedAt")));
        subscription.setGracePeriodEndsAt(timestampValue(text(subscriptionElement, "gracePeriodEndsAt")));

        return subscription;
    }

    private CustomerPayment parseLatestPayment(Document document) {
        Element paymentElement = firstElement(document, "latestPayment");

        if (paymentElement == null) {
            return null;
        }

        CustomerPayment payment = new CustomerPayment();

        payment.setPaymentId(intValue(text(paymentElement, "paymentId")));
        payment.setCustomerId(intValue(text(paymentElement, "customerId")));
        payment.setSubscriptionId(intValue(text(paymentElement, "subscriptionId")));
        payment.setPaymentStatus(CustomerPaymentStatus.fromCodeOrDefault(
                text(paymentElement, "paymentStatus"),
                CustomerPaymentStatus.NONE
        ));
        payment.setPaymentProvider(text(paymentElement, "paymentProvider"));
        payment.setPaymentProviderReference(text(paymentElement, "paymentProviderReference"));
        payment.setAmount(decimalValue(text(paymentElement, "amount")));
        payment.setCurrency(text(paymentElement, "currency"));
        payment.setPaymentDueAt(timestampValue(text(paymentElement, "paymentDueAt")));
        payment.setGracePeriodEndsAt(timestampValue(text(paymentElement, "gracePeriodEndsAt")));
        payment.setRequestedAt(timestampValue(text(paymentElement, "requestedAt")));
        payment.setAuthorizedAt(timestampValue(text(paymentElement, "authorizedAt")));
        payment.setCapturedAt(timestampValue(text(paymentElement, "capturedAt")));
        payment.setSucceededAt(timestampValue(text(paymentElement, "succeededAt")));
        payment.setFailedAt(timestampValue(text(paymentElement, "failedAt")));
        payment.setCancelledAt(timestampValue(text(paymentElement, "cancelledAt")));
        payment.setFailureReason(text(paymentElement, "failureReason"));

        return payment;
    }

    private CustomerPaymentMethod parsePaymentMethod(Document document) {
        Element paymentMethodElement = firstElement(document, "paymentMethod");

        if (paymentMethodElement == null) {
            return null;
        }

        CustomerPaymentMethod paymentMethod = new CustomerPaymentMethod();

        paymentMethod.setCustomerPaymentMethodId(intValue(text(paymentMethodElement, "customerPaymentMethodId")));
        paymentMethod.setCustomerId(intValue(text(paymentMethodElement, "customerId")));
        paymentMethod.setPaymentProvider(text(paymentMethodElement, "paymentProvider"));
        paymentMethod.setProviderPaymentMethodReference(text(paymentMethodElement, "providerPaymentMethodReference"));
        paymentMethod.setCardholderName(text(paymentMethodElement, "cardholderName"));
        paymentMethod.setCardBrand(text(paymentMethodElement, "cardBrand"));
        paymentMethod.setMaskedCardNumber(text(paymentMethodElement, "maskedCardNumber"));
        paymentMethod.setExpiryMonth(intValue(text(paymentMethodElement, "expiryMonth")));
        paymentMethod.setExpiryYear(intValue(text(paymentMethodElement, "expiryYear")));
        paymentMethod.setBillingZipCode(text(paymentMethodElement, "billingZipCode"));
        paymentMethod.setPaymentMethodStatus(CustomerPaymentMethodStatus.fromCodeOrDefault(
                text(paymentMethodElement, "paymentMethodStatus"),
                CustomerPaymentMethodStatus.ACTIVE
        ));

        return paymentMethod;
    }

    private List<CustomerModule> parseModules(Document document) {
        List<CustomerModule> modules = new ArrayList<>();
        NodeList moduleNodes = document.getElementsByTagName("module");

        for (int index = 0; index < moduleNodes.getLength(); index++) {
            if (!(moduleNodes.item(index) instanceof Element moduleElement)) {
                continue;
            }

            CustomerModule module = new CustomerModule();

            module.setCustomerModuleId(intValue(text(moduleElement, "customerModuleId")));
            module.setCustomerId(intValue(text(moduleElement, "customerId")));
            module.setSubscriptionPlanId(intValue(text(moduleElement, "subscriptionPlanId")));
            module.setModuleCode(text(moduleElement, "moduleCode"));
            module.setModuleName(text(moduleElement, "moduleName"));
            module.setCustomerModuleStatus(CustomerModuleStatus.fromCodeOrDefault(
                    text(moduleElement, "customerModuleStatus"),
                    CustomerModuleStatus.ACTIVE
            ));

            modules.add(module);
        }

        return modules;
    }

    private void appendLookups(StringBuilder xml) {
        xml.append("<lookups>");

        appendStaticLookup(
                xml,
                "workflowType",
                new LookupOption(
                        "CUSTOMER_ONBOARDING",
                        "Customer onboarding"
                )
        );

        appendLookup(
                xml,
                "customerStatus",
                CustomerStatus.class
        );

        appendLookup(
                xml,
                "subscriptionStatus",
                CustomerSubscriptionStatus.class
        );

        appendLookup(
                xml,
                "paymentStatus",
                CustomerPaymentStatus.class
        );

        appendLookup(
                xml,
                "paymentMethodStatus",
                CustomerPaymentMethodStatus.class
        );

        appendLookup(
                xml,
                "customerModuleStatus",
                CustomerModuleStatus.class
        );

        appendLookup(
                xml,
                "workflowStatus",
                CustomerWorkflowStatus.class
        );

        appendLookup(
                xml,
                "workflowState",
                CustomerWorkflowState.class
        );

        appendLookup(
                xml,
                "workflowEventType",
                CustomerWorkflowEventType.class
        );

        xml.append("</lookups>");
    }

    private void appendStaticLookup(
            StringBuilder xml,
            String lookupName,
            LookupOption... options
    ) {
        xml.append("<lookup name=\"")
                .append(escapeXml(lookupName))
                .append("\">");

        if (options != null) {
            for (LookupOption option : options) {
                if (option == null || option.code() == null || option.code().isBlank()) {
                    continue;
                }

                xml.append("<option");

                xml.append(" code=\"")
                        .append(escapeXml(option.code()))
                        .append("\"");

                xml.append(" label=\"")
                        .append(escapeXml(option.label() == null || option.label().isBlank()
                                ? humanReadableEnumName(option.code())
                                : option.label()))
                        .append("\"");

                xml.append("/>");
            }
        }

        xml.append("</lookup>");
    }

    private void appendLookup(
            StringBuilder xml,
            String lookupName,
            Class<? extends Enum<?>> enumType
    ) {
        xml.append("<lookup name=\"")
                .append(escapeXml(lookupName))
                .append("\">");

        Enum<?>[] enumConstants = enumType.getEnumConstants();

        if (enumConstants != null) {
            for (Enum<?> enumValue : enumConstants) {
                xml.append("<option");

                xml.append(" code=\"")
                        .append(escapeXml(enumCode(enumValue)))
                        .append("\"");

                xml.append(" label=\"")
                        .append(escapeXml(enumLabel(enumValue)))
                        .append("\"");

                xml.append("/>");
            }
        }

        xml.append("</lookup>");
    }

    private void appendCustomerListItem(
            StringBuilder xml,
            CustomerRecord customer
    ) {
        xml.append("<customer>");

        appendElement(xml, "customerId", customer == null ? null : customer.getCustomerId());
        appendElement(xml, "customerPK", customer == null ? null : customer.getCustomerPK());
        appendElement(xml, "version", customer == null ? null : customer.getVersion());
        appendElement(xml, "customerName", customer == null ? null : customer.getCustomerName());
        appendElement(xml, "cvrNumber", customer == null ? null : customer.getCvrNumber());
        appendElement(xml, "contactName", customer == null ? null : customer.getContactName());
        appendElement(xml, "contactEmail", customer == null ? null : customer.getContactEmail());
        appendElement(xml, "contactPhone", customer == null ? null : customer.getPhone());
        appendElement(xml, "customerStatus", customer == null ? null : customer.getCustomerStatusCode());
        appendElement(xml, "createdDateTime", customer == null ? null : customer.getCreatedDateTime());
        appendElement(xml, "changedDateTime", customer == null ? null : customer.getChangedDateTime());

        xml.append("</customer>");
    }

    private void appendCustomerDetail(
            StringBuilder xml,
            CustomerRecord customer
    ) {
        xml.append("<customer>");

        appendElement(xml, "customerId", customer == null ? null : customer.getCustomerId());
        appendElement(xml, "customerPK", customer == null ? null : customer.getCustomerPK());
        appendElement(xml, "version", customer == null ? null : customer.getVersion());
        appendElement(xml, "customerName", customer == null ? null : customer.getCustomerName());
        appendElement(xml, "cvrNumber", customer == null ? null : customer.getCvrNumber());
        appendElement(xml, "phone", customer == null ? null : customer.getPhone());
        appendElement(xml, "address", customer == null ? null : customer.getAddress());
        appendElement(xml, "zipCode", customer == null ? null : customer.getZipCode());
        appendElement(xml, "city", customer == null ? null : customer.getCity());
        appendElement(xml, "country", customer == null ? null : customer.getCountry());
        appendElement(xml, "contactName", customer == null ? null : customer.getContactName());
        appendElement(xml, "contactEmail", customer == null ? null : customer.getContactEmail());
        appendElement(xml, "customerStatus", customer == null ? null : customer.getCustomerStatusCode());
        appendElement(xml, "changedByUserId", customer == null ? null : customer.getChangedByUserId());
        appendElement(xml, "createdDateTime", customer == null ? null : customer.getCreatedDateTime());
        appendElement(xml, "changedDateTime", customer == null ? null : customer.getChangedDateTime());

        xml.append("</customer>");
    }

    private void appendSubscription(
            StringBuilder xml,
            CustomerSubscription subscription
    ) {
        xml.append("<subscription>");

        appendElement(xml, "subscriptionId", subscription == null ? null : subscription.getSubscriptionId());
        appendElement(xml, "customerId", subscription == null ? null : subscription.getCustomerId());
        appendElement(xml, "subscriptionStatus", subscription == null ? null : subscription.getSubscriptionStatusCode());
        appendElement(xml, "subscriptionPlanId", subscription == null ? null : subscription.getSubscriptionPlanId());
        appendElement(xml, "subscriptionPlanName", subscription == null ? null : subscription.getSubscriptionPlanName());
        appendElement(xml, "trialStartAt", subscription == null ? null : subscription.getTrialStartAt());
        appendElement(xml, "trialEndAt", subscription == null ? null : subscription.getTrialEndAt());
        appendElement(xml, "trialReminderSentAt", subscription == null ? null : subscription.getTrialReminderSentAt());
        appendElement(xml, "periodStartAt", subscription == null ? null : subscription.getPeriodStartAt());
        appendElement(xml, "periodEndAt", subscription == null ? null : subscription.getPeriodEndAt());
        appendElement(xml, "renewalReminderSentAt", subscription == null ? null : subscription.getRenewalReminderSentAt());
        appendElement(xml, "continuationConfirmedAt", subscription == null ? null : subscription.getContinuationConfirmedAt());
        appendElement(xml, "renewalConfirmedAt", subscription == null ? null : subscription.getRenewalConfirmedAt());
        appendElement(xml, "gracePeriodEndsAt", subscription == null ? null : subscription.getGracePeriodEndsAt());
        appendElement(xml, "createdAt", subscription == null ? null : subscription.getCreatedAt());
        appendElement(xml, "updatedAt", subscription == null ? null : subscription.getUpdatedAt());

        xml.append("</subscription>");
    }

    private void appendLatestPayment(
            StringBuilder xml,
            CustomerPayment payment
    ) {
        xml.append("<latestPayment>");

        appendElement(xml, "paymentId", payment == null ? null : payment.getPaymentId());
        appendElement(xml, "customerId", payment == null ? null : payment.getCustomerId());
        appendElement(xml, "subscriptionId", payment == null ? null : payment.getSubscriptionId());
        appendElement(xml, "paymentStatus", payment == null ? null : payment.getPaymentStatusCode());
        appendElement(xml, "paymentProvider", payment == null ? null : payment.getPaymentProvider());
        appendElement(xml, "paymentProviderReference", payment == null ? null : payment.getPaymentProviderReference());
        appendElement(xml, "amount", payment == null ? null : payment.getAmount());
        appendElement(xml, "currency", payment == null ? null : payment.getCurrency());
        appendElement(xml, "paymentDueAt", payment == null ? null : payment.getPaymentDueAt());
        appendElement(xml, "gracePeriodEndsAt", payment == null ? null : payment.getGracePeriodEndsAt());
        appendElement(xml, "requestedAt", payment == null ? null : payment.getRequestedAt());
        appendElement(xml, "authorizedAt", payment == null ? null : payment.getAuthorizedAt());
        appendElement(xml, "capturedAt", payment == null ? null : payment.getCapturedAt());
        appendElement(xml, "succeededAt", payment == null ? null : payment.getSucceededAt());
        appendElement(xml, "failedAt", payment == null ? null : payment.getFailedAt());
        appendElement(xml, "cancelledAt", payment == null ? null : payment.getCancelledAt());
        appendElement(xml, "failureReason", payment == null ? null : payment.getFailureReason());
        appendElement(xml, "createdAt", payment == null ? null : payment.getCreatedAt());
        appendElement(xml, "updatedAt", payment == null ? null : payment.getUpdatedAt());

        xml.append("</latestPayment>");
    }

    private void appendPaymentMethod(
            StringBuilder xml,
            CustomerPaymentMethod paymentMethod
    ) {
        xml.append("<paymentMethod>");

        appendElement(xml, "customerPaymentMethodId", paymentMethod == null ? null : paymentMethod.getCustomerPaymentMethodId());
        appendElement(xml, "customerId", paymentMethod == null ? null : paymentMethod.getCustomerId());
        appendElement(xml, "paymentProvider", paymentMethod == null ? null : paymentMethod.getPaymentProvider());
        appendElement(xml, "providerPaymentMethodReference", paymentMethod == null ? null : paymentMethod.getProviderPaymentMethodReference());
        appendElement(xml, "cardholderName", paymentMethod == null ? null : paymentMethod.getCardholderName());
        appendElement(xml, "cardBrand", paymentMethod == null ? null : paymentMethod.getCardBrand());
        appendElement(xml, "maskedCardNumber", paymentMethod == null ? null : paymentMethod.getMaskedCardNumber());
        appendElement(xml, "expiryMonth", paymentMethod == null ? null : paymentMethod.getExpiryMonth());
        appendElement(xml, "expiryYear", paymentMethod == null ? null : paymentMethod.getExpiryYear());
        appendElement(xml, "billingZipCode", paymentMethod == null ? null : paymentMethod.getBillingZipCode());
        appendElement(xml, "paymentMethodStatus", paymentMethod == null ? null : paymentMethod.getPaymentMethodStatusCode());
        appendElement(xml, "createdAt", paymentMethod == null ? null : paymentMethod.getCreatedAt());
        appendElement(xml, "updatedAt", paymentMethod == null ? null : paymentMethod.getUpdatedAt());

        xml.append("</paymentMethod>");
    }

    private void appendModules(
            StringBuilder xml,
            List<CustomerModule> modules
    ) {
        xml.append("<modules>");

        if (modules != null) {
            for (CustomerModule module : modules) {
                xml.append("<module>");

                appendElement(xml, "customerModuleId", module == null ? null : module.getCustomerModuleId());
                appendElement(xml, "customerId", module == null ? null : module.getCustomerId());
                appendElement(xml, "subscriptionPlanId", module == null ? null : module.getSubscriptionPlanId());
                appendElement(xml, "moduleCode", module == null ? null : module.getModuleCode());
                appendElement(xml, "moduleName", module == null ? null : module.getModuleName());
                appendElement(xml, "customerModuleStatus", module == null ? null : module.getCustomerModuleStatusCode());
                appendElement(xml, "createdAt", module == null ? null : module.getCreatedAt());
                appendElement(xml, "updatedAt", module == null ? null : module.getUpdatedAt());

                xml.append("</module>");
            }
        }

        xml.append("</modules>");
    }

    private void appendWorkflow(
            StringBuilder xml,
            CustomerWorkflow workflow
    ) {
        xml.append("<workflow>");

        appendElement(xml, "workflowId", workflow == null ? null : workflow.getWorkflowId());
        appendElement(xml, "customerId", workflow == null ? null : workflow.getCustomerId());
        appendElement(xml, "workflowType", workflow == null ? null : workflow.getWorkflowType());
        appendElement(xml, "workflowStatus", workflow == null ? null : workflow.getWorkflowStatusCode());
        appendElement(xml, "currentState", workflow == null ? null : workflow.getCurrentStateCode());
        appendElement(xml, "subscriptionId", workflow == null ? null : workflow.getSubscriptionId());
        appendElement(xml, "paymentId", workflow == null ? null : workflow.getPaymentId());
        appendElement(xml, "nextActionAt", workflow == null ? null : workflow.getNextActionAt());
        appendElement(xml, "retryCount", workflow == null ? null : workflow.getRetryCount());
        appendElement(xml, "lastEventType", workflow == null ? null : workflow.getLastEventType());
        appendElement(xml, "lastEventAt", workflow == null ? null : workflow.getLastEventAt());
        appendElement(xml, "lastError", workflow == null ? null : workflow.getLastError());
        appendElement(xml, "lockedAt", workflow == null ? null : workflow.getLockedAt());
        appendElement(xml, "lockedBy", workflow == null ? null : workflow.getLockedBy());
        appendElement(xml, "createdAt", workflow == null ? null : workflow.getCreatedAt());
        appendElement(xml, "updatedAt", workflow == null ? null : workflow.getUpdatedAt());

        xml.append("</workflow>");
    }

    private void appendWorkflowEvents(
            StringBuilder xml,
            List<CustomerWorkflowEvent> workflowEvents
    ) {
        xml.append("<workflowEvents>");

        if (workflowEvents != null) {
            for (CustomerWorkflowEvent event : workflowEvents) {
                xml.append("<event>");

                appendElement(xml, "workflowEventId", event == null ? null : event.getWorkflowEventId());
                appendElement(xml, "workflowId", event == null ? null : event.getWorkflowId());
                appendElement(xml, "customerId", event == null ? null : event.getCustomerId());
                appendElement(xml, "eventType", event == null ? null : event.getEventTypeCode());
                appendElement(xml, "eventCategory", event == null ? null : event.getEventCategory());
                appendElement(xml, "fromState", event == null ? null : event.getFromStateCode());
                appendElement(xml, "toState", event == null ? null : event.getToStateCode());
                appendElement(xml, "description", event == null ? null : event.getDescription());
                appendElement(xml, "payloadJson", event == null ? null : event.getPayloadJson());
                appendElement(xml, "createdAt", event == null ? null : event.getCreatedAt());
                appendElement(xml, "createdByUserId", event == null ? null : event.getCreatedByUserId());

                xml.append("</event>");
            }
        }

        xml.append("</workflowEvents>");
    }

    private Element firstElement(
            Document document,
            String tagName
    ) {
        if (document == null || tagName == null || tagName.isBlank()) {
            return null;
        }

        NodeList nodes = document.getElementsByTagName(tagName);

        if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element)) {
            return null;
        }

        return element;
    }

    private String text(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return "";
        }

        NodeList nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent() == null
                ? ""
                : nodes.item(0).getTextContent().trim();
    }

    private void appendXmlHeader(StringBuilder xml) {
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    private void appendElement(
            StringBuilder xml,
            String elementName,
            Object value
    ) {
        xml.append("<").append(elementName).append(">");

        if (value != null) {
            xml.append(escapeXml(toXmlValue(value)));
        }

        xml.append("</").append(elementName).append(">");
    }

    private String toXmlValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toPlainString();
        }

        return String.valueOf(value);
    }

    private String enumCode(Enum<?> enumValue) {
        if (enumValue == null) {
            return "";
        }

        Object reflectedValue = invokeNoArgumentMethod(
                enumValue,
                "getCode"
        );

        if (reflectedValue != null) {
            return String.valueOf(reflectedValue);
        }

        return enumValue.name();
    }

    private String enumLabel(Enum<?> enumValue) {
        if (enumValue == null) {
            return "";
        }

        Object reflectedValue = invokeNoArgumentMethod(
                enumValue,
                "getLabel"
        );

        if (reflectedValue != null) {
            return String.valueOf(reflectedValue);
        }

        return humanReadableEnumName(enumValue.name());
    }

    private Object invokeNoArgumentMethod(
            Enum<?> enumValue,
            String methodName
    ) {
        try {
            Method method = enumValue.getClass().getMethod(methodName);
            return method.invoke(enumValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String humanReadableEnumName(String enumName) {
        if (enumName == null || enumName.isBlank()) {
            return "";
        }

        String[] parts = enumName
                .trim()
                .toLowerCase(Locale.ENGLISH)
                .split("_");

        StringBuilder label = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!label.isEmpty()) {
                label.append(" ");
            }

            label.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1) {
                label.append(part.substring(1));
            }
        }

        return label.toString();
    }

    private String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Integer intValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal decimalValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private Timestamp timestampValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }

        normalized = normalized.replace("T", " ");

        try {
            return Timestamp.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void writeXml(
            HttpServletResponse response,
            int status,
            String xml
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(xml == null ? "" : xml);
    }

    private record SaveResult(
            boolean success,
            Integer customerId,
            String message
    ) {
        private static SaveResult ok(
                Integer customerId,
                String message
        ) {
            return new SaveResult(
                    true,
                    customerId,
                    message
            );
        }

        private static SaveResult failed(
                Integer customerId,
                String message
        ) {
            return new SaveResult(
                    false,
                    customerId,
                    message
            );
        }
    }

    private record LookupOption(
            String code,
            String label
    ) {
    }
}