package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.dataprovider.entities.common.AttachmentRecord;
import com.bepa.eis.server.dataprovider.entities.common.NoteRecord;
import com.bepa.eis.server.dataprovider.fields.binary.FileData;
import com.bepa.eis.server.dataprovider.fields.integers.FileSize;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityAttachmentId;
import com.bepa.eis.server.dataprovider.fields.strings.ContentType;
import com.bepa.eis.server.dataprovider.fields.strings.FileDescription;
import com.bepa.eis.server.dataprovider.fields.strings.FileName;
import com.bepa.eis.common.providers.SessionProvider;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

abstract public class GenericDataProviderServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GenericDataProviderServlet.class);

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

        try {
            processGetRequest(webSession, request, response);
            PerformanceProvider performanceProvider = new PerformanceProvider(webSession);
            performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            log.error("Error processing request: {}", throwable.getMessage(), throwable);
            throw new ServletException("Error processing request", throwable);
        }
    }

    public void processGetRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String command = getCommandParameter(request);
        String module = request.getServletPath() +  "." + getCommandParameter(request);
        long startTime = System.currentTimeMillis();

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
                case "export" -> {
                    handleExport(webSession, request, response);
                }
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
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);

            log.error("Error processing request: {}", throwable.getMessage(), throwable);
            throw new ServletException("Error processing request", throwable);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        WebSession webSession = getWebSessionFromRequest(request);
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
                    log.debug("payload : " + toXmlString(doc));
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
            log.error("Error processing request: {}", throwable.getMessage(), throwable);
            setErrorResponse(response, throwable);
            throw new ServletException("Error processing request", throwable);
        }
    }

    private void handleSave(HttpServletRequest request ) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        Document doc = dbf.newDocumentBuilder().parse(request.getInputStream());

        log.debug("payload : " + toXmlString(doc));
    }

    private String getCommandParameter(HttpServletRequest request) {
        String command = request.getParameter("cmd");
        return command != null ? command.trim().toLowerCase() : "";
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

    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
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

        String message = "<error><message>#MSG#</message></error>";
        message = message.replace("#MSG#", throwable.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        try {
//            response.getWriter().write(message);
            response.getWriter().write("Error occurred : " + throwable.getMessage() + "");
        } catch (IOException e) {
            log.error("Unable to respone client : {}", throwable.getMessage(), throwable);
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

    public void setXmlResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=UTF-8");

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    /**
     * Convenience method that serializes into an XML string.
     *
     * @param prettyPrint if true, indents the output (human-readable)
     */
    public String toXmlString(Document doc, boolean prettyPrint) throws ParserConfigurationException, TransformerException {

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
        String text = null;
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
            } catch (NumberFormatException e) { }
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

    public void parseAttachmentDocument(AbstractEntity entity, Element attachmentSection) {
        // --- Attachment ---
        if (attachmentSection != null) {
            for (Element entry : children(attachmentSection, "EntityAttachment")) {
                Integer entityAttachmentPK = intValue(entry, EntityAttachmentId.FIELD_NAME);
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
                        entityAttachmentPK, createdById, createdTime, fileName);

                AttachmentRecord attachmentRecord = new AttachmentRecord(entity,
                        entityAttachmentPK,
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

    public String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public WebSession getWebSessionFromRequest(HttpServletRequest request) {
        WebSession webSession = null;
        try {
            String sessionId = getSessionIdFromRequest(request);
            webSession = getWebSession(sessionId);
        } catch (Exception e) {
            log.error("Error getting session for page viewer: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return webSession;
    }

}
