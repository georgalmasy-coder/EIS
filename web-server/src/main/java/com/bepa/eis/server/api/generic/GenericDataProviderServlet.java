package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.dataprovider.entities.common.AttachmentRecord;
import com.bepa.eis.server.dataprovider.entities.common.LinkRecord;
import com.bepa.eis.server.dataprovider.entities.common.NoteRecord;
import com.bepa.eis.server.dataprovider.fields.binary.FileData;
import com.bepa.eis.server.dataprovider.fields.integers.FileSize;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityAttachmentId;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.server.dataprovider.fields.strings.ContentType;
import com.bepa.eis.server.dataprovider.fields.strings.FileDescription;
import com.bepa.eis.server.dataprovider.fields.strings.FileName;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

abstract public class GenericDataProviderServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GenericDataProviderServlet.class);

    private WebSession webSession;

    abstract public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception;
    abstract public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) throws Exception;

    abstract public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable;
    abstract public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable;
    abstract public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable;
    abstract public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable;
    abstract public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        String module = request.getServletPath() +  "." + getCommandParameter(request);
        long startTime = System.currentTimeMillis();

        WebSession webSession = getWebSessionFromRequest(request);
        setWebSession(webSession);

        try {
            processGetRequest(webSession, request, response);
            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            logIncidentError(module, throwable);
        }
    }

    public void processGetRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String command = getCommandParameter(request);
        String module = request.getServletPath() +  "." + getCommandParameter(request);

        try {
            switch (command) {
                case "list" -> {
                    GenericXmlDocument xmlDocument = handleListOfEntities(webSession, request, response);
                    setOkResponse(response, xmlDocument);
                }
                case "edit" -> {
                    GenericXmlDocument xmlDocument;
                    Integer entityId = getEntityIdParameter(request);
                    Integer version =   getVersionParameter(request);
                    if (entityId != null) {
                        xmlDocument = handleEditEntity(webSession, request, response, entityId, version);
                    } else {
                        xmlDocument = handleCreateEntity(webSession, request, response, entityId);
                    }
                    setOkResponse(response, xmlDocument);
                }
                case "create" -> {
                    Integer parentEntityId = getEntityIdParameter(request);
                    GenericXmlDocument xmlDocument= handleCreateEntity(webSession, request, response, parentEntityId);
                    setOkResponse(response, xmlDocument);
                }
                case "export" -> handleExport(webSession, request, response);
                case "overview" -> {
                    GenericXmlDocument xmlDocument = handleOverview(webSession, request, response);
                    setOkResponse(response, xmlDocument);
                }
                default -> {
                    log.warn("Invalid command : {}", command);
                    throw new IllegalArgumentException("Invalid request : " + command);
                }

            }

        } catch (Throwable throwable) {
            logIncidentError(module, throwable);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        WebSession webSession = getWebSessionFromRequest(request);
        setWebSession(webSession);

        String module = request.getServletPath() +  "." + getCommandParameter(request);
        long startTime = System.currentTimeMillis();

        try {
            String command = getCommandParameter(request);

            switch (command) {
                case "import" -> {
                    log.debug("Importing data");
                    handleImport(webSession, request, response);
                }
                case "save" -> {
                    log.debug("Save data");
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(false);
                    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

                    Document doc = dbf.newDocumentBuilder().parse(request.getInputStream());
                    Element rootElement = doc.getDocumentElement();
                    handleSave(webSession, request, rootElement);
                    log.debug("payload : {} ", toXmlString(doc));
                }
                default -> {
                    log.warn("Invalid command : {}", command);
                    throw new IllegalArgumentException("Invalid request : " + command);
                }
            }

            setOkResponse(response);

            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            setErrorResponse(response, throwable);
            logIncidentError(module,
                    throwable);
        }
    }

    public void logIncidentError(String module, Throwable throwable) throws ServletException {
        if (throwable instanceof IllegalArgumentException) {
            // Ignore IllegalArgumentExceptions, they are not errors.
            // They are just used to indicate that the request is invalid.
            // We don't want to log them as errors.
        } else {

            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            throw new ServletException("Error processing request", throwable);
        }
    }

    public String getCommandParameter(HttpServletRequest request) {
        String command = request.getParameter("cmd");
        return command != null ? command.trim().toLowerCase() : "";
    }

    protected String buildDownloadFileName(WebSession webSession, String entityLabel, String extension) throws Exception {
        String projectName = "";

        if (webSession != null) {
            projectName = new TopPanelProvider(webSession).getProjectName();
        }

        String baseName = sanitizeFileName(entityLabel + " - " + projectName);

        if (baseName.isBlank()) {
            baseName = sanitizeFileName(entityLabel);
        }

        return baseName + "." + extension.toLowerCase();
    }

    private String sanitizeFileName(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|]+", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        sanitized = sanitized.replaceAll("^[.\\s]+|[.\\s]+$", "");
        return sanitized;
    }

    private Integer getEntityIdParameter(HttpServletRequest request) {
        String id = request.getParameter("id");
        Integer entityId = null;
        if (id != null && !id.isEmpty()) {
            try {
                entityId = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                log.error("Invalid get entity ID format: {}", id);
                throw new IllegalArgumentException("Invalid requirement ID format");
            }
        }
        return entityId;
    }

    private Integer getVersionParameter(HttpServletRequest request) {
        String version = request.getParameter("version");
        Integer ver = null;
        if (version != null && !version.isEmpty()) {
            try {
                ver = Integer.parseInt(version);
            } catch (NumberFormatException e) {
                log.error("Invalid get version format: {}", version);
                throw new IllegalArgumentException("Invalid version ID format");
            }
        }
        return ver;
    }

    private void setOkResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setOkResponse(HttpServletResponse response, GenericXmlDocument xmlDocument) throws IOException, ParserConfigurationException, TransformerException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(xmlDocument.toXmlString());
    }

    private void setErrorResponse(HttpServletResponse response, Throwable throwable) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        try {
            response.getWriter().write("Error occurred : " + throwable.getMessage());
        } catch (IOException e) {
            log.error("Unable to response client : {}", throwable.getMessage(), throwable);
        }

    }

    private WebSession getWebSession(String sessionId) {
        WebSession ws;
        if (GlobalConfiguration.isUdvMode()) {
            ws = new WebSession();
            ws.setId(1);
            ws.setSessionId("georg.almasy@mail.com");
            ws.setCustomerId(GlobalConfiguration.getDefaultCustomerId());
            ws.setProjectId(GlobalConfiguration.getDefaultProjectId());
            ws.setUserId(1);
            return ws;
        } else {
            try {
                SessionProvider sessionProvider = new SessionProvider(null);
                ws = sessionProvider.getBySessionId(sessionId);
            } catch (SQLException e) {

                log.error("Error getting session for page viewer: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return ws;
    }

    public String getSessionIdFromRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return (String) session.getAttribute("sessionID");
    }

    /**
     * Convenience method that serializes into an XML string.
     *
     * @param prettyPrint if true, indents the output (human-readable)
     */
    public String toXmlString(Document doc, boolean prettyPrint) throws TransformerException {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Common Xalan property (works in the usual JDK transformer)
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        String xmlAsString = writer.toString();
        log.info("Created XML : {}", xmlAsString);

        return xmlAsString;
    }

    /**
     * Defaults to pretty-printed XML.
     */
    public String toXmlString(Document doc) throws ParserConfigurationException, TransformerException {
        return toXmlString(doc, true);
    }

    public Element firstChild(Element parent, String tagName) {
        if (parent == null) return null;
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            if (parent.getChildNodes().item(i) instanceof Element el && tagName.equals(el.getTagName())) {
                return el;
            }
        }
        return null;
    }

    public List<Element> children(Element parent, String tagName) {
        List<Element> result = new java.util.ArrayList<>();
        if (parent != null) {
            for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
                if (parent.getChildNodes().item(i) instanceof Element el && tagName.equals(el.getTagName())) {
                    result.add(el);
                }
            }
        }
        return result;
    }

    public String textValue(Element parent, String tagName) {
        Element el = firstChild(parent, tagName);
        String text;
        if (el == null || el.getTextContent() == null) {
            text =  "";
        } else {
            text = el.getTextContent().trim();
        }
        return text;
    }

    public Integer intValue(Element parent, String tagName) {
        Integer result = null;
        String text = getIntAsValue(parent, tagName);
        if (text == null || text.isBlank()) {
            text = textValue(parent, tagName);
        }

        if (! text.isBlank()) {
            try {
                result = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return result;
    }

    private String getIntAsValue(Element parent, String tagName) {
        Element field = (Element) parent.getElementsByTagName(tagName).item(0);
        if (field == null) {
            return null;
        }

        Element valueElement = (Element) field.getElementsByTagName("Value").item(0);
        if (valueElement == null) {
            return null;
        }

        return valueElement.getTextContent().trim();
    }

    public Boolean boolValue(Element parent, String tagName) {
        Boolean result = null;
        String text = textValue(parent, tagName);
        if (!text.isBlank()) {
            result = Boolean.parseBoolean(text);
        }
        return result;
    }

    public void parseNoteDocument(AbstractEntity entity, Element noteSection) {
        // --- Notes ---
        if (noteSection != null) {
            for (Element entry : children(noteSection, "EntityNote")) {
                Integer entityNotePK = intValue(entry, "EntityNotePK");
                String noteText = textValue(entry, "NoteText");

                Element createdByElement = (Element) entry.getElementsByTagName("CreatedById").item(0);
                Integer createdById = null;

                if (createdByElement != null) {
                    Element valueElement = (Element) createdByElement.getElementsByTagName("Value").item(0);
                    if (valueElement != null) {
                        String createdByText = valueElement.getTextContent();
                        if (createdByText != null && !createdByText.isBlank()) {
                            try {
                                createdById = Integer.parseInt(createdByText.trim());
                            } catch (NumberFormatException ignored) {
                                // keep null
                            }
                        }
                    }
                }

                String createdTime = textValue(entry, "CreatedTime");
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(createdTime);
                } catch (Exception e) {
                    dateTime = LocalDateTime.now();
                }

                log.debug("entityNotePK {} createdById {} createdAt {} noteText {} ", entityNotePK, createdById, createdTime, noteText);

                NoteRecord noteRecord = new NoteRecord(entity, noteText, createdById, dateTime);
                entity.addNoteRecord(noteRecord);
            }
        }
    }

    public void parseLinkDocument(AbstractEntity entity, Element linkSection) {
        // --- Notes ---
        if (linkSection != null) {
            for (Element entry : children(linkSection, "EntityLink")) {
                Integer entityLinkPK = intValue(entry, "EntityLinkPK");
                String description = textValue(entry, "Description");
                String url = textValue(entry, "LinkUrl");

                Element createdByElement = (Element) entry.getElementsByTagName("CreatedById").item(0);
                Integer createdById = null;

                if (createdByElement != null) {
                    Element valueElement = (Element) createdByElement.getElementsByTagName("Value").item(0);
                    if (valueElement != null) {
                        String createdByText = valueElement.getTextContent();
                        if (createdByText != null && !createdByText.isBlank()) {
                            try {
                                createdById = Integer.parseInt(createdByText.trim());
                            } catch (NumberFormatException ignored) {
                                // keep null
                            }
                        }
                    }
                }

                String createdTime = textValue(entry, "CreatedTime");
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(createdTime);
                } catch (Exception e) {
                    dateTime = LocalDateTime.now();
                }

                log.debug("entityLinkPK {} createdById {} createdAt {} url {} description {}", entityLinkPK, createdById, createdTime, url, description);

                LinkRecord linkRecord = new LinkRecord(entity, description, url, createdById, dateTime);
                entity.addLinkRecord(linkRecord);
            }
        }
    }

    public void parseAttachmentDocument(AbstractEntity entity, Element attachmentSection) {
        // --- Attachment ---
        if (attachmentSection != null) {
            for (Element entry : children(attachmentSection, "EntityAttachment")) {
                Integer entityAttachmentBlobPK = intValue(entry, EntityAttachmentId.FIELD_NAME);
                String fileName = textValue(entry, FileName.FIELD_NAME);
                String contentType = textValue(entry, ContentType.FIELD_NAME);
                Integer fileSize = intValue(entry, FileSize.FIELD_NAME);
                String fileDescription = textValue(entry, FileDescription.FIELD_NAME);
                String fileData  = textValue(entry, FileData.FIELD_NAME);
                Boolean deleted = boolValue(entry, "IsDeleted"); //"Active");


                Element createdByElement = (Element) entry.getElementsByTagName("CreatedById").item(0);
                Integer createdById = null;

                if (createdByElement != null) {
                    Element valueElement = (Element) createdByElement.getElementsByTagName("Value").item(0);
                    if (valueElement != null) {
                        String createdByText = valueElement.getTextContent();
                        if (createdByText != null && !createdByText.isBlank()) {
                            try {
                                createdById = Integer.parseInt(createdByText.trim());
                            } catch (NumberFormatException ignored) {
                                // keep null
                            }
                        }
                    }
                }

                String createdTime = textValue(entry, "CreatedTime");
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(createdTime);
                } catch (Exception e) {
                    dateTime = LocalDateTime.now();
                }

                log.debug("entityAttachmentPK {} createdById {} createdAt {} fileName {} ",
                        entityAttachmentBlobPK, createdById, createdTime, fileName);

                AttachmentRecord attachmentRecord = new AttachmentRecord(entity,
                        entityAttachmentBlobPK,
                        fileName,
                        contentType,
                        fileSize,
                        fileData,
                        fileDescription,
                        createdById,
                        dateTime,
                        deleted);

                entity.addAttachmentRecord(attachmentRecord);

            }
        }
    }

    public void parseRelationDocument(AbstractEntity entity, Element relationSection) {
        // --- Relations ---
        if (relationSection != null) {
            for (Element entry : children(relationSection, "EntityRelation")) {
                Integer entityRelationPK = intValue(entry, "EntityRelationPK");
                Integer entityId = intValue(entry, "EntityId");
                Integer entityTypeId = intValue(entry, "EntityType");
                Integer relatedEntityId = intValue(entry, "RelatedEntityId");
                Integer relatedEntityTypeId = intValue(entry, "RelatedEntityType");

                EntityRelationRecord relationRecord = new EntityRelationRecord(
                        entity.getCustomerId().getValue(),
                        entity.getProjectId().getValue()
                );

                relationRecord.setEntityRelationPK(entityRelationPK);
                relationRecord.setEntityId(entityId);
                relationRecord.setEntityType(entityTypeId == null ? null : EntityType.fromId(entityTypeId));
                relationRecord.setRelatedEntityId(relatedEntityId);
                relationRecord.setRelatedEntityType(relatedEntityTypeId == null ? null : EntityType.fromId(relatedEntityTypeId));

                String createdTime = textValue(entry, "CreatedTime");
                if (createdTime != null && !createdTime.isBlank()) {
                    try {
                        relationRecord.setCreatedDate(LocalDateTime.parse(createdTime));
                    } catch (Exception ignored) {
                        relationRecord.setCreatedDate(LocalDateTime.now());
                    }
                }

                Integer createdById = intValue(entry, "CreatedById");
                if (createdById != null) {
                    relationRecord.setCreatedByUserId(createdById);
                }

                String relationTypeName = textValue(entry, "RelationTypeName");
                Boolean deleted = boolValue(entry, "IsDeleted");

                if (deleted != null && deleted) {
                    relationRecord.setRelationType(RelationType.DELETED);
                } else if (relationTypeName != null) {

                    relationRecord.setRelationType(RelationType.valueOfDescription(relationTypeName));
/* GFA
                    if ("Confirmed".equalsIgnoreCase(relationTypeName.trim())) {
                        relationRecord.setRelationType(RelationType.CONFIRMED);
                    } else if ("Not Relevant".equalsIgnoreCase(relationTypeName.trim())) {
                        relationRecord.setRelationType(RelationType.NOT_RELEVANT);
                    }

 */
                }

                entity.addEntityRelationRecord(relationRecord);
            }
        }

    }


    public String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public WebSession getWebSessionFromRequest(HttpServletRequest request) {
        WebSession webSession;
        try {
            String sessionId = getSessionIdFromRequest(request);
            webSession = getWebSession(sessionId);
        } catch (Exception e) {
            log.error("Error getting session for page viewer: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return webSession;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void setWebSession(WebSession webSession) {
        this.webSession = webSession;
    }

    public Integer toInteger(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
