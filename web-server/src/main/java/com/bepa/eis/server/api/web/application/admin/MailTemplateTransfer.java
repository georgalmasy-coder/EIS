package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.enums.mail.MailTemplateType;
import com.bepa.eis.common.providers.mail.MailTemplateCatalogProvider.Template;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Pattern;

/** Versioned, environment-independent exchange format. Local revisions/users are never imported. */
public final class MailTemplateTransfer {
    public record Bundle(String format, int version, List<Entry> templates) {}
    public record Entry(String key, String name, String subject, String bodyHtml, String contentType, List<String> placeholders) {}
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern TOKEN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private MailTemplateTransfer() {}
    public static Bundle export(List<Template> templates) {
        return new Bundle("eis-mail-templates", 1, templates.stream().map(row -> new Entry(row.key(), row.name(), row.subject(), row.bodyHtml(), row.contentType(), tokens(row.placeholdersJson()))).toList());
    }
    public static List<Template> validate(Bundle bundle) {
        if (bundle == null || !"eis-mail-templates".equals(bundle.format()) || bundle.version() != 1 || bundle.templates() == null || bundle.templates().isEmpty()
                || bundle.templates().size() > MailTemplateType.values().length) throw new IllegalArgumentException("Invalid or unsupported mail template export file.");
        Set<String> keys = new HashSet<>(); List<Template> rows = new ArrayList<>();
        for (Entry entry : bundle.templates()) {
            if (entry == null || entry.key() == null || !keys.add(entry.key())) throw new IllegalArgumentException("Missing or duplicate template key.");
            MailTemplateType type;
            try { type = MailTemplateType.valueOf(entry.key()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unknown template: " + entry.key()); }
            if (entry.name() == null || entry.name().isBlank() || entry.name().length() > 200 || entry.subject() == null || entry.subject().length() > 500
                    || entry.subject().contains("\r") || entry.subject().contains("\n") || (entry.subject().isBlank() && type != MailTemplateType.SYSTEM_NOTIFICATION)
                    || entry.bodyHtml() == null || entry.bodyHtml().length() > 500_000 || !Pattern.compile("<body\\b", Pattern.CASE_INSENSITIVE).matcher(entry.bodyHtml()).find()
                    || !"text/html; charset=UTF-8".equalsIgnoreCase(entry.contentType()) || entry.placeholders() == null
                    || entry.placeholders().stream().anyMatch(token -> token == null || !token.matches("\\w+"))) {
                throw new IllegalArgumentException("Invalid content or metadata for " + entry.key() + ".");
            }
            Set<String> used = new HashSet<>(); TOKEN.matcher(entry.subject() + entry.bodyHtml()).results().forEach(match -> used.add(match.group(1)));
            Set<String> declared = new HashSet<>(entry.placeholders());
            if (declared.size() != entry.placeholders().size() || !declared.equals(used)) throw new IllegalArgumentException("Missing, split or undeclared fields in " + entry.key() + ".");
            try { rows.add(new Template(entry.key(), entry.name(), entry.subject(), entry.bodyHtml(), "text/html; charset=UTF-8", type.getFileName(), JSON.writeValueAsString(entry.placeholders()), 1, null)); }
            catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid fields in " + entry.key(), e); }
        }
        return rows;
    }
    public static void validateAgainstExisting(Template incoming, Template existing) {
        if (existing != null && !new HashSet<>(tokens(incoming.placeholdersJson())).containsAll(tokens(existing.placeholdersJson()))) {
            throw new IllegalArgumentException("The import removes required fields from " + incoming.key() + ".");
        }
    }
    private static List<String> tokens(String json) {
        try { return Arrays.asList(JSON.readValue(json, String[].class)); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid template field metadata.", e); }
    }
}
