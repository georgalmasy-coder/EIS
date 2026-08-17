package com.bepa.eis.server.api.web.application.views.basis.lookup;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.List;

@WebServlet(name = "LookupServlet", urlPatterns = {"/basis/lookup"})
public class LookupServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(LookupServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Lookup import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        LookupMaintenanceProvider provider = new LookupMaintenanceProvider(webSession);
        Element lookupElement = firstChild(rootElement, "lookup");

        if (lookupElement == null && "lookup".equalsIgnoreCase(rootElement.getTagName())) {
            lookupElement = rootElement;
        }

        if (lookupElement == null) {
            throw new IllegalArgumentException("Lookup data is required.");
        }

        Boolean activeValue = boolValue(lookupElement, "Active");

        LookupMaintenanceProvider.LookupRow lookupRow = new LookupMaintenanceProvider.LookupRow(
                intValue(lookupElement, "LookupId"),
                intValue(lookupElement, "LookupType"),
                textValue(lookupElement, "LookupCode"),
                textValue(lookupElement, "LookupDescription"),
                textValue(lookupElement, "Color"),
                intValue(lookupElement, "DisplayOrder"),
                activeValue == null || activeValue
        );

        provider.saveLookup(lookupRow);
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        LookupMaintenanceProvider provider = new LookupMaintenanceProvider(webSession);
        Integer lookupTypeId = resolveLookupTypeId(request, provider);
        return buildDocument(webSession, provider, lookupTypeId, null);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) {
        LookupMaintenanceProvider provider = new LookupMaintenanceProvider(webSession);
        LookupMaintenanceProvider.LookupRow lookupRow = provider.getLookupById(entityId);

        if (lookupRow == null) {
            throw new IllegalArgumentException("Lookup row was not found.");
        }

        return buildDocument(webSession, provider, lookupRow.lookupTypeId(), lookupRow);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) {
        LookupMaintenanceProvider provider = new LookupMaintenanceProvider(webSession);
        Integer lookupTypeId = resolveLookupTypeId(request, provider);

        LookupMaintenanceProvider.LookupRow lookupRow = new LookupMaintenanceProvider.LookupRow(
                null,
                lookupTypeId,
                "",
                "",
                "",
                null,
                true
        );

        return buildDocument(webSession, provider, lookupTypeId, lookupRow);
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Lookup export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("Lookup overview is not supported.");
    }

    private GenericXmlDocument buildDocument(
            WebSession webSession,
            LookupMaintenanceProvider provider,
            Integer selectedLookupTypeId,
            LookupMaintenanceProvider.LookupRow lookupRow
    ) {
        LookupMaintenanceXmlDocument xmlDocument = new LookupMaintenanceXmlDocument(webSession, "lookupMaintenance");
        Element root = xmlDocument.root();

        appendTopPanel(xmlDocument, root, webSession);
        appendLookupTypes(xmlDocument, root, provider.getLookupTypes());

        LookupMaintenanceProvider.LookupTypeRow selectedType = provider.getLookupTypeById(selectedLookupTypeId);

        xmlDocument.appendTextElement(root, "selectedLookupTypeId", selectedLookupTypeId);
        xmlDocument.appendTextElement(root, "selectedLookupTypeDesc", selectedType == null ? "" : selectedType.lookupTypeDesc());

        if (lookupRow == null && selectedLookupTypeId != null) {
            appendLookups(xmlDocument, root, provider.getLookups(selectedLookupTypeId));
        } else {
            appendLookup(xmlDocument, root, lookupRow);
            if (selectedLookupTypeId != null) {
                appendLookups(xmlDocument, root, provider.getLookups(selectedLookupTypeId));
            }
        }

        return xmlDocument;
    }

    private void appendTopPanel(LookupMaintenanceXmlDocument xmlDocument, Element parent, WebSession webSession) {
        Element topPanelElement = xmlDocument.appendElement(parent, "TopPanel");

        if (webSession == null) {
            return;
        }

        try {
            com.bepa.eis.server.api.web.application.views.common.TopPanelProvider topPanelProvider =
                    new com.bepa.eis.server.api.web.application.views.common.TopPanelProvider(webSession);
            com.bepa.eis.server.api.DTO.TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.LOOKUP_MAINTENANCE_PAGE);

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

    private void appendLookupTypes(
            LookupMaintenanceXmlDocument xmlDocument,
            Element parent,
            List<LookupMaintenanceProvider.LookupTypeRow> lookupTypes
    ) {
        Element lookupTypesElement = xmlDocument.appendElement(parent, "lookupTypes");

        for (LookupMaintenanceProvider.LookupTypeRow lookupType : lookupTypes) {
            Element lookupTypeElement = xmlDocument.appendElement(lookupTypesElement, "lookupType");
            xmlDocument.appendTextElement(lookupTypeElement, "LookupTypeId", lookupType.lookupTypeId());
            xmlDocument.appendTextElement(lookupTypeElement, "LookupTypeDesc", lookupType.lookupTypeDesc());
        }
    }

    private void appendLookups(
            LookupMaintenanceXmlDocument xmlDocument,
            Element parent,
            List<LookupMaintenanceProvider.LookupRow> lookups
    ) {
        Element lookupsElement = xmlDocument.appendElement(parent, "lookups");

        for (LookupMaintenanceProvider.LookupRow lookup : lookups) {
            appendLookup(xmlDocument, lookupsElement, lookup);
        }
    }

    private void appendLookup(
            LookupMaintenanceXmlDocument xmlDocument,
            Element parent,
            LookupMaintenanceProvider.LookupRow lookup
    ) {
        if (lookup == null) {
            return;
        }

        Element lookupElement = xmlDocument.appendElement(parent, "lookup");
        xmlDocument.appendTextElement(lookupElement, "LookupId", lookup.lookupId());
        xmlDocument.appendTextElement(lookupElement, "LookupType", lookup.lookupTypeId());
        xmlDocument.appendTextElement(lookupElement, "LookupCode", lookup.lookupCode());
        xmlDocument.appendTextElement(lookupElement, "LookupDescription", lookup.lookupDescription());
        xmlDocument.appendTextElement(lookupElement, "Color", lookup.color());
        xmlDocument.appendTextElement(lookupElement, "DisplayOrder", lookup.displayOrder());
        xmlDocument.appendTextElement(lookupElement, "Active", lookup.active());
    }

    private Integer resolveLookupTypeId(HttpServletRequest request, LookupMaintenanceProvider provider) {
        Integer lookupTypeId = toInteger(request.getParameter("lookupTypeId"));

        if (lookupTypeId != null && provider.getLookupTypeById(lookupTypeId) != null) {
            return lookupTypeId;
        }

        lookupTypeId = toInteger(request.getParameter("type"));
        if (lookupTypeId != null && provider.getLookupTypeById(lookupTypeId) != null) {
            return lookupTypeId;
        }

        List<LookupMaintenanceProvider.LookupTypeRow> lookupTypes = provider.getLookupTypes();
        return lookupTypes.isEmpty() ? null : lookupTypes.get(0).lookupTypeId();
    }
}
