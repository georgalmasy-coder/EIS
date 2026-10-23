package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.UserPreferenceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet(name = "HelpServlet", urlPatterns = "/api/help")
public class HelpServlet extends GenericServlet {

    private static final Pattern SAFE_HELP_PAGE_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String page = request.getParameter("page");

        if (page == null || page.isBlank() || !SAFE_HELP_PAGE_NAME.matcher(page).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid help page.");
            return;
        }

        HelpLanguage language = HelpLanguage.ENGLISH;
        if (isLanguageSelectionEnabled()) {
            try {
                language = readHelpLanguage(request, response);
                if (language == null) {
                    return;
                }
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not read help language.");
                return;
            }
        }
        ServletContext servletContext = getServletContext();

        InputStream resource = servletContext.getResourceAsStream(language.resourcePath(page));
        if (resource == null && language != HelpLanguage.ENGLISH) {
            language = HelpLanguage.ENGLISH;
            resource = servletContext.getResourceAsStream(language.resourcePath(page));
        }
        try (InputStream inputStream = resource) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Help page not found.");
                return;
            }

            byte[] bytes = inputStream.readAllBytes();

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/markdown; charset=UTF-8");
            response.setHeader("Content-Language", language.languageTag());
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.getOutputStream().write(bytes);
        }
    }

    protected boolean isLanguageSelectionEnabled() {
        return GlobalConfiguration.isUdvMode();
    }

    protected HelpLanguage readHelpLanguage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, SQLException {
        WebSession session = getWebSessionFromRequest(request, response);
        if (session == null) {
            return null;
        }
        return HelpLanguage.fromPreference(new UserPreferenceProvider(session)
                .getSetting(session.getUserId(), HelpLanguage.PREFERENCE_KEY));
    }
}
