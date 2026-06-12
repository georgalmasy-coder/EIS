package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.dto.customer.CustomerWorkflowEvent;
import com.bepa.eis.common.providers.customer.CustomerWorkflowEventProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CustomerWorkflowEventsServlet", urlPatterns = {
        "/api/customer-workflow/events"
})
public class CustomerWorkflowEventsServlet extends HttpServlet {

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
        Integer workflowId = ValueUtil.intValue(request.getParameter("workflowId"));
        Integer customerId = ValueUtil.intValue(request.getParameter("customerId"));
        Integer maxRows = ValueUtil.intValue(request.getParameter("maxRows"));

        int safeMaxRows = maxRows == null ? 100 : Math.max(1, Math.min(maxRows, 1000));

        if (workflowId == null && customerId == null) {
            JsonUtil.writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"workflowId or customerId is required\"}"
            );
            return;
        }

        CustomerWorkflowEventProvider provider = new CustomerWorkflowEventProvider(null);

        List<CustomerWorkflowEvent> events = workflowId != null
                ? provider.getWorkflowEventsByWorkflowId(workflowId, safeMaxRows)
                : provider.getWorkflowEventsByCustomerId(customerId, safeMaxRows);

        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_OK,
                buildJson(events)
        );
    }

    private String buildJson(List<CustomerWorkflowEvent> events) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"success\":true");
        json.append(",");
        json.append("\"count\":").append(events == null ? 0 : events.size());
        json.append(",");
        json.append("\"events\":[");

        if (events != null) {
            for (int index = 0; index < events.size(); index++) {
                if (index > 0) {
                    json.append(",");
                }

                appendEventJson(
                        json,
                        events.get(index)
                );
            }
        }

        json.append("]");
        json.append("}");

        return json.toString();
    }

    private void appendEventJson(
            StringBuilder json,
            CustomerWorkflowEvent event
    ) {
        json.append("{");
        JsonUtil.appendJsonNumber(json, "workflowEventId", event.getWorkflowEventId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "workflowId", event.getWorkflowId());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "customerId", event.getCustomerId());
        json.append(",");
        JsonUtil.appendJsonString(json, "eventType", event.getEventTypeCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "eventCategory", event.getEventCategory());
        json.append(",");
        JsonUtil.appendJsonString(json, "fromState", event.getFromStateCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "toState", event.getToStateCode());
        json.append(",");
        JsonUtil.appendJsonString(json, "description", event.getDescription());
        json.append(",");
        JsonUtil.appendJsonRawOrString(json, "payloadJson", event.getPayloadJson());
        json.append(",");
        JsonUtil.appendJsonTimestamp(json, "createdAt", event.getCreatedAt());
        json.append(",");
        JsonUtil.appendJsonNumber(json, "createdByUserId", event.getCreatedByUserId());
        json.append("}");
    }

}