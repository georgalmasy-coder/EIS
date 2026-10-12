package com.bepa.eis.common.providers.mail;

import java.util.Map;
import java.util.regex.Pattern;

/** Shared sample values for preview and test delivery; never generates real workflow links. */
public final class MailTemplateExamples {
    private MailTemplateExamples() {}
    public record Rendered(String subject, String bodyHtml) {}
    private static final Map<String, String> VALUES = Map.ofEntries(
            Map.entry("applicationName", "BEPA EIS"), Map.entry("customerName", "Example Ltd"),
            Map.entry("customerContactName", "Anne Jensen"), Map.entry("customerId", "123"),
            Map.entry("workflowId", "456"), Map.entry("trialDays", "30"),
            Map.entry("confirmationExpiresAt", "20 Oct 2026, 12:00"), Map.entry("paymentAmount", "499.00"),
            Map.entry("paymentCurrency", "DKK"), Map.entry("paymentReference", "EIS-2026-001"),
            Map.entry("paymentId", "789"), Map.entry("paymentDueDate", "1 Nov 2026"),
            Map.entry("paymentGracePeriodEndsAt", "8 Nov 2026"), Map.entry("paymentFailureReason", "The payment was declined"),
            Map.entry("subscriptionPlanName", "Professional"), Map.entry("subscriptionStatus", "ACTIVE"),
            Map.entry("paymentStatus", "PAID"), Map.entry("suspensionReason", "The payment deadline has passed"),
            Map.entry("suspendedAt", "10 Oct 2026, 12:00"), Map.entry("reactivatedAt", "12 Oct 2026, 09:00"),
            Map.entry("subscriptionEndDate", "1 Nov 2026"), Map.entry("subscriptionId", "321"),
            Map.entry("subscriptionPeriodDescription", "1 year"), Map.entry("confirmationDeadline", "20 Oct 2026"),
            Map.entry("renewalConfirmationDeadline", "25 Oct 2026"), Map.entry("trialStartDate", "1 Oct 2026"),
            Map.entry("trialEndDate", "31 Oct 2026"));
    private static final Pattern TOKEN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public static Rendered render(String subject, String html) {
        return new Rendered(replace(subject, false), replace(html, true));
    }
    private static String replace(String text, boolean html) {
        return TOKEN.matcher(text).replaceAll(match -> {
            String key = match.group(1);
            String value = VALUES.getOrDefault(key, key.endsWith("Link") ? "https://example.invalid/demo" : "[Example: " + key + "]");
            if (html) value = value.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("'", "&#39;");
            return java.util.regex.Matcher.quoteReplacement(value);
        });
    }
}
