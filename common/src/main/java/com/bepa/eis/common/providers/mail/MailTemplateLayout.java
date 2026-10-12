package com.bepa.eis.common.providers.mail;

import java.util.regex.Pattern;

/** Narrow, repeatable layout migration; preserves edited subjects and message content. */
public final class MailTemplateLayout {
    private MailTemplateLayout() {}
    private static final Pattern OLD_HEADER = Pattern.compile(
            "<tr>\\s*<td\\b[^>]*background-color:\\s*#(?:102a43|0f7b4f|9b1c1c|b7791f)[^>]*>\\s*<h1\\b[^>]*>.*?</h1>\\s*</td>\\s*</tr>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BANNER = Pattern.compile(
            "<table\\b[^>]*data-eis-banner=\"true\"[^>]*>.*?</table>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CELL_STYLE = Pattern.compile("(<td\\b[^>]*style=\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);

    public static String upgrade(String html) {
        if (html == null || !BANNER.matcher(html).find()) return html;
        String withoutOldHeader = OLD_HEADER.matcher(html).replaceAll("");
        return BANNER.matcher(withoutOldHeader).replaceAll(bannerMatch -> {
            String banner = bannerMatch.group().replace("width=\"150\" height=\"35\"", "width=\"110\" height=\"26\"");
            banner = Pattern.compile("<svg\\b[^>]*>.*?</svg>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                    .matcher(banner).replaceAll(java.util.regex.Matcher.quoteReplacement(MailTemplateLogo.imageHtml()));
            var cells = CELL_STYLE.matcher(banner);
            StringBuilder updated = new StringBuilder();
            int index = 0;
            while (cells.find()) {
                String style = cells.group(2);
                if (index == 1) {
                    style = style.replaceAll("(?i)width\\s*:[^;]*;?", "")
                            .replaceAll("(?i)font-size\\s*:[^;]*;?", "font-size:18px;");
                    if (!style.toLowerCase(java.util.Locale.ROOT).contains("white-space:nowrap")) {
                        style = style.replaceAll("(?i)white-space\\s*:[^;]*;?", "");
                        style += ";white-space:nowrap;";
                    }
                } else {
                    style = style.replaceAll("(?i)width\\s*:[^;]*;?", "width:120px;")
                            .replaceAll("(?i)padding\\s*:[^;]*;?", "padding:18px 12px;");
                }
                cells.appendReplacement(updated, java.util.regex.Matcher.quoteReplacement(cells.group(1) + style + cells.group(3)));
                index++;
            }
            cells.appendTail(updated);
            return java.util.regex.Matcher.quoteReplacement(updated.toString());
        });
    }
}
