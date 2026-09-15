package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ParentEntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.LogicalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.logical.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
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

@WebServlet(name = "LogicalStructureServlet", urlPatterns = {
        "/pro/logicalstructure"
})
@MultipartConfig
public class LogicalStructureServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(LogicalStructureServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LogicalStructureImporters importer = new LogicalStructureImporters(webSession, request);
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
            Element logicalStructureDocument = firstChild(rootElement, "logicalStructure");

            if (logicalStructureDocument != null) {
                Element logicalStructure = firstChild(logicalStructureDocument, "logicalDocument");
                if (logicalStructure == null) {
                    logicalStructure = firstChild(logicalStructureDocument, "logicalStructure");
                }
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element linkSection = firstChild(rootElement, "EntityLinks");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");
                Element relationSection = firstChild(rootElement, "EntityRelations");

                LogicalStructureEntity logicalStructureEntity = parseLogicalStructureDocument(webSession, logicalStructure);
                LogicalStructureProvider logicalStructureProvider = new LogicalStructureProvider(webSession);

                parseNoteDocument(logicalStructureEntity, noteSection);
                parseLinkDocument(logicalStructureEntity, linkSection);
                parseAttachmentDocument(logicalStructureEntity, attachmentSection);
                parseRelationDocument(logicalStructureEntity, relationSection);

                logicalStructureProvider.persist(logicalStructureEntity);

                String eventDescription =
                        logicalStructureEntity.getEntityType().getDescription() + " '" +
                        logicalStructureEntity.getLogicalCode() + " " +
                        logicalStructureEntity.getLogicalName()  + "'" + " has been " + (logicalStructureEntity.getVersion().getValue() == 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(logicalStructureEntity.getEntityType(),
                        logicalStructureEntity.getEntityId().getValue(),
                        eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse logical structure XML", e);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return listOfLogicalStructures(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return editLogicalStructureById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return createNewLogicalStructure(webSession, parentEntityId);
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid overview request");
    }

    private GenericXmlDocument listOfLogicalStructures(WebSession webSession) {
        try {
            return new LogicalStructureList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of logical structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewLogicalStructure(WebSession webSession, Integer parentEntityId) {
        try {
            return new LogicalStructureInfo(webSession, CREATE_ENTITY, parentEntityId);
        } catch (Exception e) {
            log.error("Error getting logical structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editLogicalStructureById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new LogicalStructureInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting logical structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private LogicalStructureEntity parseLogicalStructureDocument(WebSession webSession, Element logicalStructureElement) {
        LogicalStructureEntity logicalStructureEntity = new LogicalStructureEntity(webSession);
        logicalStructureEntity.setCustomerId(webSession.getCustomerId());
        logicalStructureEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(logicalStructureElement, EntityId.FIELD_NAME);
        Integer parentEntityId = intValue(logicalStructureElement, ParentEntityId.FIELD_NAME);
        Integer version = intValue(logicalStructureElement, Version.FIELD_NAME);
        String logicalName = textValue(logicalStructureElement, LogicalName.FIELD_NAME);
        String logicalDescription = textValue(logicalStructureElement, LogicalDescription.FIELD_NAME);
        Integer verificationStatusId = intValue(logicalStructureElement, LogicalVerificationStatus.FIELD_NAME);
        Integer ownerId = intValue(logicalStructureElement, LogicalOwner.FIELD_NAME);
        Integer logicalCriticalityId = intValue(logicalStructureElement, LogicalCriticality.FIELD_NAME);

        Integer logicalElementCategoryId = intValue(logicalStructureElement, LogicalElementCategory.FIELD_NAME);
        Integer logicalLevelId = intValue(logicalStructureElement, LogicalLevel.FIELD_NAME);
        Integer logicalTypeId = intValue(logicalStructureElement, LogicalType.FIELD_NAME);
        Integer logicalResponsibleDomainId = intValue(logicalStructureElement, LogicalResponsibleDomain.FIELD_NAME);
        Integer logicalLifecycleStatusId = intValue(logicalStructureElement, LogicalLifecycleStatus.FIELD_NAME);
        Integer logicalMaturityId = intValue(logicalStructureElement, LogicalMaturity.FIELD_NAME);
        Integer logicalAllocationStatusId = intValue(logicalStructureElement, LogicalAllocationStatus.FIELD_NAME);
        Integer logicalRealizationStatusId = intValue(logicalStructureElement, LogicalRealizationStatus.FIELD_NAME);
        Integer logicalSafetyClassificationId = intValue(logicalStructureElement, LogicalSafetyClassification.FIELD_NAME);
        Integer logicalSecurityClassificationId = intValue(logicalStructureElement, LogicalSecurityClassification.FIELD_NAME);
        Integer logicalRedundancyTypeId = intValue(logicalStructureElement, LogicalRedundancyType.FIELD_NAME);
        Integer logicalConfigurationVariantId = intValue(logicalStructureElement, LogicalConfigurationVariant.FIELD_NAME);
        Integer logicalApplicabilityId = intValue(logicalStructureElement, LogicalApplicability.FIELD_NAME);

        Boolean active = boolValue(logicalStructureElement, Active.FIELD_NAME);

        String logicalCode;
        LogicalStructureParentCodeSelector parentCodeSelector = new LogicalStructureParentCodeSelector(webSession);

        if (entityId == null && parentEntityId != null) {
            logicalCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        } else {
            logicalCode = textValue(logicalStructureElement, LogicalCode.FIELD_NAME);
        }

        Integer codeLevel = parentCodeSelector.getCodeLevel(logicalCode);

        logicalStructureEntity.setEntityId(entityId);
        logicalStructureEntity.setVersion(version);
        logicalStructureEntity.setLogicalCode(logicalCode);
        logicalStructureEntity.setLogicalCodeLevel(codeLevel);
        logicalStructureEntity.setLogicalName(logicalName);
        logicalStructureEntity.setLogicalDescription(logicalDescription);
        logicalStructureEntity.setLogicalVerificationStatusId(verificationStatusId);
        logicalStructureEntity.setOwner(ownerId);
        logicalStructureEntity.setLogicalCriticalityId(logicalCriticalityId);
        logicalStructureEntity.setLogicalElementCategoryId(logicalElementCategoryId);
        logicalStructureEntity.setLogicalLevelId(logicalLevelId);
        logicalStructureEntity.setLogicalTypeId(logicalTypeId);
        logicalStructureEntity.setLogicalResponsibleDomainId(logicalResponsibleDomainId);
        logicalStructureEntity.setLogicalLifecycleStatusId(logicalLifecycleStatusId);
        logicalStructureEntity.setLogicalMaturityId(logicalMaturityId);
        logicalStructureEntity.setLogicalAllocationStatusId(logicalAllocationStatusId);
        logicalStructureEntity.setLogicalRealizationStatusId(logicalRealizationStatusId);
        logicalStructureEntity.setLogicalSafetyClassificationId(logicalSafetyClassificationId);
        logicalStructureEntity.setLogicalSecurityClassificationId(logicalSecurityClassificationId);
        logicalStructureEntity.setLogicalRedundancyTypeId(logicalRedundancyTypeId);
        logicalStructureEntity.setLogicalConfigurationVariantId(logicalConfigurationVariantId);
        logicalStructureEntity.setLogicalApplicabilityId(logicalApplicabilityId);

        logicalStructureEntity.setActive(active);

        logicalStructureEntity.addAllDataElements();

        return logicalStructureEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<LogicalStructureExportRow> rows = fetchLogicalStructuresForExport(webSession, includeInactive);
        GenericExporters genericExporters = new LogicalStructureExporters();

        byte[] content;
        String contentType;
        String fileName;

        switch (format) {
            case "csv" -> {
                content = genericExporters.toCsv(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getCsvContentType();
                fileName = buildDownloadFileName(webSession, "Logical Architecture", "csv");
            }
            case "pdf" -> {
                content = genericExporters.toPdf(rows);
                contentType = genericExporters.getPdfContentType();
                fileName = buildDownloadFileName(webSession, "Logical Architecture", "pdf");
            }
            case "xml" -> {
                content = genericExporters.toXml(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getXmlContentType();
                fileName = buildDownloadFileName(webSession, "Logical Architecture", "xml");
            }
            case "xlsx" -> {
                content = genericExporters.toXlsx(rows);
                contentType = genericExporters.getXlsxContentType();
                fileName = buildDownloadFileName(webSession, "Logical Architecture", "xlsx");
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

    private List<LogicalStructureExportRow> fetchLogicalStructuresForExport(WebSession webSession, boolean includeInactive) {
        LogicalStructureProvider provider = new LogicalStructureProvider(webSession);

        return provider.getAllLogicalStructures(includeInactive)
                .stream()
                .map(LogicalStructureServlet::toExportRow)
                .toList();
    }

    private static LogicalStructureExportRow toExportRow(LogicalStructureEntity entity) {
        return new LogicalStructureExportRow(
                entity.getLogicalCode().getValue(),
                entity.getLogicalCodeLevel().getValue(),
                entity.getLogicalName().getValue(),
                entity.getLogicalDescription().getValue(),
                entity.getChangedByUser(),
                entity.getChangedDate().getValue(),
                entity.isActive()
        );
    }

}
