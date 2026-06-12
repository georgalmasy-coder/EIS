package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

abstract public class AbstractAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AbstractAdminServlet.class);

    abstract public void processGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException;
    abstract public void processPost(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        WebSession webSession = getWebSessionFromRequest(request);

        String module = request.getServletPath();
        long startTime = System.currentTimeMillis();

        try {
            processGet(request, response);
            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            log.error("Error processing request: {}", throwable.getMessage(), throwable);
            throw new ServletException("Error processing request", throwable);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        WebSession webSession = getWebSessionFromRequest(request);

        String module = request.getServletPath();
        long startTime = System.currentTimeMillis();

        try {
            processPost(request, response);
            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            log.error("Error processing request: {}", throwable.getMessage(), throwable);
            throw new ServletException("Error processing request", throwable);

        }
    }

    private WebSession getWebSession(String sessionId) {
        WebSession ws;
        if (GlobalConfiguration.isUdvMode()) {
            ws = new WebSession();
            ws.setId(1);
            ws.setSessionId("georg.almasy@mail.com");
            ws.setCustomerId(GlobalConfiguration.getDefaultCustomerId());
            ws.setProjectId(GlobalConfiguration.getDefaultProjectId());
            ws.setUserId(1);
            return ws;
        } else {
            try {
                SessionProvider sessionProvider = new SessionProvider(null);
                ws = sessionProvider.getBySessionId(sessionId);
            } catch (SQLException e) {

                log.error("Error getting session for page viewer: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return ws;
    }

    public String getSessionIdFromRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return (String) session.getAttribute("sessionID");
    }

    public WebSession getWebSessionFromRequest(HttpServletRequest request) {
        WebSession webSession = null;
        try {
            String sessionId = getSessionIdFromRequest(request);
            webSession = getWebSession(sessionId);
        } catch (Exception e) {
            log.error("Error getting session for page viewer: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return webSession;
    }
}
