package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.enums.mail.MailTemplateType;
import java.util.Map;

public class DatabaseMailTemplateRendererTest {
    public static void main(String[] args) throws Exception {
        var store = new java.util.HashMap<String, MailTemplateCatalogProvider.Template>();
        store.put("USER_CREATED", template("First {{name}}", "<html><body>{{name}} <a href=\"{{resetLink}}\">Set password</a></body></html>"));
        var renderer = new MailTemplateRenderer() {
            @Override protected MailTemplateCatalogProvider createCatalogProvider() {
                return new MailTemplateCatalogProvider(null) { @Override public Template get(String key) { return store.get(key); } };
            }
        };
        var result = renderer.render(MailTemplateType.USER_CREATED, Map.of("name", "Anne & <Team>", "resetLink", "https://example.test/reset?a=1&b=2"));
        check(result.getSubject().equals("First Anne & <Team>"), "Subject keeps plain text");
        check(result.getBody().contains("Anne &amp; &lt;Team&gt;") && result.getBody().contains("a=1&amp;b=2"), "HTML values are escaped");
        store.put("USER_CREATED", template("Changed in database", "<body>Changed</body>"));
        check(renderer.loadTemplate(MailTemplateType.USER_CREATED).getSubject().equals("Changed in database"), "Next mail sees latest database value");
        store.clear();
        try { renderer.loadTemplate(MailTemplateType.USER_CREATED); throw new AssertionError("Missing DB row silently fell back to bundled file"); }
        catch (java.io.IOException expected) { check(expected.getMessage().contains("Database mail template not found"), "Missing template reports database error"); }
        System.out.println("Database renderer tests passed: latest values, escaped HTML, no file fallback. No email sent.");
    }
    static MailTemplateCatalogProvider.Template template(String subject, String body) { return new MailTemplateCatalogProvider.Template("USER_CREATED", "User Created", subject, body, "text/html; charset=UTF-8", "user-created.mail", "[]", 1, "2026-10-12T00:00:00Z"); }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
