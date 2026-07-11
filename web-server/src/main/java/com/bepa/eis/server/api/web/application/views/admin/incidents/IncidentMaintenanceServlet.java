package com.bepa.eis.server.api.web.application.views.admin.incidents;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "IncidentMaintenanceServlet", urlPatterns = {"/api/admin/incidents"})
public class IncidentMaintenanceServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(IncidentMaintenanceServlet.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Incident import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        throw new UnsupportedOperationException("Incident save is not supported.");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return buildDocument(webSession, request);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) {
        throw new UnsupportedOperationException("Incident edit is not supported.");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) {
        throw new UnsupportedOperationException("Incident create is not supported.");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Incident export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return handleListOfEntities(webSession, request, response);
    }

    private IncidentMaintenanceXmlDocument buildDocument(WebSession webSession, HttpServletRequest request) {
        try {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            int limit = resolveLimit(request);

            List<IncidentProvider.RecentIncidentDetail> incidents = incidentProvider.getRecentIncidentsWithTrace(limit);

            IncidentMaintenanceXmlDocument xmlDocument = new IncidentMaintenanceXmlDocument(webSession, "incidentMaintenance");
            Element root = xmlDocument.root();

            appendTopPanel(xmlDocument, root, webSession);
            appendIncidents(xmlDocument, root, incidents);

            return xmlDocument;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load incidents.", e);
        }
    }

    private void appendTopPanel(IncidentMaintenanceXmlDocument xmlDocument, Element parent, WebSession webSession) {
        Element topPanelElement = xmlDocument.appendElement(parent, "TopPanel");

        if (webSession == null) {
            return;
        }

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession();

            if (topPanel != null && topPanel.getTopPanelElements() != null) {
                for (com.bepa.eis.server.dataprovider.fields.AbstractField field : topPanel.getTopPanelElements().getElements()) {
                    if (field != null && field.getFieldName() != null && !field.getFieldName().isBlank()) {
                        xmlDocument.appendTextElement(topPanelElement, field.getFieldName(), field.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to append top panel for incidents page", e);
        }
    }

    private void appendIncidents(
            IncidentMaintenanceXmlDocument xmlDocument,
            Element parent,
            List<IncidentProvider.RecentIncidentDetail> incidents
    ) {
        Element incidentsElement = xmlDocument.appendElement(parent, "incidents");

        for (IncidentProvider.RecentIncidentDetail incident : incidents) {
            appendIncident(xmlDocument, incidentsElement, incident);
        }
    }

    private void appendIncident(
            IncidentMaintenanceXmlDocument xmlDocument,
            Element parent,
            IncidentProvider.RecentIncidentDetail incident
    ) {
        if (incident == null) {
            return;
        }

        Element incidentElement = xmlDocument.appendElement(parent, "incident");
        xmlDocument.appendTextElement(incidentElement, "IncidentId", incident.incidentId());
        xmlDocument.appendTextElement(incidentElement, "LogCreated", incident.logCreated());
        xmlDocument.appendTextElement(incidentElement, "Customer", incident.customer());
        xmlDocument.appendTextElement(incidentElement, "Project", incident.project());
        xmlDocument.appendTextElement(incidentElement, "User", incident.user());
        xmlDocument.appendTextElement(incidentElement, "ServiceType", incident.serviceType());
        xmlDocument.appendTextElement(incidentElement, "SeverityType", incident.severityType());
        xmlDocument.appendTextElement(incidentElement, "Module", incident.module());
        xmlDocument.appendTextElement(incidentElement, "Message", incident.message());
        xmlDocument.appendTextElement(incidentElement, "Trace", incident.trace());
    }

    private int resolveLimit(HttpServletRequest request) {
        String rawLimit = request.getParameter("count");

        if (rawLimit == null || rawLimit.isBlank()) {
            return DEFAULT_LIMIT;
        }

        try {
            int parsed = Integer.parseInt(rawLimit.trim());
            return Math.max(1, Math.min(parsed, MAX_LIMIT));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }
}
