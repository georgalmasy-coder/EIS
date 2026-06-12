package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

abstract public class GenericImporters {

    private static final Logger log = LoggerFactory.getLogger(GenericImporters.class);

    public static final String EXTENSION_XML = "xml";
    public static final String EXTENSION_CSV = "csv";
    public static final String EXTENSION_XLSX = "xlsx";

    private final HttpServletRequest request;
    private final WebSession webSession;

    abstract public EntityType getEntityType();
    abstract public Object loadFromCsv(InputStream inputStream) throws Exception;
    abstract public Object loadFromXml(InputStream inputStream) throws Exception;
    abstract public Object loadFromXlsx(InputStream inputStream) throws Exception;
    abstract public EntityProvider getProvider();

    public GenericImporters(WebSession webSession, HttpServletRequest request) {
        this.webSession = webSession;
        this.request = request;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public int importEntities() throws Exception {
        Part filePart = request.getPart("file");

        if (filePart == null || filePart.getSize() == 0) {
            throw new Exception("No import file was uploaded.");
        }

        String fileName = submittedFileName(filePart);
        String extension = extensionOf(fileName);

        log.info("Importing file: {}", filePart);

        Object rows;
        try (InputStream inputStream = filePart.getInputStream()) {

            rows = switch (extension) {
                case EXTENSION_XML -> loadFromXml(inputStream);
                case EXTENSION_CSV -> loadFromCsv(inputStream);
                case EXTENSION_XLSX -> loadFromXlsx(inputStream);
                default -> throw new IllegalArgumentException("Unsupported import format: " + extension);
            };
        }

        EntityProvider provider = getProvider();
        List<AbstractEntity> entities = provider.toEntities(webSession, rows);

        log.info("Converted file to import entities : {} {}", entities.size(), getEntityType().getDescription());

        for (AbstractEntity entity : entities) {
            entity.validateEntity(true);
        }

        log.info("Validated import entities : {} {}", entities.size(), getEntityType().getDescription());

        for (AbstractEntity entity : entities) {
//GFA !!!!!!!!!!!                provider.persist(entity);
        }

        log.info("Persisted import entities : {} {}", entities.size(), getEntityType().getDescription());

        return entities.size();
    }

    public String text(Element parent, String tagName) {
        var nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent();
    }

    public List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (quoted && ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                current.append('"');
                i++;
                continue;
            }

            if (ch == '"') {
                quoted = !quoted;
                continue;
            }

            if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(current.toString());
        return values;
    }

    public String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }

        return values.get(index) == null ? "" : values.get(index).trim();
    }

    public Integer intValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean boolValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "true", "1", "yes", "y", "ja" -> true;
            case "false", "0", "no", "n", "nej" -> false;
            default -> null;
        };
    }

    public String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int index = fileName.lastIndexOf(".");

        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(index + 1).toLowerCase();
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String submittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");

        if (contentDisposition == null || contentDisposition.isBlank()) {
            return "";
        }

        for (String section : contentDisposition.split(";")) {
            String trimmed = section.trim();

            if (trimmed.startsWith("filename=")) {
                return trimmed.substring("filename=".length())
                        .trim()
                        .replace("\"", "");
            }
        }

        return "";
    }

}
