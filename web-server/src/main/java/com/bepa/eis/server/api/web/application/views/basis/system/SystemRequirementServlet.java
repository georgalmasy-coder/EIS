package com.bepa.eis.server.api.web.application.views.basis.system;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.booleans.RelevantToStakeholderRequirement;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ParentEntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemRequirementParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.*;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.RequirementCaptureDate;
import com.bepa.eis.server.entites.systemsystemrequirement.SystemRequirementEntity;
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
import static com.bepa.eis.common.enums.entity.EntityDataElement.SYSTEMREQCODE;

@WebServlet(name = "BasisSystemRequirementServlet", urlPatterns = { "/basis/systemrequirement" })
@MultipartConfig

public class SystemRequirementServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(SystemRequirementServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        BasisSystemRequirementImporters importer = new BasisSystemRequirementImporters(webSession, request);
        if (importer.importEntities() <= 0) {
            throw new RuntimeException("No entities imported");
        }
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        try {
            Element systemRequirementDocument = firstChild(rootElement, "systemRequirementDocument");

            if (systemRequirementDocument != null) {
                Element systemRequirement = firstChild(systemRequirementDocument, "systemRequirement");
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");

                SystemRequirementEntity systemRequirementEntity = parseBasisSystemRequirementDocument(webSession, systemRequirement);
                SystemRequirementProvider requirementProvider = new SystemRequirementProvider(webSession);

                parseNoteDocument(systemRequirementEntity, noteSection);
                parseAttachmentDocument(systemRequirementEntity, attachmentSection);

                requirementProvider.persist(systemRequirementEntity);

                String eventDescription =
                        systemRequirementEntity.getEntityType().getDescription() + " '" +
                        systemRequirementEntity.getRequirementCode() + " " +
                        systemRequirementEntity.getRequirementName() + "' has been " + (systemRequirementEntity.getVersion() == 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(systemRequirementEntity.getEntityType(),
                                                      systemRequirementEntity.getEntityId(),
                                                      eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse system requirement XML", e);
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
            return new BasisSystemRequirementList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of system requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewRequirement(WebSession webSession, Integer parentEntityId) {
        try {
            return new BasisSystemRequirementInfo(webSession, CREATE_ENTITY, parentEntityId);
        } catch (Exception e) {
            log.error("Error getting system requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editRequirementById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new BasisSystemRequirementInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting system requirement info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private SystemRequirementEntity parseBasisSystemRequirementDocument(WebSession webSession, Element requirmentElement) {
        SystemRequirementEntity systemRequirementEntity = new SystemRequirementEntity(webSession);
        systemRequirementEntity.setCustomerId(webSession.getCustomerId());
        systemRequirementEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(requirmentElement, EntityId.FIELD_NAME);
        Integer parentEntityId = intValue(requirmentElement, ParentEntityId.FIELD_NAME);
        Integer version = intValue(requirmentElement, Version.FIELD_NAME);
        String requirementName = textValue(requirmentElement, RequirementName.FIELD_NAME);
        String requirementDescription = textValue(requirmentElement, RequirementDescription.FIELD_NAME);

        Integer verificationStatusId = intValue(requirmentElement, RequirementVerificationStatus.FIELD_NAME);
        Integer businessPriorityId = intValue(requirmentElement, RequirementBusinessPriority.FIELD_NAME);
        Integer statusId = intValue(requirmentElement, RequirementStatus.FIELD_NAME);
        Integer ownerId = intValue(requirmentElement, RequirementOwner.FIELD_NAME);
        String rationalStatement = textValue(requirmentElement, RequirementRationaleStatement.FIELD_NAME);
        String captureDate = textValue(requirmentElement, RequirementCaptureDate.FIELD_NAME);
        Boolean relevantToStakeholderRequirement = boolValue(requirmentElement, RelevantToStakeholderRequirement.FIELD_NAME);
        Boolean active = boolValue(requirmentElement, Active.FIELD_NAME);

        String  requirementHighlevelCapability = textValue(requirmentElement, RequirementHighlevelCapability.FIELD_NAME);
        Integer requirementTypeId = intValue(requirmentElement, RequirementType.FIELD_NAME);
        Integer requirementFrequencyId = intValue(requirmentElement, RequirementFrequency.FIELD_NAME);
        String requirementPerformance = textValue(requirmentElement, RequirementPerformance.FIELD_NAME);
        Integer requirementVerificationStatusId = intValue(requirmentElement, RequirementVerificationStatus.FIELD_NAME);
        Integer requirementVerificationStatementId = intValue(requirmentElement, RequirementVerificationStatement.FIELD_NAME);
        Integer requirementTechnicalPriorityId = intValue(requirmentElement, RequirementTechnicalPriority.FIELD_NAME);

/* GFA
        String requirementCode;
        BasisSystemRequirementParentCodeSelector parentCodeSelector = new BasisSystemRequirementParentCodeSelector(webSession);
        if (entityId == null) {
            String parentCode = parentCodeSelector.fetchTextValueFromXml(requirmentElement, BasisSystemRequirementParentCodeSelector.FIELD_NAME);
            requirementCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentCode);
        } else {
            requirementCode = textValue(requirmentElement, SYSTEMREQCODE.getFieldName());
        }
        Integer codeLevel = parentCodeSelector.getCodeLevel(requirementCode);
*/

        String requirementCode;
        SystemRequirementParentCodeSelector parentCodeSelector = new SystemRequirementParentCodeSelector(webSession);

        if (entityId == null && parentEntityId != null) {
            requirementCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        } else {
            requirementCode = textValue(requirmentElement, SYSTEMREQCODE.getFieldName());
        }

        Integer codeLevel = parentCodeSelector.getCodeLevel(requirementCode);

        systemRequirementEntity.setEntityId(entityId);
        systemRequirementEntity.setVersion(version);
        systemRequirementEntity.setRequirementCode(requirementCode);
        systemRequirementEntity.setRequirementCodeLevel(codeLevel);
        systemRequirementEntity.setRequirementName(requirementName);
        systemRequirementEntity.setRequirementDescription(requirementDescription);
        systemRequirementEntity.setVerificationStatusId(verificationStatusId);
        systemRequirementEntity.setBusinessPriorityId(businessPriorityId);
        systemRequirementEntity.setStatusId(statusId);
        systemRequirementEntity.setOwnerId(ownerId);
        systemRequirementEntity.setRationalStatement(rationalStatement);
        systemRequirementEntity.setCaptureDate(captureDate);
        systemRequirementEntity.setRelevantToStakeholderRequirement(relevantToStakeholderRequirement);

        systemRequirementEntity.setRequirementHighlevelCapability(requirementHighlevelCapability);
        systemRequirementEntity.setRequirementTypeId(requirementTypeId);
        systemRequirementEntity.setRequirementFrequencyId(requirementFrequencyId);
        systemRequirementEntity.setRequirementPerformance(requirementPerformance);
        systemRequirementEntity.setRequirementVerificationStatusId(requirementVerificationStatusId);
        systemRequirementEntity.setRequirementVerificationStatementId(requirementVerificationStatementId);
        systemRequirementEntity.setRequirementTechnicalPriorityIdtId(requirementTechnicalPriorityId);


        systemRequirementEntity.setActive(active);

        systemRequirementEntity.addAllDataElements();
        return systemRequirementEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<BasisSystemRequirementExportRow> rows = fetchRequirementsForExport(webSession, includeInactive);

        byte[] content;
        String contentType;
        String fileName;

        GenericExporters genericExporters = new BasisSystemRequirementExporters();

/* GFA
        try {
            //EmailSender emailSender = new EmailSender();
            //emailSender.sendEmail();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
*/

        switch (format) {
            case "csv" -> {
                content = genericExporters.toCsv(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getCsvContentType();
                fileName = genericExporters.getCsvFileName();
            }
            case "pdf" -> {
                content = genericExporters.toPdf(rows);
                contentType = genericExporters.getPdfContentType();
                fileName = genericExporters.getPdfFileName();
            }
            case "xml" -> {
                content = genericExporters.toXml(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getXmlContentType();
                fileName = genericExporters.getXmlFileName();
            }
            case "xlsx" -> {
                content = genericExporters.toXlsx(rows);
                contentType = genericExporters.getXlsxContentType();
                fileName = genericExporters.getXlsxFileName();
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

    private List<BasisSystemRequirementExportRow> fetchRequirementsForExport(WebSession webSession, boolean includeInactive) {
        SystemRequirementProvider provider = new SystemRequirementProvider(webSession);
        return provider.getAllSystemRequirement(includeInactive)
                .stream()
                .map(SystemRequirementServlet::toExportRow)
                .toList();
    }

    private static BasisSystemRequirementExportRow toExportRow(SystemRequirementEntity entity) {
        return new BasisSystemRequirementExportRow(
                entity.getRequirementCode(),
                entity.getRequirementCodeLevel(),
                entity.getRequirementName(),
                entity.getRequirementDescription(),
                entity.getVerificationStatus(),
                entity.getRationalStatement(),
                entity.getCaptureDate(),
                entity.getStatus(),
                entity.getBusinessPriority(),
                entity.getOwner(),
                entity.getChangedByUser(),
                entity.getChangedDate(),
                entity.isActive()
        );
    }

}