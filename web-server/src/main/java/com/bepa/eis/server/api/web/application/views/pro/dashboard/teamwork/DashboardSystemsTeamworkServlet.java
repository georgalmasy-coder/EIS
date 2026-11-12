package com.bepa.eis.server.api.web.application.views.pro.dashboard.teamwork;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@WebServlet(name = "DashboardSystemsTeamworkServlet", urlPatterns = {"/master/systemsteamwork"})
@MultipartConfig
public class DashboardSystemsTeamworkServlet extends DashboardTeamworkServlet {

    private static final Logger log = LoggerFactory.getLogger(DashboardSystemsTeamworkServlet.class);

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return buildSystemsTeamworkDocument(webSession);
    }

    private GenericXmlDocument buildSystemsTeamworkDocument(WebSession webSession) {
        try {
            return new DashboardSystemsTeamworkDocument(webSession);
        } catch (Exception e) {
            log.error("Error getting systems teamwork dashboard: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
