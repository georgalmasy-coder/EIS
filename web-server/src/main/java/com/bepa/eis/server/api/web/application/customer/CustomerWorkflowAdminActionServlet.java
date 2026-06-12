package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflow;
import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.customer.CustomerWorkflowEventType;
import com.bepa.eis.common.enums.customer.CustomerWorkflowState;
import com.bepa.eis.common.enums.customer.CustomerWorkflowStatus;
import com.bepa.eis.common.providers.customer.CustomerWorkflowTimingProvider;
import com.bepa.eis.common.providers.customer.CustomerAccessProvider;
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

@WebServlet(name = "CustomerWorkflowAdminActionServlet", urlPatterns = {
        "/api/customer-workflow/admin-action"
})
public class CustomerWorkflowAdminActionServlet extends AbstractAdminServlet {

    private static final String ACTION_SUSPEND = "suspend";
    private static final String ACTION_REACTIVATE = "reactivate";
    private static final String ACTION_RETRY = "retry";
    private static final String ACTION_CANCEL = "cancel";

    @Override
    public void processGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
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
        String action = ValueUtil.safeText(request.getParameter("action"));
        Integer workflowId = ValueUtil.intValue(request.getParameter("workflowId"));
        Integer customerId = ValueUtil.intValue(request.getParameter("customerId"));
        String reason = ValueUtil.safeText(request.getParameter("reason"));

        ActionResult result = executeAction(
                action,
                workflowId,
                customerId,
                reason
        );

        JsonUtil.writeJson(
                response,
                result.isSuccess() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                result.toJson()
        );
    }

    private ActionResult executeAction(
            String action,
            Integer workflowId,
            Integer customerId,
            String reason
    ) {
        if (action.isEmpty()) {
            return ActionResult.failed("action is required");
        }

        return switch (action) {
            case ACTION_SUSPEND -> suspendCustomer(customerId, reason);
            case ACTION_REACTIVATE -> reactivateCustomer(customerId);
            case ACTION_RETRY -> retryWorkflow(workflowId);
            case ACTION_CANCEL -> cancelWorkflow(workflowId, reason);
            default -> ActionResult.failed("Unsupported action: " + action);
        };
    }

    private ActionResult suspendCustomer(
            Integer customerId,
            String reason
    ) {
        if (customerId == null) {
            return ActionResult.failed("customerId is required");
        }

        String safeReason = reason.isEmpty()
                ? "Customer manually suspended."
                : reason;

        CustomerAccessProvider accessProvider = new CustomerAccessProvider(null);
        boolean suspended = accessProvider.suspendCustomer(
                customerId,
                safeReason
        );

        if (!suspended) {
            return ActionResult.failed("Customer could not be suspended");
        }

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflow workflow = workflowProvider.getActiveWorkflowByCustomerId(customerId);

        if (workflow != null) {
            CustomerWorkflowState fromState = workflow.getCurrentState();

            workflow.setWorkflowStatus(CustomerWorkflowStatus.SUSPENDED);
            workflow.setCurrentState(CustomerWorkflowState.SUSPENDED);
            workflow.setNextActionAt(null);
            workflow.setLastError(safeReason);

            workflowProvider.updateWorkflowState(
                    workflow,
                    CustomerWorkflowEventType.CUSTOMER_MANUALLY_SUSPENDED,
                    safeReason
            );

            createEvent(
                    workflowProvider,
                    workflow,
                    CustomerWorkflowEventType.CUSTOMER_MANUALLY_SUSPENDED,
                    fromState,
                    CustomerWorkflowState.SUSPENDED,
                    safeReason
            );
        }

        return ActionResult.success(
                "Customer suspended",
                workflow == null ? null : workflow.getWorkflowId(),
                customerId
        );
    }

    private ActionResult reactivateCustomer(Integer customerId) {
        if (customerId == null) {
            return ActionResult.failed("customerId is required");
        }

        CustomerAccessProvider accessProvider = new CustomerAccessProvider(null);
        boolean reactivated = accessProvider.reactivateCustomer(customerId);

        if (!reactivated) {
            return ActionResult.failed("Customer could not be reactivated");
        }

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflow workflow = workflowProvider.getActiveWorkflowByCustomerId(customerId);

        if (workflow != null) {
            CustomerWorkflowState fromState = workflow.getCurrentState();

            workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
            workflow.setCurrentState(CustomerWorkflowState.SUBSCRIPTION_ACTIVE);
            workflow.setNextActionAt(new CustomerWorkflowTimingProvider().now());
            workflow.setLastError("");

            workflowProvider.updateWorkflowState(
                    workflow,
                    CustomerWorkflowEventType.CUSTOMER_MANUALLY_REACTIVATED,
                    ""
            );

            createEvent(
                    workflowProvider,
                    workflow,
                    CustomerWorkflowEventType.CUSTOMER_MANUALLY_REACTIVATED,
                    fromState,
                    CustomerWorkflowState.SUBSCRIPTION_ACTIVE,
                    "Customer manually reactivated."
            );
        }

        return ActionResult.success(
                "Customer reactivated",
                workflow == null ? null : workflow.getWorkflowId(),
                customerId
        );
    }

    private ActionResult retryWorkflow(Integer workflowId) {
        if (workflowId == null) {
            return ActionResult.failed("workflowId is required");
        }

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflow workflow = workflowProvider.getWorkflowById(workflowId);

        if (workflow == null) {
            return ActionResult.failed("Workflow was not found");
        }

        CustomerWorkflowState fromState = workflow.getCurrentState();

        workflow.setWorkflowStatus(CustomerWorkflowStatus.ACTIVE);
        workflow.setNextActionAt(new CustomerWorkflowTimingProvider().now());
        workflow.setLastError("");
        workflow.clearLock();

        boolean updated = workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.WORKFLOW_RETRY_REQUESTED,
                ""
        );

        if (!updated) {
            return ActionResult.failed("Workflow could not be retried");
        }

        createEvent(
                workflowProvider,
                workflow,
                CustomerWorkflowEventType.WORKFLOW_RETRY_REQUESTED,
                fromState,
                workflow.getCurrentState(),
                "Workflow retry requested."
        );

        return ActionResult.success(
                "Workflow retry requested",
                workflow.getWorkflowId(),
                workflow.getCustomerId()
        );
    }

    private ActionResult cancelWorkflow(
            Integer workflowId,
            String reason
    ) {
        if (workflowId == null) {
            return ActionResult.failed("workflowId is required");
        }

        String safeReason = reason.isEmpty()
                ? "Workflow manually cancelled."
                : reason;

        CustomerWorkflowProvider workflowProvider = new CustomerWorkflowProvider(null);
        CustomerWorkflow workflow = workflowProvider.getWorkflowById(workflowId);

        if (workflow == null) {
            return ActionResult.failed("Workflow was not found");
        }

        CustomerWorkflowState fromState = workflow.getCurrentState();

        CustomerAccessProvider accessProvider = new CustomerAccessProvider(null);
        accessProvider.setCustomerStatus(
                workflow.getCustomerId(),
                CustomerStatus.CANCELLED
        );

        workflow.setWorkflowStatus(CustomerWorkflowStatus.CANCELLED);
        workflow.setCurrentState(CustomerWorkflowState.CANCELLED);
        workflow.setNextActionAt(null);
        workflow.setLastError(safeReason);

        boolean updated = workflowProvider.updateWorkflowState(
                workflow,
                CustomerWorkflowEventType.CUSTOMER_MANUALLY_CANCELLED,
                safeReason
        );

        if (!updated) {
            return ActionResult.failed("Workflow could not be cancelled");
        }

        createEvent(
                workflowProvider,
                workflow,
                CustomerWorkflowEventType.CUSTOMER_MANUALLY_CANCELLED,
                fromState,
                CustomerWorkflowState.CANCELLED,
                safeReason
        );

        return ActionResult.success(
                "Workflow cancelled",
                workflow.getWorkflowId(),
                workflow.getCustomerId()
        );
    }

    private void createEvent(
            CustomerWorkflowProvider workflowProvider,
            CustomerWorkflow workflow,
            CustomerWorkflowEventType eventType,
            CustomerWorkflowState fromState,
            CustomerWorkflowState toState,
            String description
    ) {
        if (workflowProvider == null || workflow == null) {
            return;
        }

        CustomerWorkflowEvent event = CustomerWorkflowEvent.create(
                workflow.getWorkflowId(),
                workflow.getCustomerId(),
                eventType,
                fromState,
                toState,
                description,
                "{}",
                null
        );

        workflowProvider.createWorkflowEvent(event);
    }

    private static class ActionResult {

        private final boolean success;
        private final String message;
        private final Integer workflowId;
        private final Integer customerId;

        private ActionResult(
                boolean success,
                String message,
                Integer workflowId,
                Integer customerId
        ) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.workflowId = workflowId;
            this.customerId = customerId;
        }

        private static ActionResult success(
                String message,
                Integer workflowId,
                Integer customerId
        ) {
            return new ActionResult(
                    true,
                    message,
                    workflowId,
                    customerId
            );
        }

        private static ActionResult failed(String message) {
            return new ActionResult(
                    false,
                    message,
                    null,
                    null
            );
        }

        private boolean isSuccess() {
            return success;
        }

        private String toJson() {
            StringBuilder json = new StringBuilder();

            json.append("{");
            json.append("\"success\":").append(success);
            json.append(",");
            json.append("\"message\":\"").append(JsonUtil.escapeJson(message)).append("\"");
            json.append(",");
            json.append("\"workflowId\":");

            if (workflowId == null) {
                json.append("null");
            } else {
                json.append(workflowId);
            }

            json.append(",");
            json.append("\"customerId\":");

            if (customerId == null) {
                json.append("null");
            } else {
                json.append(customerId);
            }

            json.append("}");

            return json.toString();
        }
}
}