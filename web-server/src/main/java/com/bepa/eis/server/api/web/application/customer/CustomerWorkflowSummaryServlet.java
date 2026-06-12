package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflowSummary;
import com.bepa.eis.common.providers.customer.CustomerWorkflowSummaryProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CustomerWorkflowSummaryServlet", urlPatterns = {
        "/api/customer-workflow/summary"
})
public class CustomerWorkflowSummaryServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Integer customerId = ValueUtil.intValue(request.getParameter("customerId"));
        String status = ValueUtil.safeText(request.getParameter("status"));
        Integer maxRows = ValueUtil.intValue(request.getParameter("maxRows"));
        int safeMaxRows = maxRows == null ? 100 : Math.max(1, Math.min(maxRows, 1000));

        CustomerWorkflowSummaryProvider provider = new CustomerWorkflowSummaryProvider(null);

        List<CustomerWorkflowSummary> summaries;

        if (customerId != null) {
            summaries = provider.getWorkflowSummariesByCustomerId(
                    customerId,
                    safeMaxRows
            );
        } else if (!status.isEmpty()) {
            summaries = provider.getWorkflowSummariesByStatus(
                    status,
                    safeMaxRows
            );
        } else {
            summaries = provider.getLatestWorkflowSummaries(safeMaxRows);
        }

        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_OK,
                buildJson(summaries)
        );
    }

    private String buildJson(List<CustomerWorkflowSummary> summaries) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"success\":true");
        json.append(",");
        json.append("\"count\":").append(summaries == null ? 0 : summaries.size());
        json.append(",");
        json.append("\"items\":[");

        if (summaries != null) {
            for (int index = 0; index < summaries.size(); index++) {
                if (index > 0) {
                    json.append(",");
                }

                appendSummaryJson(
                        json,
                        summaries.get(index)
                );
            }
        }

        json.append("]");
        json.append("}");

        return json.toString();
    }

    private void appendSummaryJson(
            StringBuilder json,
            CustomerWorkflowSummary summary
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "workflowId", summary.getWorkflowId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", summary.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonString(json, "customerName", summary.getCustomerName());
        json.append(",");
        JsonUtil.appendJsonString(json, "contactEmail", summary.getContactEmail());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerStatusId", summary.getCustomerStatusId());
        json.append(",");
        JsonUtil.appendJsonString(json, "customerStatusCode", summary.getCustomerStatusCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "customerStatusLabel", summary.getCustomerStatusLabel());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "customerLoginAllowed", summary.isCustomerLoginAllowedByDefault());
        json.append(",");
        JsonUtil.appendJsonString(json, "workflowType", summary.getWorkflowType());
        json.append(",");
        JsonUtil.appendJsonString(json, "workflowStatus", summary.getWorkflowStatus());
        json.append(",");
        JsonUtil.appendJsonString(json, "currentState", summary.getCurrentState());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "subscriptionId", summary.getSubscriptionId());
        json.append(",");
        JsonUtil.appendJsonString(json, "subscriptionStatus", summary.getSubscriptionStatus());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "trialEndAt", summary.getTrialEndAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "periodEndAt", summary.getPeriodEndAt());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "paymentId", summary.getPaymentId());
        json.append(",");
        JsonUtil.appendJsonString(json, "paymentStatus", summary.getPaymentStatus());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "paymentDueAt", summary.getPaymentDueAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "paymentGracePeriodEndsAt", summary.getPaymentGracePeriodEndsAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "nextActionAt", summary.getNextActionAt());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "retryCount", summary.getRetryCount());
        json.append(",");
        JsonUtil.appendJsonString(json, "lastEventType", summary.getLastEventType());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "lastEventAt", summary.getLastEventAt());
        json.append(",");
        JsonUtil.appendJsonString(json, "lastError", summary.getLastError());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "lockedAt", summary.getLockedAt());
        json.append(",");
        JsonUtil.appendJsonString(json, "lockedBy", summary.getLockedBy());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "createdAt", summary.getCreatedAt());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "updatedAt", summary.getUpdatedAt());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "hasError", summary.hasError());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "locked", summary.isLocked());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "requiresManualAttention", summary.requiresManualAttention());
        json.append(",");
        JsonUtil.appendJsonBoolean(json, "suspended", summary.isSuspended());
        json.append("}");
    }
}