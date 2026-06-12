package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.mail.MailTemplate;
import com.bepa.eis.common.enums.mail.MailTemplateType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailTemplateRenderer {

    private static final String DEFAULT_CONTENT_TYPE = "text/html; charset=UTF-8";

    private static final String SUBJECT_HEADER = "Subject:";
    private static final String CONTENT_TYPE_HEADER = "Content-Type:";

    public MailTemplateRenderer() {
    }

    public MailTemplate render(
            MailTemplateType templateType,
            Map<String, Object> parameters
    ) throws IOException {
        MailTemplate template = loadTemplate(templateType);

        return new MailTemplate(
                template.getTemplateType(),
                renderText(template.getSubject(), parameters),
                renderText(template.getBody(), parameters),
                template.getContentType(),
                template.getSourceFileName()
        );
    }

    public MailTemplate loadTemplate(MailTemplateType templateType) throws IOException {
        MailTemplateType safeTemplateType = templateType == null
                ? MailTemplateType.SYSTEM_NOTIFICATION
                : templateType;

        File templateFile = resolveTemplateFile(safeTemplateType);

        if (!templateFile.isFile()) {
            throw new IOException("Mail template file not found: " + templateFile.getAbsolutePath());
        }

        String fileContent = readFile(templateFile);
        ParsedTemplate parsedTemplate = parseTemplate(fileContent);

        return new MailTemplate(
                safeTemplateType,
                parsedTemplate.subject,
                parsedTemplate.body,
                parsedTemplate.contentType,
                templateFile.getAbsolutePath()
        );
    }

    public String renderText(
            String text,
            Map<String, Object> parameters
    ) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Map<String, Object> safeParameters = parameters == null
                ? new LinkedHashMap<String, Object>()
                : parameters;

        String renderedText = text;

        for (Map.Entry<String, Object> entry : safeParameters.entrySet()) {
            String key = entry.getKey();

            if (key == null || key.trim().isEmpty()) {
                continue;
            }

            String placeholder = "{{" + key.trim() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());

            renderedText = renderedText.replace(placeholder, value);
        }

        return renderedText;
    }

    public File getTemplateDirectory() {
        return GlobalConfiguration.getMailTemplateDirectory();
    }

    public File resolveTemplateFile(MailTemplateType templateType) {
        MailTemplateType safeTemplateType = templateType == null
                ? MailTemplateType.SYSTEM_NOTIFICATION
                : templateType;

        return new File(getTemplateDirectory(), safeTemplateType.getFileName());
    }

    private ParsedTemplate parseTemplate(String fileContent) {
        String[] lines = normalizeLineEndings(fileContent).split("\n", -1);

        String subject = "";
        String contentType = GlobalConfiguration.getMailDefaultContentType();

        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = DEFAULT_CONTENT_TYPE;
        }

        StringBuilder body = new StringBuilder();
        boolean bodyStarted = false;

        for (String line : lines) {
            if (!bodyStarted) {
                if (line.trim().isEmpty()) {
                    bodyStarted = true;
                    continue;
                }

                if (startsWithIgnoreCase(line, SUBJECT_HEADER)) {
                    subject = line.substring(SUBJECT_HEADER.length()).trim();
                    continue;
                }

                if (startsWithIgnoreCase(line, CONTENT_TYPE_HEADER)) {
                    contentType = line.substring(CONTENT_TYPE_HEADER.length()).trim();

                    if (contentType.isEmpty()) {
                        contentType = GlobalConfiguration.getMailDefaultContentType();
                    }

                    continue;
                }

                bodyStarted = true;
            }

            body.append(line).append("\n");
        }

        return new ParsedTemplate(
                subject,
                trimTrailingLineBreak(body.toString()),
                contentType
        );
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    private String normalizeLineEndings(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        if (value == null || prefix == null) {
            return false;
        }

        if (value.length() < prefix.length()) {
            return false;
        }

        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private String trimTrailingLineBreak(String value) {
        if (value == null) {
            return "";
        }

        String result = value;

        while (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private static class ParsedTemplate {

        private final String subject;
        private final String body;
        private final String contentType;

        private ParsedTemplate(
                String subject,
                String body,
                String contentType
        ) {
            this.subject = subject == null ? "" : subject.trim();
            this.body = body == null ? "" : body.trim();
            this.contentType = contentType == null || contentType.trim().isEmpty()
                    ? DEFAULT_CONTENT_TYPE
                    : contentType.trim();
        }
    }
}