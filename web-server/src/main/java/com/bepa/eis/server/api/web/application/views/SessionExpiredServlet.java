package com.bepa.eis.server.api.web.application.views;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.server.api.web.application.enums.theme.Theme;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "SessionExpiredServlet", urlPatterns = "/session-expired.html")
public class SessionExpiredServlet extends HttpServlet {

    private static final String TEMPLATE = "html-pages/session-expired.html";
    private static final String THEME_SESSION_ATTRIBUTE = "eis.theme.id";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String html = loadTemplate()
                .replace("{{themeClass}}", resolveTheme(request).getCssClass())
                .replace("{{loginUrl}}", escapeHtml(GlobalConfiguration.getLoginPage()));

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.getWriter().write(html);
    }

    private Theme resolveTheme(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object themeId = session == null ? null : session.getAttribute(THEME_SESSION_ATTRIBUTE);

        if (themeId == null) {
            return Theme.LIGHT;
        }

        try {
            return Theme.fromId(String.valueOf(themeId));
        } catch (RuntimeException ignored) {
            return Theme.LIGHT;
        }
    }

    private String loadTemplate() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE)) {
            if (inputStream == null) {
                throw new IOException("HTML resource not found: " + TEMPLATE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
