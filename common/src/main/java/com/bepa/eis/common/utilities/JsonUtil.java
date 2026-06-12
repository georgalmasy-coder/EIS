package com.bepa.eis.common.utilities;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

public class JsonUtil {
    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 32) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.toString();
    }

    public static void writeJson(
            HttpServletResponse response,
            int status,
            String json
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }


    public static void appendJsonString(
            StringBuilder json,
            String name,
            String value
    ) {
        json.append("\"")
                .append(escapeJson(name))
                .append("\":\"")
                .append(escapeJson(value))
                .append("\"");
    }

    public static void appendJsonNumber(
            StringBuilder json,
            String name,
            Integer value
    ) {
        json.append("\"")
                .append(escapeJson(name))
                .append("\":");

        if (value == null) {
            json.append("null");
        } else {
            json.append(value);
        }
    }

    public static void appendJsonTimestamp(
            StringBuilder json,
            String name,
            Timestamp value
    ) {
        json.append("\"")
                .append(escapeJson(name))
                .append("\":");

        if (value == null) {
            json.append("null");
        } else {
            json.append("\"")
                    .append(escapeJson(value.toString()))
                    .append("\"");
        }
    }

    public static void appendJsonBoolean(
            StringBuilder json,
            String name,
            boolean value
    ) {
        json.append("\"")
                .append(JsonUtil.escapeJson(name))
                .append("\":")
                .append(value);
    }

    public static void appendJsonRawOrString(
            StringBuilder json,
            String name,
            String value
    ) {
        json.append("\"")
                .append(JsonUtil.escapeJson(name))
                .append("\":");

        if (value == null || value.trim().isEmpty()) {
            json.append("null");
            return;
        }

        String trimmedValue = value.trim();

        if ((trimmedValue.startsWith("{") && trimmedValue.endsWith("}"))
                || (trimmedValue.startsWith("[") && trimmedValue.endsWith("]"))) {
            json.append(trimmedValue);
            return;
        }

        json.append("\"")
                .append(JsonUtil.escapeJson(trimmedValue))
                .append("\"");
    }


/*
    public static void sendJson(
            HttpServletResponse resp,
            int status,
            Object body
    ) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        objectMapper.writeValue(
                resp.getOutputStream(),
                body
        );
    }
*/
}
