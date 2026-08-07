package com.bepa.eis.server.api.web.application.enums.theme;

import java.util.Locale;

public enum Theme {

    LIGHT(1, "theme-light"),
    BLUE(2, "theme-blue"),
    BLACK(3, "theme-black");

    private final int id;
    private final String cssClass;

    Theme(int id, String cssClass) {
        this.id = id;
        this.cssClass = cssClass;
    }

    public int getId() {
        return id;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static Theme fromId(Integer id) {
        if (id == null) {
            return LIGHT;
        }

        for (Theme theme : values()) {
            if (theme.id == id) {
                return theme;
            }
        }

        return LIGHT;
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
