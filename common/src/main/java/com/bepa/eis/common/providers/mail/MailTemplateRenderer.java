package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.dto.mail.MailTemplate;
import com.bepa.eis.common.enums.mail.MailTemplateType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/** Reads the current database template for every new mail. No file fallback or template cache. */
public class MailTemplateRenderer {
    public MailTemplate render(MailTemplateType type, Map<String, Object> parameters) throws IOException {
        MailTemplate template = loadTemplate(type);
        return new MailTemplate(template.getTemplateType(), renderText(template.getSubject(), parameters),
                renderText(template.getBody(), parameters, template.isHtml()), template.getContentType(), template.getSourceFileName());
    }
    public MailTemplate loadTemplate(MailTemplateType type) throws IOException {
        MailTemplateType safeType = type == null ? MailTemplateType.SYSTEM_NOTIFICATION : type;
        try {
            var row = createCatalogProvider().get(safeType.name());
            if (row == null) throw new IOException("Database mail template not found: " + safeType.name());
            return new MailTemplate(safeType, row.subject(), row.bodyHtml(), row.contentType(), "database:MAIL_TEMPLATE/" + safeType.name());
        } catch (SQLException e) { throw new IOException("Could not load database mail template: " + safeType.name(), e); }
    }
    protected MailTemplateCatalogProvider createCatalogProvider() { return new MailTemplateCatalogProvider(null); }
    public String renderText(String text, Map<String, Object> parameters) { return renderText(text, parameters, false); }
    private String renderText(String text, Map<String, Object> parameters, boolean html) {
        if (text == null) return "";
        if (parameters == null) return text;
        String result = text;
        for (var entry : parameters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            String key = entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            if (html) value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
            result = result.replace("{{" + key + "}}", value);
            result = Pattern.compile(Pattern.quote("%7B%7B" + key + "%7D%7D"), Pattern.CASE_INSENSITIVE)
                    .matcher(result).replaceAll(java.util.regex.Matcher.quoteReplacement(value));
        }
        return result;
    }
}
