package com.bepa.eis.server.api.web.application.enums.theme;

import com.bepa.eis.server.api.web.application.enums.PageType;

import java.util.Locale;

public enum Theme {

    LIGHT(1, "theme-light"),
    BLUE(2, "theme-blue"),
    BLACK(3, "theme-black");

    private final Integer id;
    private final String cssClass;

    Theme(Integer id, String cssClass) {
        this.id = id;
        this.cssClass = cssClass;
    }

    public Integer getCssId() {
        return id;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static Theme fromId(String value) {
        if (value == null || value.isBlank()) {
            return LIGHT;
        }

        for (Theme theme : Theme.values()) {
            if (theme.id == Integer.parseInt(value.trim())) return theme;
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
