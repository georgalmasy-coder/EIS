package com.bepa.eis.server.api.web.application.views.basis.stakeholder;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.cache.LookupCache;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.strings.email.ContactEmail;
import com.bepa.eis.server.dataprovider.fields.strings.phone.ContactPhone;
import com.bepa.eis.server.entites.stakeholder.StakeholderEntity;
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

@WebServlet(name = "BasisStakeholderServlet", urlPatterns = {"/basis/stakeholder"})
@MultipartConfig
public class StakeholderServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(StakeholderServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StakeholderImporters importer = new StakeholderImporters(webSession, request);
        if (importer.importEntities() <= 0) {
            throw new RuntimeException("No entities imported");
        }
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        try {
            Element StakeholderDocument = firstChild(rootElement, "stakeholderDocument");

            if (StakeholderDocument != null) {
                Element stakeholder = firstChild(StakeholderDocument, "stakeholder");
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");

                StakeholderEntity stakeholderEntity = parseStakeholderDocument(webSession, stakeholder);
                StakeholderProvider stakeholderProvider = new StakeholderProvider(webSession);

                parseNoteDocument(stakeholderEntity, noteSection);
                parseAttachmentDocument(stakeholderEntity, attachmentSection);

                stakeholderProvider.persist(stakeholderEntity);

                EhcacheProvider.clearCacheEntry(webSession.getCustomerId());

                String eventDescription =
                        stakeholderEntity.getEntityType().getDescription() + " '" +
                                stakeholderEntity.getName() + "'" + " has been " + (stakeholderEntity.getVersion().getValue()== 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(stakeholderEntity.getEntityType(),
                        stakeholderEntity.getEntityId().getValue(),
                        eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse stakeholder XML", e);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return listOfStakeholders(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return editStakeholderById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return createNewStakeholder(webSession);
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid overview request");
    }

    private GenericXmlDocument listOfStakeholders(WebSession webSession) {
        try {
            return new StakeholderList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of stakeholder info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewStakeholder(WebSession webSession) {
        try {
            return new StakeholderInfo(webSession, CREATE_ENTITY);
        } catch (Exception e) {
            log.error("Error getting basis stakeholder info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editStakeholderById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new StakeholderInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting stakeholder info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private StakeholderEntity parseStakeholderDocument(WebSession webSession, Element stakeholderElement) {

        StakeholderEntity stakeholderEntity = new StakeholderEntity(webSession);
        stakeholderEntity.setCustomerId(webSession.getCustomerId());
        stakeholderEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(stakeholderElement, EntityId.FIELD_NAME);
        Integer version = intValue(stakeholderElement, Version.FIELD_NAME);
        String stakeholderName = textValue(stakeholderElement, StakeholderName.FIELD_NAME);
        String stakeholderDescription = textValue(stakeholderElement, StakeholderDescription.FIELD_NAME);
        String contactName = textValue(stakeholderElement, ContactName.FIELD_NAME);
        String contactEmail = textValue(stakeholderElement, ContactEmail.FIELD_NAME);
        String contactPhone = textValue(stakeholderElement, ContactPhone.FIELD_NAME);
        Boolean active = boolValue(stakeholderElement, Active.FIELD_NAME);

        stakeholderEntity.setEntityId(entityId);
        stakeholderEntity.setVersion(version);
        stakeholderEntity.setStakeholderName(stakeholderName);
        stakeholderEntity.setStakeholderDescription(stakeholderDescription);
        stakeholderEntity.setStakeholderContactName(contactName);
        stakeholderEntity.setStakeholderContactEmail(contactEmail);
        stakeholderEntity.setStakeholderContactPhone(contactPhone);
        stakeholderEntity.setActive(active);

        stakeholderEntity.addAllDataElements();

        return stakeholderEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<StakeholderExportRow> rows = fetchStakeholdersForExport(webSession, includeInactive);
        GenericExporters genericExporters = new StakeholderExporters();

        byte[] content;
        String contentType;
        String fileName;

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

    private List<StakeholderExportRow> fetchStakeholdersForExport(WebSession webSession, boolean includeInactive) {
        StakeholderProvider provider = new StakeholderProvider(webSession);

        return provider.getAllStakeholder(includeInactive)
                .stream()
                .map(StakeholderServlet::toExportRow)
                .toList();
    }

    private static StakeholderExportRow toExportRow(StakeholderEntity entity) {
        return new StakeholderExportRow(
                entity.getStakeholderName(),
                entity.getStakeholderDescription(),
                entity.getStakeholderContactName(),
                entity.getStakeholderContactEmail(),
                entity.getStakeholderContactPhone(),
                entity.getChangedByUser(),
                entity.getChangedDate().getValue(),
                entity.isActive()
        );
    }

}