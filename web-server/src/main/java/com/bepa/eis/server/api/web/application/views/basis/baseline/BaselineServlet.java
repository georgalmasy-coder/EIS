package com.bepa.eis.server.api.web.application.views.basis.baseline;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.util.List;

@WebServlet(name = "BaselineServlet", urlPatterns = { "/basis/baseline" })
public class BaselineServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(BaselineServlet.class);

    @Override
    public void handleImport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        throw new UnsupportedOperationException("Baseline import is not supported.");
    }

    @Override
    public void handleSave(
            WebSession webSession,
            HttpServletRequest request,
            Element rootElement
    ) throws Exception {
        Element baselineElement = firstChild(rootElement, "baseline");

        if (baselineElement == null) {
            Element baselineDocumentElement = firstChild(rootElement, "baselineDocument");

            if (baselineDocumentElement != null) {
                baselineElement = firstChild(baselineDocumentElement, "baseline");
            }
        }

        if (baselineElement == null) {
            throw new IllegalArgumentException("Baseline data is required.");
        }

        String tagName = textValue(
                baselineElement,
                "tagName"
        );

        String description = textValue(
                baselineElement,
                "description"
        );

        BaselineProvider baselineProvider = new BaselineProvider(webSession);

        baselineProvider.createBaseline(
                tagName,
                description
        );

    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        BaselineProvider baselineProvider = new BaselineProvider(webSession);
        List<Baseline> baselines = baselineProvider.getBaselines();

        BaselineXmlDocument xmlDocument = new BaselineXmlDocument(
                webSession,
                "baselineList"
        );

        Element root = xmlDocument.root();

        appendTopPanel(
                xmlDocument,
                root,
                webSession
        );

        xmlDocument.appendTextElement(
                root,
                "customerId",
                webSession == null ? null : webSession.getCustomerId()
        );

        xmlDocument.appendTextElement(
                root,
                "projectId",
                webSession == null ? null : webSession.getProjectId()
        );

        Element baselinesElement = xmlDocument.appendElement(
                root,
                "baselines"
        );

        for (Baseline baseline : baselines) {
            appendBaseline(
                    xmlDocument,
                    baselinesElement,
                    baseline
            );
        }

        return xmlDocument;
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer baselineId,
            Integer version
    ) throws Throwable {
        BaselineProvider baselineProvider = new BaselineProvider(webSession);
        Baseline baseline = baselineProvider.getBaselineById(baselineId);

        if (baseline == null) {
            throw new IllegalArgumentException("Baseline was not found.");
        }

        BaselineXmlDocument xmlDocument = new BaselineXmlDocument(
                webSession,
                "baselineDetail"
        );

        Element root = xmlDocument.root();

        appendTopPanel(
                xmlDocument,
                root,
                webSession
        );

        appendBaseline(
                xmlDocument,
                root,
                baseline
        );

        appendChangeSection(
                xmlDocument,
                root,
                "stakeholderRequirements",
                baselineProvider.getStakeholderRequirementChanges(baseline)
        );

        appendChangeSection(
                xmlDocument,
                root,
                "systemRequirements",
                baselineProvider.getSystemRequirementChanges(baseline)
        );

        if (true) { // Subscription condition
            appendChangeSection(
                    xmlDocument,
                    root,
                    "functionalStructures",
                    baselineProvider.getFunctionalStructureChanges(baseline)
            );

            appendChangeSection(
                    xmlDocument,
                    root,
                    "logicalStructures",
                    baselineProvider.getLogicalStructureChanges(baseline)
            );
        }

        appendChangeSection(
                xmlDocument,
                root,
                "physicalStructures",
                baselineProvider.getPhysicalStructureChanges(baseline)
        );

        log.debug("BaselineServlet.handleGetEntity: baseline={}", xmlDocument.toXmlString());

        return xmlDocument;
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) throws Throwable {
        BaselineXmlDocument xmlDocument = new BaselineXmlDocument(
                webSession,
                "baselineCreate"
        );

        Element root = xmlDocument.root();

        appendTopPanel(
                xmlDocument,
                root,
                webSession
        );

        Element baseline = xmlDocument.appendElement(
                root,
                "baseline"
        );

        xmlDocument.appendTextElement(
                baseline,
                "tagName",
                ""
        );

        xmlDocument.appendTextElement(
                baseline,
                "description",
                ""
        );

        return xmlDocument;
    }

    @Override
    public void handleExport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        throw new UnsupportedOperationException("Baseline export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        throw new UnsupportedOperationException("Baseline overview is not supported.");
    }

    private void appendTopPanel(
            BaselineXmlDocument xmlDocument,
            Element parent,
            WebSession webSession
    ) throws Exception {
        Element topPanelElement = xmlDocument.appendElement(
                parent,
                "TopPanel"
        );

        if (webSession == null) {
            return;
        }

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession();

        if (topPanel == null || topPanel.getTopPanelElements() == null) {
            return;
        }

        appendListOfElements(
                xmlDocument,
                topPanelElement,
                topPanel.getTopPanelElements()
        );
    }

    private void appendListOfElements(
            BaselineXmlDocument xmlDocument,
            Element parent,
            ListOfElements listOfElements
    ) {
        if (listOfElements == null || listOfElements.getElements() == null) {
            return;
        }

        for (AbstractField field : listOfElements.getElements()) {
            if (field == null || field.getFieldName() == null || field.getFieldName().isBlank()) {
                continue;
            }

            xmlDocument.appendTextElement(
                    parent,
                    field.getFieldName(),
                    field.toString()
            );
        }
    }

    private void appendBaseline(
            BaselineXmlDocument xmlDocument,
            Element parent,
            Baseline baseline
    ) {
        Element baselineElement = xmlDocument.appendElement(
                parent,
                "baseline"
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "baselinePK",
                baseline == null ? null : baseline.getBaselineId()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "customerId",
                baseline == null ? null : baseline.getCustomerId()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "projectId",
                baseline == null ? null : baseline.getProjectId()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "tagName",
                baseline == null ? null : baseline.getTagName()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "description",
                baseline == null ? null : baseline.getDescription()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "changedByUserId",
                baseline == null ? null : baseline.getChangedByUserId()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "changedBy",
                baseline == null ? null : baseline.getChangedBy()
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "changedDateTime",
                baseline == null ? null : timestampValue(baseline.getChangedDateTime())
        );

        xmlDocument.appendTextElement(
                baselineElement,
                "previousBaselineDateTime",
                baseline == null ? null : timestampValue(baseline.getPreviousBaselineDateTime())
        );

    }

    private void appendChangeSection(
            BaselineXmlDocument xmlDocument,
            Element parent,
            String sectionName,
            List<BaselineChangeRow> rows
    ) {
        Element sectionElement = xmlDocument.appendElement(
                parent,
                sectionName
        );

        if (rows == null) {
            return;
        }

        for (BaselineChangeRow row : rows) {
            Element rowElement = xmlDocument.appendElement(
                    sectionElement,
                    "change"
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "activity",
                    row.getActivity()
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "id",
                    row.getId()
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "name",
                    row.getName()
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "lastModified",
                    timestampValue(row.getLastModified())
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "lastModifiedBy",
                    row.getLastModifiedBy()
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "entityType",
                    row.getEntityType().getId()
            );

            xmlDocument.appendTextElement(
                    rowElement,
                    "entityId",
                    row.getEntityId()
            );
        }
    }

    private String timestampValue(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toLocalDateTime().toString();
    }
}