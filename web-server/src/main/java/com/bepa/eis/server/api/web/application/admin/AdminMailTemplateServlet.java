package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.mail.MailTemplateCatalogProvider;
import com.bepa.eis.common.providers.mail.MailTemplateExamples;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.dto.mail.MailTemplate;
import com.bepa.eis.common.enums.mail.MailTemplateType;
import jakarta.mail.internet.InternetAddress;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@WebServlet(name = "AdminMailTemplateServlet", urlPatterns = "/api/admin/mail-templates")
public class AdminMailTemplateServlet extends AbstractAdminServlet {
    private final ObjectMapper json = new ObjectMapper();
    private static final String CSRF = "mailTemplateCsrf";
    public record Edit(String key, String subject, String bodyHtml, int revision, String action, String recipientEmail) {}

    @Override public void processGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession session = authorize(request, response);
        if (session == null) return;
        try {
            var provider = createCatalogProvider(session);
            if ("export".equals(request.getParameter("action"))) {
                response.setHeader("Content-Disposition", "attachment; filename=\"eis-mail-templates.json\"");
                write(response, 200, MailTemplateTransfer.export(provider.exportAll())); return;
            }
            String key = request.getParameter("key");
            if (key == null) {
                String csrf;
                synchronized (request.getSession()) {
                    csrf = (String) request.getSession().getAttribute(CSRF);
                    if (csrf == null) { csrf = UUID.randomUUID().toString(); request.getSession().setAttribute(CSRF, csrf); }
                }
                write(response, 200, Map.of("templates", provider.list(), "csrfToken", csrf));
            } else {
                var template = provider.get(key);
                if (template == null) error(response, 404, "Template not found.");
                else write(response, 200, template);
            }
        } catch (java.sql.SQLException e) { throw new ServletException("Could not load mail templates", e); }
    }

    @Override public void processPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession session = authorize(request, response);
        if (session == null) return;
        Object expected = request.getSession().getAttribute(CSRF);
        if (expected == null || !expected.equals(request.getHeader("X-CSRF-Token"))) { error(response, 403, "Reload the page before saving."); return; }
        boolean importing = "import".equals(request.getParameter("action"));
        int maxSize = importing ? 20_000_000 : 600_000;
        if (request.getContentLengthLong() > maxSize) { error(response, 413, "The template file is too large."); return; }
        request.setCharacterEncoding("UTF-8");
        StringBuilder payload = new StringBuilder();
        char[] buffer = new char[4096]; int count;
        var reader = request.getReader();
        while ((count = reader.read(buffer)) != -1) {
            payload.append(buffer, 0, count);
            if (payload.length() > maxSize) { error(response, 413, "The template file is too large."); return; }
        }
        if (importing) {
            try {
                var rows = MailTemplateTransfer.validate(json.readValue(payload.toString(), MailTemplateTransfer.Bundle.class));
                int imported = createCatalogProvider(session).importAll(rows, MailTemplateTransfer::validateAgainstExisting);
                write(response, 200, Map.of("message", imported + " templates imported.", "imported", imported));
            } catch (IOException | IllegalArgumentException e) { error(response, 400, e instanceof IllegalArgumentException ? e.getMessage() : "Invalid import file."); }
            catch (java.sql.SQLException e) { throw new ServletException("Could not import mail templates", e); }
            return;
        }
        Edit edit;
        try { edit = json.readValue(payload.toString(), Edit.class); }
        catch (IOException e) { error(response, 400, "Invalid template data."); return; }
        if (edit.key() == null || edit.subject() == null || edit.subject().isBlank() || edit.subject().length() > 500
                || edit.subject().contains("\r") || edit.subject().contains("\n") || edit.bodyHtml() == null || edit.bodyHtml().length() > 500_000
                || !Pattern.compile("<body\\b", Pattern.CASE_INSENSITIVE).matcher(edit.bodyHtml()).find()) {
            error(response, 400, "Enter a subject and email content."); return;
        }
        try {
            var provider = createCatalogProvider(session);
            var original = provider.get(edit.key());
            if (original == null) { error(response, 404, "Template not found."); return; }
            // Original tokens may be moved but must survive visual editing intact.
            String text = edit.subject() + edit.bodyHtml();
            for (String token : json.readValue(original.placeholdersJson(), String[].class)) {
                if (!text.contains("{{" + token + "}}")) { error(response, 400, "The field {{" + token + "}} is missing or has been split. Undo the change or insert the field again."); return; }
            }
            if ("preview".equals(edit.action()) || "test".equals(edit.action())) {
                var rendered = MailTemplateExamples.render(edit.subject(), edit.bodyHtml());
                if ("preview".equals(edit.action())) { write(response, 200, rendered); return; }
                String recipient = edit.recipientEmail() == null ? "" : edit.recipientEmail().trim();
                try {
                    if (recipient.isEmpty() || recipient.length() > 254 || recipient.contains("\r") || recipient.contains("\n")) throw new IllegalArgumentException();
                    var address = new InternetAddress(recipient, true);
                    address.validate();
                    if (!recipient.equals(address.getAddress()) || address.isGroup() || address.getPersonal() != null) throw new IllegalArgumentException();
                } catch (Exception e) { error(response, 400, "Enter one valid recipient email address."); return; }
                var draft = new MailTemplate(MailTemplateType.mapToType(edit.key()), "[TEST] " + rendered.subject(),
                        rendered.bodyHtml(), "text/html; charset=UTF-8", "catalogue:" + edit.key());
                Integer mailId = createMailProvider(session).createTemplateTestMail(MailRecipient.of("", recipient), draft);
                if (mailId == null) { error(response, 503, "The test email could not be queued. Please try again."); return; }
                write(response, 200, Map.of("message", "Test email queued for " + recipient + ".", "mailId", mailId));
                return;
            }
            if (edit.action() != null && !edit.action().isBlank() && !"save".equals(edit.action())) {
                error(response, 400, "Unsupported template action."); return;
            }
            if (!provider.save(edit.key(), edit.subject(), edit.bodyHtml(), edit.revision())) {
                error(response, 409, "Someone else has changed this template. Close the dialog and reopen it."); return;
            }
            write(response, 200, provider.get(edit.key()));
        } catch (java.sql.SQLException e) { throw new ServletException("Could not save mail template", e); }
    }

    protected MailProvider createMailProvider(WebSession session) { return new MailProvider(session); }
    protected MailTemplateCatalogProvider createCatalogProvider(WebSession session) { return new MailTemplateCatalogProvider(session); }

    protected WebSession authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WebSession session = getWebSessionFromRequest(request, false);
        var user = session == null || session.getUserId() == null ? null : new UserProvider(session).getUserAdministrationRow(session.getUserId());
        if (user == null || user.userRole() != UserRoles.BEPA_SYSTEM_ADMINISTRATOR) {
            error(response, 403, "Only system administrators can access mail templates."); return null;
        }
        return session;
    }

    private void error(HttpServletResponse response, int status, String message) throws IOException { write(response, status, Map.of("message", message)); }
    private void write(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getWriter(), data);
    }
}
