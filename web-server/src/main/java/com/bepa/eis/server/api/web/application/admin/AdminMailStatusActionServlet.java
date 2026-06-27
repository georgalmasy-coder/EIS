package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.mail.MailQueueItem;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.enums.mail.MailStatus;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.common.utilities.ValueUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "AdminMailStatusActionServlet", urlPatterns = {
        "/admin/api/dashboard/mail-status-action"
})
public class AdminMailStatusActionServlet extends AbstractAdminServlet {

    private static final String ACTION_RESEND = "resend";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void processGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendUnsupportedAction(response);
    }

    @Override
    public void processPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = ValueUtil.safeText(request.getParameter("action"));
        Integer mailId = ValueUtil.intValue(request.getParameter("mailId"));
        String recipientEmail = ValueUtil.safeText(request.getParameter("recipientEmail"));

        ActionResult result = executeAction(
                request,
                action,
                mailId,
                recipientEmail
        );

        JsonUtil.writeJson(
                response,
                result.isSuccess() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                result.toJson()
        );
    }

    private ActionResult executeAction(
            HttpServletRequest request,
            String action,
            Integer mailId,
            String recipientEmail
    ) {
        if (action.isEmpty()) {
            return ActionResult.failed("action is required");
        }

        return switch (action) {
            case ACTION_RESEND -> resendMail(request, mailId, recipientEmail);
            default -> ActionResult.failed("Unsupported action: " + action);
        };
    }

    private ActionResult resendMail(
            HttpServletRequest request,
            Integer mailId,
            String recipientEmail
    ) {
        if (mailId == null) {
            return ActionResult.failed("mailId is required");
        }

        WebSession webSession = getWebSessionFromRequest(request, false);
        MailProvider mailProvider = new MailProvider(webSession);
        MailQueueItem originalMail = mailProvider.getMailById(mailId);

        if (originalMail == null) {
            return ActionResult.failed("Mail was not found");
        }

        MailStatus status = originalMail.getStatus();
        if (status != MailStatus.FAILED && status != MailStatus.UNDELIVERED) {
            return ActionResult.failed("Only failed mails can be resent");
        }

        String safeRecipientEmail = recipientEmail == null || recipientEmail.trim().isEmpty()
                ? originalMail.getToEmail()
                : recipientEmail.trim();

        MailRecipient from = MailRecipient.of(
                originalMail.getFromName(),
                originalMail.getFromEmail()
        );
        MailRecipient to = MailRecipient.of(
                originalMail.getToName(),
                safeRecipientEmail
        );

        if (!to.isValid()) {
            return ActionResult.failed("Recipient email is invalid");
        }

        Map<String, Object> parameters = parseParameters(originalMail.getParametersJson());
        parameters.put("fromName", from.getName());
        parameters.put("fromEmail", from.getEmail());
        parameters.put("toName", to.getName());
        parameters.put("toEmail", to.getEmail());

        Integer newMailId = mailProvider.createMail(
                from,
                to,
                originalMail.getTemplateType(),
                parameters
        );

        if (newMailId == null) {
            return ActionResult.failed("Mail could not be queued again");
        }

        return ActionResult.success(
                "Mail queued again",
                newMailId
        );
    }

    private Map<String, Object> parseParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(
                    parametersJson,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void sendUnsupportedAction(HttpServletResponse response) throws IOException {
        JsonUtil.writeJson(
                response,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "{\"success\":false,\"message\":\"Use POST for mail status actions\"}"
        );
    }

    private static class ActionResult {

        private final boolean success;
        private final String message;
        private final Integer mailId;

        private ActionResult(
                boolean success,
                String message,
                Integer mailId
        ) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.mailId = mailId;
        }

        private static ActionResult success(
                String message,
                Integer mailId
        ) {
            return new ActionResult(
                    true,
                    message,
                    mailId
            );
        }

        private static ActionResult failed(String message) {
            return new ActionResult(
                    false,
                    message,
                    null
            );
        }

        private boolean isSuccess() {
            return success;
        }

        private String toJson() {
            StringBuilder json = new StringBuilder();

            json.append("{");
            JsonUtil.appendJsonBoolean(json, "success", success);
            json.append(",");
            JsonUtil.appendJsonString(json, "message", message);
            json.append(",");
            JsonUtil.appendJsonNumber(json, "mailId", mailId);
            json.append("}");

            return json.toString();
        }
    }
}
