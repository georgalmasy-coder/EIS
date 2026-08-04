package com.bepa.eis.server.api.web.application.views.basis.stakeholderrequirement;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ParentEntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.StakeholderRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.stakeholder.Stakeholder;
import com.bepa.eis.server.dataprovider.fields.strings.StakeholderRequirementCode;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementDescription;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementName;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.CREATE_ENTITY;
import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.EDIT_ENTITY;

@WebServlet(name = "BasisStakeholderRequirementServlet", urlPatterns = {
        "/basis/stakeholderrequirement"
})
@MultipartConfig
public class StakeholderRequirementServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(StakeholderRequirementServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StakeholderRequirementImporters importer = new StakeholderRequirementImporters(webSession, request);
        if ("preview".equalsIgnoreCase(request.getParameter("phase"))) {
            importer.previewImport(response);
            return;
        }

        if (importer.importEntities() <= 0) {
            throw new RuntimeException("No entities imported");
        }
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        try {
            Element basisRequirementDocument = firstChild(rootElement, "stakeholderRequirementDocument");

            if (basisRequirementDocument != null) {
                Element basisRequirement = firstChild(basisRequirementDocument, "stakeholderRequirement");
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element linkSection = firstChild(rootElement, "EntityLinks");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");
                Element relationSection = firstChild(rootElement, "EntityRelations");

                StakeholderRequirementEntity requirementEntity = parseBasisRequirementDocument(webSession, basisRequirement);
                StakeholderRequirementProvider requirementProvider = new StakeholderRequirementProvider(webSession);

                parseNoteDocument(requirementEntity, noteSection);
                parseLinkDocument(requirementEntity, linkSection);
                parseAttachmentDocument(requirementEntity, attachmentSection);
                parseRelationDocument(requirementEntity, relationSection);

                requirementProvider.persist(requirementEntity);

                String eventDescription =
                        requirementEntity.getEntityType().getDescription() + " '" +
                        requirementEntity.getRequirementCode() + " " +
                        requirementEntity.getRequirementName()  + "'" + " has been " + (requirementEntity.getVersion().getValue() == 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(requirementEntity.getEntityType(),
                        requirementEntity.getEntityId().getValue(),
                        eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse stakeholder requirement XML", e);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return listOfRequirements(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return editRequirementById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return createNewRequirement(webSession, parentEntityId);
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid overview request");
    }

    private GenericXmlDocument listOfRequirements(WebSession webSession) {
        try {
            return new StakeholderRequirementList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of basis requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewRequirement(WebSession webSession,Integer parentEntityId) {
        try {
            return new StakeholderRequirementInfo(webSession, CREATE_ENTITY, parentEntityId);
        } catch (Exception e) {
            log.error("Error getting basis requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editRequirementById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new StakeholderRequirementInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting basis requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private StakeholderRequirementEntity parseBasisRequirementDocument(WebSession webSession, Element requirmentElement) {
        StakeholderRequirementEntity requirementEntity = new StakeholderRequirementEntity(webSession);
        requirementEntity.setCustomerId(webSession.getCustomerId());
        requirementEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(requirmentElement, EntityId.FIELD_NAME);
        Integer parentEntityId = intValue(requirmentElement, ParentEntityId.FIELD_NAME);
        Integer version = intValue(requirmentElement, Version.FIELD_NAME);
        String requirementName = textValue(requirmentElement, RequirementName.FIELD_NAME);
        String requirementDescription = textValue(requirmentElement, RequirementDescription.FIELD_NAME);
        Integer stakeholderId = intValue(requirmentElement, Stakeholder.FIELD_NAME);
        Boolean active = boolValue(requirmentElement, Active.FIELD_NAME);

        String requirementCode;
        StakeholderRequirementParentCodeSelector parentCodeSelector = new StakeholderRequirementParentCodeSelector(webSession);

        if (entityId == null && parentEntityId != null) {
            requirementCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        } else {
            requirementCode = textValue(requirmentElement, StakeholderRequirementCode.FIELD_NAME);
        }

        Integer codeLevel = parentCodeSelector.getCodeLevel(requirementCode);

        requirementEntity.setEntityId(entityId);
        requirementEntity.setVersion(version);
        requirementEntity.setRequirementCode(requirementCode);
        requirementEntity.setRequirementCodeLevel(codeLevel);
        requirementEntity.setRequirementName(requirementName);
        requirementEntity.setRequirementDescription(requirementDescription);
        requirementEntity.setStakeholderId(stakeholderId);
        requirementEntity.setActive(active);

        requirementEntity.addAllDataElements();

        return requirementEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<StakeholderRequirementExportRow> rows = fetchRequirementsForExport(webSession, includeInactive);
        GenericExporters genericExporters = new StakeholderRequirementExporters();

        byte[] content;
        String contentType;
        String fileName;

        switch (format) {
            case "csv" -> {
                content = genericExporters.toCsv(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getCsvContentType();
                fileName = buildDownloadFileName(webSession, "Stakeholder Requirement", "csv");
            }
            case "pdf" -> {
                content = genericExporters.toPdf(rows);
                contentType = genericExporters.getPdfContentType();
                fileName = buildDownloadFileName(webSession, "Stakeholder Requirement", "pdf");
            }
            case "xml" -> {
                content = genericExporters.toXml(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getXmlContentType();
                fileName = buildDownloadFileName(webSession, "Stakeholder Requirement", "xml");
            }
            case "xlsx" -> {
                content = genericExporters.toXlsx(rows);
                contentType = genericExporters.getXlsxContentType();
                fileName = buildDownloadFileName(webSession, "Stakeholder Requirement", "xlsx");
            }
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        }

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(content.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(content);
            os.flush();
        }
    }

    private List<StakeholderRequirementExportRow> fetchRequirementsForExport(WebSession webSession, boolean includeInactive) {
        StakeholderRequirementProvider provider = new StakeholderRequirementProvider(webSession);

        return provider.getAllStakeholderRequirement(includeInactive)
                .stream()
                .map(StakeholderRequirementServlet::toExportRow)
                .toList();
    }

    private static StakeholderRequirementExportRow toExportRow(StakeholderRequirementEntity entity) {
        return new StakeholderRequirementExportRow(
                entity.getRequirementCode().getValue(),
                entity.getRequirementCodeLevel().getValue(),
                entity.getRequirementName().getValue(),
                entity.getRequirementDescription().getValue(),
                entity.getChangedByUser(),
                entity.getChangedDate().getValue(),
                entity.isActive()
        );
    }

}
