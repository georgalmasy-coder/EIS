package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import com.bepa.eis.common.providers.customer.CustomerPaymentProvider;
import com.bepa.eis.common.providers.customer.CustomerSubscriptionActivationProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import com.bepa.eis.common.utilities.HtmlUtil;
import com.bepa.eis.server.api.web.application.admin.AbstractAdminServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "CustomerPaymentCallbackServlet", urlPatterns = {
        "/api/customer-payment/callback"
})
public class CustomerPaymentCallbackServlet extends AbstractAdminServlet {

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
        Integer paymentId = intValue(request.getParameter("paymentId"));
        String status = safeText(request.getParameter("status"));
        String failureReason = safeText(request.getParameter("failureReason"));

        if (paymentId == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"paymentId is required\"}"
            );
            return;
        }

        CustomerPaymentStatus paymentStatus = CustomerPaymentStatus.fromCode(status);

        if (paymentStatus == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"valid status is required\"}"
            );
            return;
        }

        CustomerPaymentProvider paymentProvider = new CustomerPaymentProvider(null);
        CustomerPayment payment = paymentProvider.getPaymentById(paymentId);

        if (payment == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "{\"success\":false,\"message\":\"Payment was not found\"}"
            );
            return;
        }

        boolean paymentUpdated = paymentProvider.updatePaymentStatus(
                paymentId,
                paymentStatus,
                failureReason
        );

        if (!paymentUpdated) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Payment status could not be updated\"}"
            );
            return;
        }

        updateSubscriptionStatusFromPayment(
                payment,
                paymentStatus
        );

        CustomerWorkflow workflow = findRelatedWorkflow(payment);

        if (workflow != null) {
            wakeWorkflow(
                    workflow,
                    paymentStatus,
                    failureReason
            );
        }

        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_OK,
                buildSuccessJson(paymentId, paymentStatus, workflow)
        );
    }

    private void updateSubscriptionStatusFromPayment(
            CustomerPayment payment,
            CustomerPaymentStatus paymentStatus
    ) {
        if (payment == null || payment.getSubscriptionId() == null || paymentStatus == null) {
            return;
        }

        CustomerSubscriptionActivationProvider activationProvider = new CustomerSubscriptionActivationProvider(null);

        if (paymentStatus.isSuccessfulStatus()) {
            activationProvider.activateSubscription(payment.getSubscriptionId());
            return;
        }

        if (paymentStatus.isFailedStatus()) {
            activationProvider.markSubscriptionPaymentOverdue(payment.getSubscriptionId());
        }
    }

    private CustomerWorkflow findRelatedWorkflow(CustomerPayment payment) {
        if (payment == null || payment.getCustomerId() == null) {
            return null;
        }

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflow workflow = workflowProvider.getActiveWorkflowByCustomerId(payment.getCustomerId());

        if (workflow == null) {
            return null;
        }

        if (workflow.getPaymentId() == null && payment.getPaymentId() != null) {
            workflow.setPaymentId(payment.getPaymentId());
        }

        return workflow;
    }

    private void wakeWorkflow(
            CustomerWorkflow workflow,
            CustomerPaymentStatus paymentStatus,
            String failureReason
    ) {
        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflowTimingProvider timingProvider = new CustomerWorkflowTimingProvider();

        CustomerWorkflowState fromState = workflow.getCurrentState();

        workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
        workflow.setNextActionAt(timingProvider.now());
        workflow.setLastError(paymentStatus.isFailedStatus() ? failureReason : "");

        CustomerWorkflowEventType eventType = toWorkflowEventType(paymentStatus);

        workflowProvider.updateWorkflowState(
                workflow,
                eventType,
                workflow.getLastError()
        );

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                eventType,
                fromState,
                workflow.getCurrentState(),
                "Payment callback received with status " + paymentStatus.getCode() + ".",
                "{}",
                null
        );

        workflowProvider.createWorkflowEvent(event);
    }

    private CustomerWorkflowEventType toWorkflowEventType(CustomerPaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            return CustomerWorkflowEventType.PAYMENT_REQUESTED;
        }

        if (paymentStatus.isSuccessfulStatus()) {
            return CustomerWorkflowEventType.PAYMENT_SUCCEEDED;
        }

        if (paymentStatus == CustomerPaymentStatus.CANCELLED) {
            return CustomerWorkflowEventType.PAYMENT_CANCELLED;
        }

        if (paymentStatus == CustomerPaymentStatus.TIMED_OUT || paymentStatus == CustomerPaymentStatus.EXPIRED) {
            return CustomerWorkflowEventType.PAYMENT_TIMED_OUT;
        }

        if (paymentStatus.isFailedStatus()) {
            return CustomerWorkflowEventType.PAYMENT_FAILED;
        }

        return CustomerWorkflowEventType.PAYMENT_REQUESTED;
    }

    private String buildSuccessJson(
            Integer paymentId,
            CustomerPaymentStatus paymentStatus,
            CustomerWorkflow workflow
    ) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"success\":true");
        json.append(",");
        json.append("\"paymentId\":").append(paymentId);
        json.append(",");
        json.append("\"paymentStatus\":\"").append(JsonUtil.escapeJson(paymentStatus.getCode())).append("\"");
        json.append(",");
        json.append("\"workflowId\":");

        if (workflow == null || workflow.getWorkflowId() == null) {
            json.append("null");
        } else {
            json.append(workflow.getWorkflowId());
        }

        json.append(",");
        json.append("\"customerId\":");

        if (workflow == null || workflow.getCustomerId() == null) {
            json.append("null");
        } else {
            json.append(workflow.getCustomerId());
        }

        json.append("}");

        return json.toString();
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }


}