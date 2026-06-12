package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.providers.customer.CustomerWorkflowStarterProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import com.bepa.eis.server.api.web.application.admin.AbstractAdminServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "CustomerWorkflowStartServlet", urlPatterns = {
        "/api/customer-workflow/start"
})
public class CustomerWorkflowStartServlet extends AbstractAdminServlet {

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
        Integer customerId = ValueUtil.intValue(request.getParameter("customerId"));

        if (customerId == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"customerId is required\"}"
            );
            return;
        }

        CustomerWorkflowStarterProvider starterProvider = new CustomerWorkflowStarterProvider();

        Integer workflowId = starterProvider.startCustomerOnboardingWorkflow(
                customerId,
                null,
                "Customer onboarding workflow started from API."
        );

        if (workflowId == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Workflow could not be started\"}"
            );
            return;
        }

        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_OK,
                "{\"success\":true,\"customerId\":" + customerId + ",\"workflowId\":" + workflowId + "}"
        );
    }

}