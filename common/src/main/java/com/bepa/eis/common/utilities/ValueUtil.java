package com.bepa.eis.common.utilities;

public class ValueUtil {

    public static Integer intValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String safeText(String value) {
        return value == null ? "" : value.trim();
    }


}
