package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.EventProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericExporters;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ParentEntityId;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.FunctionalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
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

@WebServlet(name = "FunctionalStructureServlet", urlPatterns = {
        "/pro/functionalstructure"
})
@MultipartConfig
public class FunctionalStructureServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(FunctionalStructureServlet.class);

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        FunctionalStructureImporters importer = new FunctionalStructureImporters(webSession, request);
        if (importer.importEntities() <= 0) {
            throw new RuntimeException("No entities imported");
        }
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        try {
            Element functionalDocument = firstChild(rootElement, "functionStructureDocument");
            if (functionalDocument == null) {
                functionalDocument = firstChild(rootElement, "functionStructure");
            }

            if (functionalDocument != null) {
                Element functionalElement = firstChild(functionalDocument, "functionalDocument");
                if (functionalElement == null) {
                    functionalElement = firstChild(functionalDocument, "functionStructure");
                }
                Element noteSection = firstChild(rootElement, "EntityNotes");
                Element linkSection = firstChild(rootElement, "EntityLinks");
                Element attachmentSection = firstChild(rootElement, "EntityAttachments");
                Element relationSection = firstChild(rootElement, "EntityRelations");

                FunctionalStructureEntity functionalEntity = parseFunctionalStructureDocument(webSession, functionalElement);
                FunctionalStructureProvider functionalProvider = new FunctionalStructureProvider(webSession);

                parseNoteDocument(functionalEntity, noteSection);
                parseLinkDocument(functionalEntity, linkSection);
                parseAttachmentDocument(functionalEntity, attachmentSection);
                parseRelationDocument(functionalEntity, relationSection);

                functionalProvider.persist(functionalEntity);

                String eventDescription =
                        functionalEntity.getEntityType().getDescription() + " '" +
                        functionalEntity.getFunctionalCode() + " " +
                        functionalEntity.getFunctionalDescription()  + "'" + " has been " + (functionalEntity.getVersion().getValue() == 1 ? "created" : "updated");

                EventProvider eventProvider = new EventProvider(webSession);
                eventProvider.createEntityChangeEvent(functionalEntity.getEntityType(),
                        functionalEntity.getEntityId().getValue(),
                        eventDescription);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse functional structure XML", e);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return listOfFunctionalStructures(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        return editFunctionalStructureById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        return createNewFunctionStructure(webSession, parentEntityId);
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid overview request");
    }

    private GenericXmlDocument listOfFunctionalStructures(WebSession webSession) {
        try {
            return new FunctionalStructureList(webSession);
        } catch (Exception e) {
            log.error("Error getting list of functional structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument createNewFunctionStructure(WebSession webSession, Integer parentEntityId) {
        try {
            return new FunctionalStructureInfo(webSession, CREATE_ENTITY, parentEntityId);
        } catch (Exception e) {
            log.error("Error getting functional structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private GenericXmlDocument editFunctionalStructureById(WebSession webSession, Integer entityId, Integer version) {
        try {
            return new FunctionalStructureInfo(webSession, EDIT_ENTITY, entityId, version);
        } catch (Exception e) {
            log.error("Error getting functional structure info: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private FunctionalStructureEntity parseFunctionalStructureDocument(WebSession webSession, Element functionalElement) {
        FunctionalStructureEntity functionalEntity = new FunctionalStructureEntity(webSession);
        functionalEntity.setCustomerId(webSession.getCustomerId());
        functionalEntity.setProjectId(webSession.getProjectId());

        Integer entityId = intValue(functionalElement, EntityId.FIELD_NAME);
        Integer parentEntityId = intValue(functionalElement, ParentEntityId.FIELD_NAME);
        Integer version = intValue(functionalElement, Version.FIELD_NAME);
        String functionalName = textValue(functionalElement, FunctionalName.FIELD_NAME);
        String functionalDescription = textValue(functionalElement, FunctionalDescription.FIELD_NAME);
        Boolean active = boolValue(functionalElement, Active.FIELD_NAME);

        String functionalCode;
        FunctionalStructureParentCodeSelector parentCodeSelector = new FunctionalStructureParentCodeSelector(webSession);

        if (entityId == null && parentEntityId != null) {
            functionalCode = parentCodeSelector.getNextAvailableCodeValue(webSession, parentEntityId);
        } else {
            functionalCode = textValue(functionalElement, FunctionalCode.FIELD_NAME);
        }

        Integer codeLevel = parentCodeSelector.getCodeLevel(functionalCode);

        functionalEntity.setEntityId(entityId);
        functionalEntity.setVersion(version);
        functionalEntity.setFunctionalCode(functionalCode);
        functionalEntity.setFunctionalCodeLevel(codeLevel);
        functionalEntity.setFunctionalName(functionalName);
        functionalEntity.setFunctionalDescription(functionalDescription);
        functionalEntity.setActive(active);

        functionalEntity.addAllDataElements();

        return functionalEntity;
    }

    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String format = valueOrDefault(request.getParameter("format"), "xlsx").toLowerCase();
        boolean includeInactive = Boolean.parseBoolean(valueOrDefault(request.getParameter("includeInaktive"), "false"));

        List<FunctionalStructureExportRow> rows = fetchFunctionalStructuresForExport(webSession, includeInactive);
        GenericExporters genericExporters = new FunctionalStructureExporters();

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

    private List<FunctionalStructureExportRow> fetchFunctionalStructuresForExport(WebSession webSession, boolean includeInactive) {
        FunctionalStructureProvider provider = new FunctionalStructureProvider(webSession);

        return provider.getAllFunctionalStructure(includeInactive)
                .stream()
                .map(FunctionalStructureServlet::toExportRow)
                .toList();
    }

    private static FunctionalStructureExportRow toExportRow(FunctionalStructureEntity entity) {
        return new FunctionalStructureExportRow(
                entity.getFunctionalCode().getValue(),
                entity.geFunctionalCodeLevel().getValue(),
                entity.getFunctionalName().getValue(),
                entity.getFunctionalDescription().getValue(),
                entity.getChangedByUser(),
                entity.getChangedDate().getValue(),
                entity.isActive()
        );
    }

}

