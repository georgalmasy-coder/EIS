package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.dto.mail.MailTemplate;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.providers.mail.MailTemplateCatalogProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Standalone endpoint test; fake providers prevent database writes and real mail delivery. */
public class MailTemplateActionsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    record Result(int status, String json) {}
    public static void main(String[] args) throws Exception {
        var servlet = new FakeServlet();
        var preview = post(servlet, "preview", null, "csrf");
        check(preview.status == 200 && preview.json.contains("Example Ltd") && preview.json.contains("Unsaved message"), "Preview uses unsaved draft and shared examples");
        check(servlet.queued == 0, "Preview never queues a message");
        for (String invalid : new String[]{"broken", "a@example.test,b@example.test", "a@example.test\r\nBcc: b@example.test"}) {
            check(post(servlet, "test", invalid, "csrf").status == 400, "Reject invalid or multiple recipients");
        }
        check(post(servlet, "test", "review@example.test", "wrong").status == 403 && servlet.queued == 0, "CSRF failure cannot queue mail");
        var sent = post(servlet, "test", "review@example.test", "csrf");
        check(sent.status == 200 && servlet.queued == 1, "Explicit test request queues one message");
        check(servlet.recipient.getEmail().equals("review@example.test"), "Only manually specified recipient used");
        check(servlet.mail.getSubject().equals("[TEST] Hello Example Ltd"), "Test subject is identifiable");
        check(servlet.mail.getBody().contains("Unsaved message") && servlet.mail.getBody().contains("https://example.invalid/demo") && !servlet.mail.getBody().contains("{{"), "Unsaved body rendered with inactive sample links");
        servlet.failQueue = true;
        check(post(servlet, "test", "review@example.test", "csrf").status == 503, "Queue failure is not reported as successful delivery");
        System.out.println("Mail template action tests passed: unsaved preview/test, single recipient, CSRF, queue failure. No mail was sent.");
    }
    static Result post(FakeServlet servlet, String action, String recipient, String csrf) throws Exception {
        var values = new java.util.LinkedHashMap<String,Object>();
        values.put("key", "USER_CREATED"); values.put("subject", "Hello {{customerName}}");
        values.put("bodyHtml", "<html><body>Unsaved message <a href=\"{{resetLink}}\">Reset</a></body></html>");
        values.put("revision", 1); values.put("action", action); values.put("recipientEmail", recipient);
        String payload = JSON.writeValueAsString(values);
        var session = proxy(HttpSession.class, (method, parameters) -> "getAttribute".equals(method) ? "csrf" : null);
        var reader = new BufferedReader(new StringReader(payload));
        var request = proxy(HttpServletRequest.class, (method, parameters) -> switch (method) {
            case "getSession" -> session;
            case "getHeader" -> csrf;
            case "getContentLengthLong" -> (long) payload.length();
            case "getReader" -> reader;
            default -> null;
        });
        var output = new StringWriter(); var writer = new PrintWriter(output); int[] status = {200};
        var response = proxy(HttpServletResponse.class, (method, parameters) -> {
            if (method.equals("getWriter")) return writer;
            if (method.equals("setStatus")) status[0] = (int) parameters[0];
            return null;
        });
        servlet.processPost(request, response); writer.flush();
        return new Result(status[0], output.toString());
    }
    interface Handler { Object handle(String method, Object[] arguments); }
    static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, arguments) -> handler.handle(method.getName(), arguments)));
    }
    static class FakeServlet extends AdminMailTemplateServlet {
        int queued; boolean failQueue; MailTemplate mail; MailRecipient recipient;
        final WebSession session = new WebSession();
        @Override protected WebSession authorize(HttpServletRequest request, HttpServletResponse response) { return session; }
        @Override protected MailTemplateCatalogProvider createCatalogProvider(WebSession session) {
            return new MailTemplateCatalogProvider(session) {
                @Override public Template get(String key) { return new Template(key, "User Created", "Saved subject", "<body>Saved body</body>", "text/html", "user-created.mail", "[\"resetLink\"]", 1, "2026-10-12T00:00:00Z"); }
                @Override public boolean save(String key, String subject, String body, int revision) { throw new AssertionError("Preview and test must not save drafts"); }
            };
        }
        @Override protected MailProvider createMailProvider(WebSession session) {
            return new MailProvider(session) {
                @Override public Integer createTemplateTestMail(MailRecipient to, MailTemplate draft) {
                    if (failQueue) return null;
                    queued++; recipient=to; mail=draft; return 101;
                }
            };
        }
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
