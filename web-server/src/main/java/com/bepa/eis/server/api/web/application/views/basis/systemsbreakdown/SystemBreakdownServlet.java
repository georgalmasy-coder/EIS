package com.bepa.eis.server.api.web.application.views.basis.systemsbreakdown;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ParentEntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemsBreakdownParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.fields.strings.SBSCode;
import com.bepa.eis.server.dataprovider.fields.strings.SystemName;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineFinalized;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineNextTRL;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
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

@WebServlet(name = "SystemBreakdownServlet", urlPatterns = { "/project/systembreakdown" })
@MultipartConfig
public class SystemBreakdownServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(SystemBreakdownServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SystemBreakdownImporters importer = new SystemBreakdownImporters(webSession, request);
        if ("preview".equalsIgnoreCase(request.getParameter("phase"))) {
            importer.previewImport(response);
            return;
        }

        if (importer.importEntities() <= 0) {
            throw new RuntimeException("No entities imported");
        }
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) throws Exception {
        try {
            Element systemBreakdownDocument = firstChild(rootElement, "systemBreakdownDocument");

            if (systemBreakdownDocument != null) {
                Element systemBreakdown = firstChild(systemBreakdownDocument, "systembreakdown");
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element linkSection = firstChild(rootElement, "EntityLinks");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");

                SystemBreakdownEntity systemBreakdownEntity = parseSystemBreakdownDocument(webSession, systemBreakdown);
                SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(webSession);

                parseNoteDocument(systemBreakdownEntity, noteSection);
                parseLinkDocument(systemBreakdownEntity, linkSection);
                parseAttachmentDocument(systemBreakdownEntity, attachmentSection);

                systemBreakdownProvider.persist(systemBreakdownEntity);

                String eventDescription =
                        systemBreakdownEntity.getEntityType().getDescription() + " '" +
                        systemBreakdownEntity.getSbsCode() + " " +
                        systemBreakdownEntity.getSystemName() + "' has been " + (systemBreakdownEntity.getVersion().getValue() == 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(systemBreakdownEntity.getEntityType(),
                                                      systemBreakdownEntity.getEntityId().getValue(),
                                                      eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse system breakdown XML", e);
        }

    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return listOfSystems(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return editSystemById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return createNewSystem(webSession, parentEntityId);
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid overview request");
    }

    private GenericXmlDocument listOfSystems(WebSession webSession) {
        try {
            return new SystemBreakdownList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of systems breakdown info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewSystem(WebSession webSession, Integer parentEntityId) {
        try {
            return new SystemBreakdownInfo(webSession, CREATE_ENTITY, parentEntityId);
        } catch (Exception e) {
            log.error("Error getting system breakdown info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editSystemById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new SystemBreakdownInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting system breakdown info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private SystemBreakdownEntity parseSystemBreakdownDocument(WebSession webSession, Element systemBreakdown) {
        SystemBreakdownEntity systemBreakdownEntity = new SystemBreakdownEntity(webSession);
        systemBreakdownEntity.setCustomerId(webSession.getCustomerId());
        systemBreakdownEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(systemBreakdown, EntityId.FIELD_NAME);
        Integer parentEntityId = intValue(systemBreakdown, ParentEntityId.FIELD_NAME);
        Integer version = intValue(systemBreakdown, Version.FIELD_NAME);
        String systemName = textValue(systemBreakdown, SystemName.FIELD_NAME);
        Integer systemOwnerId = intValue(systemBreakdown, SystemOwner.FIELD_NAME);
        Integer departmentId = intValue(systemBreakdown, SystemDepartment.FIELD_NAME);
        Integer trlId = intValue(systemBreakdown, TRL.FIELD_NAME);
        String deadlineNextTrl = textValue(systemBreakdown, DeadlineNextTRL.FIELD_NAME);
        String deadlineFinalized = textValue(systemBreakdown, DeadlineFinalized.FIELD_NAME);
        Boolean active = boolValue(systemBreakdown, Active.FIELD_NAME);

        String sbsCode = "";
//GFA        Integer sbsCodeLevel;

//GFA        SBSCodeParentSystem parentSystem = new SBSCodeParentSystem();
        SystemsBreakdownParentCodeSelector parentCodeSelector = new SystemsBreakdownParentCodeSelector(webSession);

        if (entityId == null && parentEntityId != null) {
            sbsCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        } else {
            sbsCode = textValue(systemBreakdown, SBSCode.FIELD_NAME);
        }
        Integer sbsCodeLevel = parentCodeSelector.getCodeLevel(sbsCode);

        /* GFA
        if (entityId == null) {
            SBSCodeParentSystem parentSystem = new SBSCodeParentSystem();
            String sbsCodeType = textValue(systemBreakdown, SBSCodeType.FIELD_NAME);
            String sbsCodeParentSystem = textValue(systemBreakdown, SBSCodeParentSystem.FIELD_NAME);

            sbsCode = parentSystem.getNextAvailableSBSCode(webSession, sbsCodeType, sbsCodeParentSystem);
            sbsCodeLevel = parentSystem.getCodeLevel(sbsCode);
        } else {
            sbsCode = textValue(systemBreakdown, SBSCode.FIELD_NAME);
            sbsCodeLevel = intValue(systemBreakdown, CodeLevel.FIELD_NAME);
        }

         */

        systemBreakdownEntity.setEntityId(entityId);
        systemBreakdownEntity.setVersion(version);
        systemBreakdownEntity.setSbsCode(sbsCode);
        systemBreakdownEntity.setSbsCodeLevel(sbsCodeLevel);
        systemBreakdownEntity.setSystemName(systemName);
        systemBreakdownEntity.setSystemOwner(systemOwnerId);
        systemBreakdownEntity.setDepartment(departmentId);
        systemBreakdownEntity.setTrlId(trlId);
        systemBreakdownEntity.setDeadlineNextTrl(deadlineNextTrl);
        systemBreakdownEntity.setDeadlineFinalized(deadlineFinalized);
        systemBreakdownEntity.setActive(active);

        systemBreakdownEntity.addAllDataElements();

        return systemBreakdownEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<SystemBreakdownExportRow> rows = fetchSystemsForExport(webSession, includeInactive);

        GenericExporters genericExporters = new SystemBreakdownExporters();

        byte[] content;
        String contentType;
        String fileName;

        switch (format) {
            case "csv" -> {
                content = genericExporters.toCsv(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getCsvContentType();
                fileName = buildDownloadFileName(webSession, "Systems Breakdown", "csv");
            }
            case "pdf" -> {
                content = genericExporters.toPdf(rows);
                contentType = genericExporters.getPdfContentType();
                fileName = buildDownloadFileName(webSession, "Systems Breakdown", "pdf");
            }
            case "xml" -> {
                content = genericExporters.toXml(rows).getBytes(StandardCharsets.UTF_8);
                contentType = genericExporters.getXmlContentType();
                fileName = buildDownloadFileName(webSession, "Systems Breakdown", "xml");
            }
            case "xlsx" -> {
                content = genericExporters.toXlsx(rows);
                contentType = genericExporters.getXlsxContentType();
                fileName = buildDownloadFileName(webSession, "Systems Breakdown", "xlsx");
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

    private List<SystemBreakdownExportRow> fetchSystemsForExport(WebSession webSession, boolean includeInactive) {
        SystemBreakdownProvider provider = new SystemBreakdownProvider(webSession);

        return provider.findAllForExport(includeInactive)
                .stream()
                .map(SystemBreakdownServlet::toExportRow)
                .toList();
    }

    private static SystemBreakdownExportRow toExportRow(SystemBreakdownEntity entity) {
        return new SystemBreakdownExportRow(
                entity.getCode(),
                entity.getSbsCodeLevel().getValue(),
                entity.getName(),
                entity.getSystemOwner(),
                entity.getSystemDepartment(),
                entity.getTrl(),
                entity.getDeadlineNextTrl(),
                entity.getDeadlineFinalized(),
                entity.getChangedByUser(),
                entity.getChangedDate().getValue(),
                entity.isActive()
        );
    }

}
