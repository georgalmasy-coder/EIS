package com.bepa.eis.server.api.web.application.settings;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet(name = "CustomerSettingsServlet", urlPatterns = "/api/customer-settings")
public class CustomerSettingsServlet extends GenericServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession session = getWebSessionFromRequest(request, response);
        if (session == null) return;
        if (session.getCustomerId() == null) { error(response, 400, "Select a customer first"); return; }
        try { writeSettings(response, session); }
        catch (RuntimeException e) { log("Could not read customer settings", e); error(response, 500, "Could not load settings"); }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession session = getWebSessionFromRequest(request, response);
        if (session == null) return;
        request.setCharacterEncoding("UTF-8");
        try {
            // Posted IDs only detect a changed browser session; they never choose the scope to save.
            if (!Objects.equals(String.valueOf(session.getCustomerId()), request.getParameter("customerId"))
                    || !Objects.equals(Objects.toString(session.getProjectId(), ""), request.getParameter("projectId"))) {
                error(response, 409, "The selected customer or project changed. Reload settings before saving."); return;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (SettingDefinition definition : SettingDefinition.ALL) {
                if (definition.scope().equals("project") && session.getProjectId() == null) continue;
                values.put(definition.key(), request.getParameter(definition.key()));
            }
            new CustomerSettingsProvider(session).save(values);
            EhcacheProvider.clearCacheEntry(session.getCustomerId());
            writeSettings(response, session);
        } catch (IllegalArgumentException e) { error(response, 400, e.getMessage()); }
        catch (SQLException | RuntimeException e) {
            log("Could not save customer settings", e);
            error(response, 500, "Could not save or reload settings. Reload the page to check the saved values.");
        }
    }

    private void writeSettings(HttpServletResponse response, WebSession session) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", session.getCustomerId());
        payload.put("projectId", session.getProjectId());
        payload.put("customerName", CustomerLookupCache.getCustomerInfo(session).getCustomerName());
        var project = CustomerLookupCache.getProjectInfo(session);
        payload.put("projectName", project == null ? "" : project.getProjectName());
        payload.put("definitions", SettingDefinition.ALL);
        payload.put("values", CustomerLookupCache.getSettings(session).values(session.getCustomerId(), session.getProjectId()));
        json(response, 200, payload);
    }

    private void error(HttpServletResponse response, int status, String message) throws IOException {
        json(response, status, Map.of("error", message));
    }

    private void json(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSON.writeValue(response.getWriter(), value);
    }
}
