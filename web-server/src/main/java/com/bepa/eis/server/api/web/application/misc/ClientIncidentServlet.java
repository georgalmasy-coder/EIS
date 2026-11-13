package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "ClientIncidentServlet", urlPatterns = "/api/incidents/client")
public class ClientIncidentServlet extends GenericServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RATE_ATTRIBUTE = ClientIncidentServlet.class.getName() + ".rate";
    private static final int MAX_BODY = 48_000;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        // A missing/expired session must not redirect telemetry to an HTML page.
        if (!GlobalConfiguration.isUdvMode() && getSessionIdFromRequest(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        WebSession session = getSession(request);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!allowReport(request.getSession())) {
            response.setStatus(429);
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY * 4L) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        var reader = request.getReader();
        char[] buffer = new char[2048];
        StringBuilder payload = new StringBuilder();
        int count;
        while (payload.length() <= MAX_BODY && (count = reader.read(buffer, 0,
                Math.min(buffer.length, MAX_BODY + 1 - payload.length()))) != -1) {
            payload.append(buffer, 0, count);
        }
        String body = payload.toString();
        if (body.length() > MAX_BODY) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        JsonNode incident;
        try {
            incident = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (incident == null || !incident.isObject()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String message = text(incident, "message", 1000);
        PageType page = PageType.mapToType(text(incident, "page", 100));
        if (message.isBlank() || page == PageType.NONE) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String file = text(incident, "file", 1000);
        String line = text(incident, "line", 10);
        String column = text(incident, "column", 10);
        StringBuilder trace = new StringBuilder();
        for (String field : new String[]{"kind", "url", "parentUrl", "dialogId", "build", "userAgent", "time", "gesture"}) {
            trace.append(field).append(": ").append(text(incident, field, 1000)).append('\n');
        }
        trace.append("source: ").append(file).append(':').append(line).append(':').append(column)
                .append("\n\n").append(text(incident, "stack", 16_000));
        // Identity comes only from the authenticated server session. Trace is the original JS stack.
        createIncidentProvider(session).createJavaScriptIncident("frontend." + page.getPageName(), message,
                truncate(file + ":" + line + ":" + column, 250), trace.toString());
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected IncidentProvider createIncidentProvider(WebSession session) {
        return new IncidentProvider(session);
    }

    private static String text(JsonNode node, String name, int maxLength) {
        JsonNode value = node.get(name);
        return value != null && value.isValueNode() && !value.isNull() ? truncate(value.asText(), maxLength) : "";
    }

    private static String truncate(String value, int maxLength) {
        return value.substring(0, Math.min(value.length(), maxLength));
    }

    private static boolean allowReport(HttpSession session) {
        synchronized (session) {
            long now = System.currentTimeMillis();
            long[] rate = (long[]) session.getAttribute(RATE_ATTRIBUTE);
            if (rate == null || now - rate[0] >= 60_000) {
                rate = new long[]{now, 0};
                session.setAttribute(RATE_ATTRIBUTE, rate);
            }
            return ++rate[1] <= 20;
        }
    }
}
