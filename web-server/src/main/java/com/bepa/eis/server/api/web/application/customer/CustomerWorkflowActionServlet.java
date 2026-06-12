package com.bepa.eis.server.api.web.application.customer;

import com.bepa.eis.common.providers.customer.CustomerWorkflowActionProvider;
import com.bepa.eis.common.providers.customer.CustomerWorkflowActionProvider.CustomerWorkflowActionResult;
import com.bepa.eis.common.utilities.HtmlUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "CustomerWorkflowActionServlet", urlPatterns = {
        "/customer-workflow-action",
        "/confirm-customer",
        "/continue-subscription",
        "/renew-subscription",
        "/reactivate-customer"
})
public class CustomerWorkflowActionServlet extends HttpServlet {

    private static final String ACTION_CONFIRM_CUSTOMER = "confirm-customer";
    private static final String ACTION_CONTINUE_SUBSCRIPTION = "continue-subscription";
    private static final String ACTION_RENEW_SUBSCRIPTION = "renew-subscription";
    private static final String ACTION_REACTIVATE_CUSTOMER = "reactivate-customer";

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        processAction(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        processAction(request, response);
    }

    private void processAction(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String action = resolveAction(request);
        String token = ValueUtil.safeText(request.getParameter("token"));

        CustomerWorkflowActionResult result = executeAction(
                action,
                token
        );

        writeHtmlResponse(
                response,
                action,
                result
        );
    }

    private String resolveAction(HttpServletRequest request) {
        String actionParameter = ValueUtil.safeText(request.getParameter("action"));

        if (!actionParameter.isEmpty()) {
            return actionParameter;
        }

        String servletPath = ValueUtil.safeText(request.getServletPath());

        if (servletPath.startsWith("/")) {
            servletPath = servletPath.substring(1);
        }

        return servletPath;
    }

    private CustomerWorkflowActionResult executeAction(
            String action,
            String token
    ) {
        if (action.isEmpty()) {
            return CustomerWorkflowActionResult.failed("Action is missing.");
        }

        if (token.isEmpty()) {
            return CustomerWorkflowActionResult.failed("Token is missing.");
        }

        CustomerWorkflowActionProvider actionProvider = new CustomerWorkflowActionProvider();

        return switch (action) {
            case ACTION_CONFIRM_CUSTOMER -> actionProvider.confirmCustomerEmail(token);
            case ACTION_CONTINUE_SUBSCRIPTION -> actionProvider.confirmSubscriptionContinuation(token);
            case ACTION_RENEW_SUBSCRIPTION -> actionProvider.confirmSubscriptionRenewal(token);
            case ACTION_REACTIVATE_CUSTOMER -> actionProvider.reactivateCustomer(token);
            default -> CustomerWorkflowActionResult.failed("Unsupported action: " + action);
        };
    }

    private void writeHtmlResponse(
            HttpServletResponse response,
            String action,
            CustomerWorkflowActionResult result
    ) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");

        boolean success = result != null && result.isSuccess();

        String title = success
                ? successTitle(action)
                : "The request could not be completed";

        String message = result == null
                ? "The request could not be completed."
                : result.getMessage();

        String html = buildHtml(
                title,
                message,
                success
        );

        response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(html);
    }

    private String successTitle(String action) {
        return switch (ValueUtil.safeText(action)) {
            case ACTION_CONFIRM_CUSTOMER -> "Account confirmed";
            case ACTION_CONTINUE_SUBSCRIPTION -> "Subscription continuation confirmed";
            case ACTION_RENEW_SUBSCRIPTION -> "Subscription renewal confirmed";
            case ACTION_REACTIVATE_CUSTOMER -> "Account reactivated";
            default -> "Action completed";
        };
    }

    private String buildHtml(
            String title,
            String message,
            boolean success
    ) {
        String color = success ? "#0f7b4f" : "#9b1c1c";
        String escapedTitle = HtmlUtil.escapeHtml(title);
        String escapedMessage = HtmlUtil.escapeHtml(message);

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background: #f4f6f8;
                            font-family: Arial, Helvetica, sans-serif;
                            color: #1f2933;
                        }
                        .page {
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 24px;
                        }
                        .card {
                            width: 640px;
                            max-width: 100%%;
                            background: #ffffff;
                            border: 1px solid #d9e2ec;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 10px 24px rgba(16, 42, 67, 0.08);
                        }
                        .header {
                            background: %s;
                            color: #ffffff;
                            padding: 24px 32px;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 22px;
                            line-height: 1.3;
                        }
                        .content {
                            padding: 32px;
                        }
                        .content p {
                            margin: 0 0 16px 0;
                            font-size: 16px;
                            line-height: 1.5;
                        }
                        .footer {
                            padding: 20px 32px;
                            background: #f8fafc;
                            border-top: 1px solid #d9e2ec;
                            color: #7b8794;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="card">
                            <div class="header">
                                <h1>%s</h1>
                            </div>
                            <div class="content">
                                <p>%s</p>
                                <p>You may now close this page.</p>
                            </div>
                            <div class="footer">
                                This page was generated automatically by BEPA EIS.
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapedTitle,
                color,
                escapedTitle,
                escapedMessage
        );
    }

}