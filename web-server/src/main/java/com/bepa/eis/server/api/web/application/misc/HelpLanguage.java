package com.bepa.eis.server.api.web.application.misc;

/** The development language experiment currently applies only to Markdown help. */
public enum HelpLanguage {
    ENGLISH("en", "en"),
    DANISH("dk", "da");

    public static final String PREFERENCE_KEY = "eis.language";

    private final String folder;
    private final String languageTag;

    HelpLanguage(String folder, String languageTag) {
        this.folder = folder;
        this.languageTag = languageTag;
    }

    public String resourcePath(String page) {
        return "/WEB-INF/help/" + folder + "/" + page + ".md";
    }

    public String languageTag() {
        return languageTag;
    }

    public static HelpLanguage fromPreference(String value) {
        return "dk".equals(value) ? DANISH : ENGLISH;
    }

    public static boolean isSupported(String value) {
        return "en".equals(value) || "dk".equals(value);
    }
}
