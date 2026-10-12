package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.providers.EisDataSourceProvider;
import java.nio.file.*;
import java.util.regex.Pattern;

/** Explicit local-development database smoke test. Source config is copied, never modified. */
public class MailTemplateCatalogDatabaseTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply the local development config path");
        Path directory = Path.of("common/target").toAbsolutePath();
        Files.createDirectories(directory);
        Path config = Files.createTempFile(directory, "mail-catalog-test-", ".properties");
        try {
            var properties = new java.util.Properties();
            try (var reader = Files.newBufferedReader(Path.of(args[0]))) { properties.load(reader); }
            String url = properties.getProperty("integration-server.database.jdbc.url", "");
            if (!url.startsWith("jdbc:sqlserver://localhost:")) throw new IllegalArgumentException("Only localhost development database is allowed");
            try (var writer = Files.newBufferedWriter(config)) { properties.store(writer, "Isolated configuration for catalogue smoke test"); }
            System.setProperty("eis.config.file", config.toString());
            System.setProperty("eis.application.name", "integration-server");
            var provider = new MailTemplateCatalogProvider(null);
            provider.install();
            var rows = provider.list();
            long count;
            try (var files = Files.list(Path.of("common/src/eis/mail-templates"))) { count = files.filter(path -> path.toString().endsWith(".mail")).count(); }
            check(rows.size() >= count, "Every existing template imported");
            for (var row : rows) {
                var template = provider.get(row.key());
                check(template.bodyHtml().equals(MailTemplateLayout.upgrade(template.bodyHtml())), "Layout migrated and stable: " + row.key());
                check(!template.bodyHtml().contains("#102a43"), "Legacy blue banner removed: " + row.key());
                for (String colour : new String[]{"#0f7b4f", "#9b1c1c", "#b7791f"}) {
                    check(!template.bodyHtml().contains(colour), "Extra legacy heading removed: " + row.key());
                }
                check(template.bodyHtml().contains("white-space:nowrap"), "Green heading cannot wrap: " + row.key());
                check(template.bodyHtml().contains(MailTemplateLogo.DATA_URI) && !template.bodyHtml().contains("<svg"), "PNG logo migrated: " + row.key());
                Path original = Path.of("common/src/eis/mail-templates", row.sourceFileName());
                if (!Files.exists(original) || row.revision() != 1) continue;
                String content = Files.readString(original);
                if (content.isBlank()) continue;
                String[] parts = content.split("\\r?\\n\\r?\\n", 2);
                String subject = Pattern.compile("(?m)^Subject:\\s*(.*)").matcher(parts[0]).results().findFirst().orElseThrow().group(1).trim();
                check(subject.equals(template.subject()), "Subject preserved: " + row.key());
                String withoutBanner = template.bodyHtml().replaceFirst("(?s)\\n<table data-eis-banner=\"true\".*?</table>\\n", "");
                String originalMessage = parts[1].replaceAll("(?is)<tr>\\s*<td\\b[^>]*background-color:\\s*#(?:102a43|0f7b4f|9b1c1c|b7791f)[^>]*>\\s*<h1\\b[^>]*>.*?</h1>\\s*</td>\\s*</tr>", "");
                check(withoutBanner.equals(originalMessage), "Original HTML preserved except old blue heading: " + row.key());
                check(template.bodyHtml().contains("#073f34") && template.bodyHtml().contains(MailTemplateLogo.DATA_URI), "Sidebar colour and PNG logo imported");
            }
            provider.install();
            for (var row : rows) check(provider.get(row.key()).revision() == row.revision(), "Reimport does not overwrite edits");
            var first = provider.get(rows.get(0).key());
            var second = provider.get(rows.get(1).key());
            var changed = new MailTemplateCatalogProvider.Template(first.key(), first.name(), "UNCOMMITTED_IMPORT_TEST", first.bodyHtml(), first.contentType(), first.sourceFileName(), first.placeholdersJson(), first.revision(), first.updatedAt());
            var invalid = new MailTemplateCatalogProvider.Template(second.key(), "x".repeat(201), second.subject(), second.bodyHtml(), second.contentType(), second.sourceFileName(), second.placeholdersJson(), second.revision(), second.updatedAt());
            try { provider.importAll(java.util.List.of(changed, invalid), (incoming, existing) -> {}); throw new AssertionError("Invalid SQL row unexpectedly committed"); }
            catch (java.sql.SQLException expected) {}
            check(provider.get(first.key()).subject().equals(first.subject()) && provider.get(first.key()).revision() == first.revision(), "Failed multi-template import rolls back earlier update");
            check(new MailTemplateRenderer().loadTemplate(com.bepa.eis.common.enums.mail.MailTemplateType.valueOf(first.key())).getSubject().equals(first.subject()), "Delivery renderer reads actual database content");
            // Exercise the same SQL revision condition in a transaction that is rolled back.
            try (var connection = EisDataSourceProvider.getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try (var update = connection.prepareStatement("UPDATE dbo.MAIL_TEMPLATE SET Revision=Revision+1 WHERE TemplateKey=? AND Revision=?")) {
                    var row = rows.get(0); update.setString(1, row.key()); update.setInt(2, row.revision());
                    check(update.executeUpdate() == 1, "Current revision accepted");
                    check(update.executeUpdate() == 0, "Stale revision rejected");
                } finally { connection.rollback(); }
            }
            System.out.println("Catalogue database tests passed: " + rows.size() + " templates, original subjects/HTML, idempotent import, revision conflict.");
        } finally { Files.deleteIfExists(config); }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
