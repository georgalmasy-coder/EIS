package com.bepa.eis.server.api.web.application.views.pro.dashboard.systemsteamwork;

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

@WebServlet(name = "DashboardSystemsTeamworkServlet", urlPatterns = {"/pro/systemsteamwork"})
@MultipartConfig
public class DashboardSystemsTeamworkServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(DashboardSystemsTeamworkServlet.class);

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return buildSystemsTeamworkDocument(webSession);
    }

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new RuntimeException("Invalid import request");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        throw new RuntimeException("Invalid save request");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid list request");
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        throw new RuntimeException("Invalid edit request");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        throw new RuntimeException("Invalid create request");
    }

    private GenericXmlDocument buildSystemsTeamworkDocument(WebSession webSession) {
        try {
            return new DashboardSystemsTeamworkDocument(webSession);
        } catch (Exception e) {
            log.error("Error getting dashboard IRL : {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new RuntimeException("Invalid export request");
    }

}