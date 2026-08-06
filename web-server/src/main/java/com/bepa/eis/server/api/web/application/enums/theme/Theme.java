package com.bepa.eis.server.api.web.application.enums.theme;

import java.util.Locale;

public enum Theme {

    LIGHT("theme-light"),
    BLUE("theme-blue"),
    BLACK("theme-black");

    private final String cssClass;

    Theme(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static Theme fromName(String value) {
        if (value == null || value.isBlank()) {
            return LIGHT;
        }

        try {
            return Theme.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
            return LIGHT;
        }
    }
}
