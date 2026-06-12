package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.providers.customer.CustomerPaymentProvider;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import com.bepa.eis.server.api.web.application.admin.AbstractAdminServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "CustomerWorkflowStatusServlet", urlPatterns = {
        "/api/customer-workflow/status"
})
public class CustomerWorkflowStatusServlet extends AbstractAdminServlet {

    @Override
    public void processGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        processRequest(request, response);
    }

    @Override
    public void processPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        processRequest(request, response);
    }

    private void processRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Integer workflowId = ValueUtil.intValue(request.getParameter("workflowId"));
        Integer customerId = ValueUtil.intValue(request.getParameter("customerId"));

        if (workflowId == null && customerId == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"workflowId or customerId is required\"}"
            );
            return;
        }

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);

        CustomerWorkflow workflow = workflowId != null
                ? workflowProvider.getWorkflowById(workflowId)
                : workflowProvider.getActiveWorkflowByCustomerId(customerId);

        if (workflow == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "{\"success\":false,\"message\":\"Customer workflow was not found\"}"
            );
            return;
        }

        CustomerRecord customer = null;
        CustomerSubscription subscription = null;
        CustomerPayment payment = null;

        if (workflow.getCustomerId() != null) {
            CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(null);
            customer = customerRecordProvider.getLatestCustomerByCustomerId(workflow.getCustomerId());
        }

        if (workflow.getSubscriptionId() != null) {
            CustomerSubscriptionProvider subscriptionProvider = new CustomerSubscriptionProvider(null);
            subscription = subscriptionProvider.getSubscriptionById(workflow.getSubscriptionId());
        }

        if (workflow.getPaymentId() != null) {
            CustomerPaymentProvider paymentProvider = new CustomerPaymentProvider(null);
            payment = paymentProvider.getPaymentById(workflow.getPaymentId());
        }

        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_OK,
                buildJson(
                        customer,
                        workflow,
                        subscription,
                        payment
                )
        );
    }

    private String buildJson(
            CustomerRecord customer,
            CustomerWorkflow workflow,
            CustomerSubscription subscription,
            CustomerPayment payment
    ) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"success\":true");

        json.append(",");
        json.append("\"customer\":");

        if (customer == null) {
            json.append("null");
        } else {
            appendCustomerJson(json, customer);
        }

        json.append(",");
        json.append("\"workflow\":");
        appendWorkflowJson(json, workflow);

        json.append(",");
        json.append("\"subscription\":");

        if (subscription == null) {
            json.append("null");
        } else {
            appendSubscriptionJson(json, subscription);
        }

        json.append(",");
        json.append("\"payment\":");

        if (payment == null) {
            json.append("null");
        } else {
            appendPaymentJson(json, payment);
        }

        json.append("}");

        return json.toString();
    }

    private void appendCustomerJson(
            StringBuilder json,
            CustomerRecord customer
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "customerPK", customer.getCustomerPK());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", customer.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "version", customer.getVersion());
        json.append(",");
        JsonUtil.appendJsonString(json, "customerName", customer.getCustomerName());
        json.append(",");
        JsonUtil.appendJsonString(json, "cvrNumber", customer.getCvrNumber());
        json.append(",");
        JsonUtil.appendJsonString(json, "phone", customer.getPhone());
        json.append(",");
        JsonUtil.appendJsonString(json, "address", customer.getAddress());
        json.append(",");
        JsonUtil.appendJsonString(json, "zipCode", customer.getZipCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "city", customer.getCity());
        json.append(",");
        JsonUtil.appendJsonString(json, "country", customer.getCountry());
        json.append(",");
        JsonUtil.appendJsonString(json, "contactName", customer.getContactName());
        json.append(",");
        JsonUtil.appendJsonString(json, "contactEmail", customer.getContactEmail());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerStatusId", customer.getCustomerStatusId());
        json.append(",");
        JsonUtil.appendJsonString(json, "customerStatusCode", customer.getCustomerStatusCode());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "customerLoginAllowed", customer.isLoginAllowedByDefault());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "changedByUserId", customer.getChangedByUserId());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "changedDateTime", customer.getChangedDateTime());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "latest", customer.isLatest());
        json.append("}");
    }

    private void appendWorkflowJson(
            StringBuilder json,
            CustomerWorkflow workflow
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "workflowId", workflow.getWorkflowId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", workflow.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonString(json, "workflowType", workflow.getWorkflowType());
        json.append(",");
        JsonUtil.appendJsonString(json, "workflowStatus", workflow.getWorkflowStatusCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "currentState", workflow.getCurrentStateCode());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "subscriptionId", workflow.getSubscriptionId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "paymentId", workflow.getPaymentId());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "nextActionAt", workflow.getNextActionAt());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "retryCount", workflow.getRetryCount());
        json.append(",");
        JsonUtil.appendJsonString(json, "lastEventType", workflow.getLastEventType());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "lastEventAt", workflow.getLastEventAt());
        json.append(",");
        JsonUtil.appendJsonString(json, "lastError", workflow.getLastError());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "lockedAt", workflow.getLockedAt());
        json.append(",");
        JsonUtil.appendJsonString(json, "lockedBy", workflow.getLockedBy());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "createdAt", workflow.getCreatedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "updatedAt", workflow.getUpdatedAt());
        json.append("}");
    }

    private void appendSubscriptionJson(
            StringBuilder json,
            CustomerSubscription subscription
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "subscriptionId", subscription.getSubscriptionId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", subscription.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonString(json, "subscriptionStatus", subscription.getSubscriptionStatusCode());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "subscriptionPlanId", subscription.getSubscriptionPlanId());
        json.append(",");
        JsonUtil.appendJsonString(json, "subscriptionPlanName", subscription.getSubscriptionPlanName());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "trialStartAt", subscription.getTrialStartAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "trialEndAt", subscription.getTrialEndAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "trialReminderSentAt", subscription.getTrialReminderSentAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "periodStartAt", subscription.getPeriodStartAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "periodEndAt", subscription.getPeriodEndAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "renewalReminderSentAt", subscription.getRenewalReminderSentAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "continuationConfirmedAt", subscription.getContinuationConfirmedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "renewalConfirmedAt", subscription.getRenewalConfirmedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "gracePeriodEndsAt", subscription.getGracePeriodEndsAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "createdAt", subscription.getCreatedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "updatedAt", subscription.getUpdatedAt());
        json.append("}");
    }

    private void appendPaymentJson(
            StringBuilder json,
            CustomerPayment payment
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "paymentId", payment.getPaymentId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", payment.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "subscriptionId", payment.getSubscriptionId());
        json.append(",");
        JsonUtil.appendJsonString(json, "paymentStatus", payment.getPaymentStatusCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "paymentProvider", payment.getPaymentProvider());
        json.append(",");
        JsonUtil.appendJsonString(json, "paymentProviderReference", payment.getPaymentProviderReference());
        json.append(",");
        JsonUtil.appendJsonString(json, "amount", String.valueOf(payment.getAmount()));
        json.append(",");
        JsonUtil.appendJsonString(json, "currency", payment.getCurrency());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "paymentDueAt", payment.getPaymentDueAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "gracePeriodEndsAt", payment.getGracePeriodEndsAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "requestedAt", payment.getRequestedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "authorizedAt", payment.getAuthorizedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "capturedAt", payment.getCapturedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "succeededAt", payment.getSucceededAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "failedAt", payment.getFailedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "cancelledAt", payment.getCancelledAt());
        json.append(",");
        JsonUtil.appendJsonString(json, "failureReason", payment.getFailureReason());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "createdAt", payment.getCreatedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "updatedAt", payment.getUpdatedAt());
        json.append("}");
    }

}