package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.UserPreferenceProvider;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.server.api.generic.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

@WebServlet(name = "UserPreferencesServlet", urlPatterns = "/api/user-preferences")
public class UserPreferencesServlet extends GenericServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession webSession = getWebSessionFromRequest(request, response);
        if (webSession == null) {
            return;
        }

        try {
            Map<String, String> settings = new UserPreferenceProvider(webSession).getSettings(webSession.getUserId());
            JsonUtil.writeJson(response, HttpServletResponse.SC_OK, toSettingsJson(settings));
        } catch (SQLException e) {
            JsonUtil.writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"error\":\"Could not read user preferences\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession webSession = getWebSessionFromRequest(request, response);
        if (webSession == null) {
            return;
        }

        String key = request.getParameter("key");
        String value = request.getParameter("value");
        boolean remove = "true".equalsIgnoreCase(request.getParameter("remove"));

        try {
            new UserPreferenceProvider(webSession).setSetting(webSession.getUserId(), key, remove ? null : value);
            JsonUtil.writeJson(response, HttpServletResponse.SC_OK, "{\"ok\":true}");
        } catch (IllegalArgumentException e) {
            JsonUtil.writeJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            JsonUtil.writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"error\":\"Could not write user preference\"}");
        }
    }

    private String toSettingsJson(Map<String, String> settings) {
        StringBuilder json = new StringBuilder("{\"settings\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            if (!first) {
                json.append(",");
            }
            JsonUtil.appendJsonString(json, entry.getKey(), entry.getValue());
            first = false;
        }
        json.append("}}");
        return json.toString();
    }
}
