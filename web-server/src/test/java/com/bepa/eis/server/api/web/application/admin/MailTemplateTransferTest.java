package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.providers.mail.MailTemplateCatalogProvider.Template;
import com.bepa.eis.common.providers.mail.MailTemplateLogo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class MailTemplateTransferTest {
    public static void main(String[] args) throws Exception {
        var original = new Template("USER_CREATED", "Welcome", "Hello {{name}}", "<html><body>Edited Å &amp; Ø " + MailTemplateLogo.imageHtml() + " <a href=\"{{resetLink}}\">Reset</a></body></html>", "text/html; charset=UTF-8", "user-created.mail", "[\"name\",\"resetLink\"]", 42, "2026-10-12T00:00:00Z");
        var system = new Template("SYSTEM_NOTIFICATION", "System Notification", "", "<body>" + MailTemplateLogo.imageHtml() + "</body>", "text/html; charset=UTF-8", "system-notification.mail", "[]", 1, null);
        var json = new ObjectMapper();
        var exported = MailTemplateTransfer.export(List.of(original, system));
        String file = json.writeValueAsString(exported);
        check(!file.contains("updatedAt") && !file.contains("revision"), "Local revisions and audit data are not transferred");
        var imported = MailTemplateTransfer.validate(json.readValue(file, MailTemplateTransfer.Bundle.class));
        check(imported.get(0).subject().equals(original.subject()) && imported.get(0).bodyHtml().equals(original.bodyHtml()), "Subjects, Unicode and PNG survive JSON round trip exactly");
        MailTemplateTransfer.validateAgainstExisting(imported.get(0), original);
        invalid(new MailTemplateTransfer.Bundle("eis-mail-templates", 99, exported.templates()));
        invalid(new MailTemplateTransfer.Bundle("eis-mail-templates", 1, List.of(exported.templates().get(0), exported.templates().get(0))));
        var entry = exported.templates().get(0);
        invalid(new MailTemplateTransfer.Bundle("eis-mail-templates", 1, List.of(new MailTemplateTransfer.Entry("UNKNOWN", entry.name(), entry.subject(), entry.bodyHtml(), entry.contentType(), entry.placeholders()))));
        invalid(new MailTemplateTransfer.Bundle("eis-mail-templates", 1, List.of(new MailTemplateTransfer.Entry(entry.key(), entry.name(), entry.subject(), entry.bodyHtml().replace("{{resetLink}}", "gone"), entry.contentType(), entry.placeholders()))));
        var reduced = new Template(original.key(), original.name(), "Hello", "<body>Blank</body>", original.contentType(), original.sourceFileName(), "[]", 1, null);
        try { MailTemplateTransfer.validateAgainstExisting(reduced, original); throw new AssertionError("Removed workflow fields accepted"); }
        catch (IllegalArgumentException expected) {}
        System.out.println("Template transfer tests passed: lossless export/import, PNG, Unicode, empty system template, bad versions, duplicates, unknown keys, missing fields.");
    }
    static void invalid(MailTemplateTransfer.Bundle bundle) { try { MailTemplateTransfer.validate(bundle); throw new AssertionError("Invalid bundle accepted"); } catch (IllegalArgumentException expected) {} }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
