package com.bepa.eis.server.api.web.application.views.basis.lookupmaintenance;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@WebServlet(name = "LookupValueMaintenanceServlet", urlPatterns = {"/basis/lookup-maintenance"})
public class LookupValueMaintenanceServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(LookupValueMaintenanceServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Lookup maintenance import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        LookupValueMaintenanceProvider provider = new LookupValueMaintenanceProvider(webSession);
        Element lookupElement = firstChild(rootElement, "lookupRow");

        if (lookupElement == null && "lookupRow".equalsIgnoreCase(rootElement.getTagName())) {
            lookupElement = rootElement;
        }

        if (lookupElement == null) {
            throw new IllegalArgumentException("Lookup row data is required.");
        }

        Boolean activeValue = boolValue(lookupElement, "Active");

        LookupValueMaintenanceProvider.LookupRow lookupRow = new LookupValueMaintenanceProvider.LookupRow(
                parseLookupType(textValue(lookupElement, "LookupType")),
                intValue(lookupElement, "LookupId"),
                intValue(lookupElement, "LookupLevel"),
                textValue(lookupElement, "LookupCode"),
                textValue(lookupElement, "LookupName"),
                textValue(lookupElement, "LookupDescription"),
                activeValue == null || activeValue,
                textValue(lookupElement, "Color")
        );

        provider.saveLookupRow(lookupRow);
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        LookupValueMaintenanceProvider provider = new LookupValueMaintenanceProvider(webSession);
        return buildDocument(webSession, provider);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) {
        throw new UnsupportedOperationException("Lookup maintenance edit endpoint is not supported.");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) {
        throw new UnsupportedOperationException("Lookup maintenance create endpoint is not supported.");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Lookup maintenance export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        return handleListOfEntities(webSession, request, response);
    }

    private LookupValueMaintenanceXmlDocument buildDocument(
            WebSession webSession,
            LookupValueMaintenanceProvider provider
    ) {
        LookupValueMaintenanceXmlDocument xmlDocument = new LookupValueMaintenanceXmlDocument(webSession, "lookupMaintenance");
        Element root = xmlDocument.root();

        appendTopPanel(xmlDocument, root, webSession);

        LookupValueMaintenanceProvider.LookupMaintenanceData data = provider.getLookupMaintenanceData();
        appendRows(xmlDocument, root, "trlRows", "trlRow", data.trlRows());
        appendRows(xmlDocument, root, "irlRows", "irlRow", data.irlRows());
        appendRows(xmlDocument, root, "srlRows", "srlRow", data.srlRows());

        return xmlDocument;
    }

    private void appendTopPanel(LookupValueMaintenanceXmlDocument xmlDocument, Element parent, WebSession webSession) {
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
            log.warn("Unable to append top panel", e);
        }
    }

    private void appendRows(
            LookupValueMaintenanceXmlDocument xmlDocument,
            Element parent,
            String containerName,
            String rowName,
            java.util.List<LookupValueMaintenanceProvider.LookupRow> rows
    ) {
        Element container = xmlDocument.appendElement(parent, containerName);

        for (LookupValueMaintenanceProvider.LookupRow row : rows) {
            appendRow(xmlDocument, container, rowName, row);
        }
    }

    private void appendRow(
            LookupValueMaintenanceXmlDocument xmlDocument,
            Element parent,
            String rowName,
            LookupValueMaintenanceProvider.LookupRow row
    ) {
        if (row == null) {
            return;
        }

        Element rowElement = xmlDocument.appendElement(parent, rowName);
        xmlDocument.appendTextElement(rowElement, "LookupType", row.lookupType() == null ? "" : row.lookupType().name());
        xmlDocument.appendTextElement(rowElement, "LookupId", row.rowId());
        xmlDocument.appendTextElement(rowElement, "LookupLevel", row.level());
        xmlDocument.appendTextElement(rowElement, "LookupCode", row.code());
        xmlDocument.appendTextElement(rowElement, "LookupName", row.name());
        xmlDocument.appendTextElement(rowElement, "LookupDescription", row.description());
        xmlDocument.appendTextElement(rowElement, "Active", row.active());
        xmlDocument.appendTextElement(rowElement, "Color", row.color());
    }

    private LookupValueMaintenanceProvider.LookupType parseLookupType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LookupType is required.");
        }

        return LookupValueMaintenanceProvider.LookupType.valueOf(value.trim().toUpperCase());
    }
}
