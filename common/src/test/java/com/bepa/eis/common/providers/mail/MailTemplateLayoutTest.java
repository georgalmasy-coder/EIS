package com.bepa.eis.common.providers.mail;

public class MailTemplateLayoutTest {
    public static void main(String[] args) {
        String html = "<html><body><table data-eis-banner=\"true\"><tr>"
                + "<td style=\"width:25%;padding:18px 16px;\"><svg width=\"150\" height=\"35\"></svg></td>"
                + "<td style=\"width:50%;font-size:22px;\">Edited green title</td>"
                + "<td style=\"width:25%;padding:18px 16px;\"></td></tr></table>"
                + "<table><tr><td style=\"background-color:#102a43;color:#fff;\"><h1>Old title</h1></td></tr>"
                + "<tr><td>Edited message {{resetLink}}</td></tr></table></body></html>";
        String updated = MailTemplateLayout.upgrade(html);
        check(!updated.contains("Old title") && !updated.contains("#102a43"), "Only old blue heading removed");
        check(updated.contains("Edited green title") && updated.contains("Edited message {{resetLink}}"), "Edited content and placeholders preserved");
        check(updated.contains("white-space:nowrap") && !updated.contains("width:50%"), "Green title has room and cannot wrap");
        check(updated.equals(MailTemplateLayout.upgrade(updated)), "Migration is idempotent");
        for (String colour : new String[]{"#0f7b4f", "#9b1c1c", "#b7791f"}) {
            String migrated = MailTemplateLayout.upgrade(html.replace("#102a43", colour));
            check(!migrated.contains("Old title") && migrated.contains("Edited message {{resetLink}}")
                    && migrated.contains("Edited green title"), "Legacy heading removed while preserving content: " + colour);
        }
        check(html.replace("data-eis-banner", "custom-banner").equals(MailTemplateLayout.upgrade(html.replace("data-eis-banner", "custom-banner"))), "Unrelated layouts left intact");
        System.out.println("Mail template layout tests passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
