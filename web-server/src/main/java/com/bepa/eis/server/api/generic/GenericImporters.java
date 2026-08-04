package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.utilities.JsonUtil;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

abstract public class GenericImporters {

    private static final Logger log = LoggerFactory.getLogger(GenericImporters.class);
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d+(?:\\.\\d+){0,4}$");

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
    protected abstract List<String> getPreviewFieldNames();
    protected abstract List<String> getRequiredFieldNames();
    protected abstract List<String> getValidPrefixes();
    protected abstract String getImportEntitiesButtonText();

    public GenericImporters(WebSession webSession, HttpServletRequest request) {
        this.webSession = webSession;
        this.request = request;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void previewImport(HttpServletResponse response) throws Exception {
        ImportValidationResult validationResult = loadAndValidateImportRows();
        JsonUtil.writeJson(response, HttpServletResponse.SC_OK, validationResult.toJson());
    }

    public int importEntities() throws Exception {
        ImportValidationResult validationResult = loadAndValidateImportRows();

        if (!validationResult.allValid()) {
            throw new IllegalArgumentException("Import contains validation errors. Review the preview and fix the highlighted rows.");
        }

        EntityProvider provider = getProvider();
        List<AbstractEntity> entities = provider.toEntities(webSession, validationResult.importRows());

        log.info("Converted file to import entities : {} {}", entities.size(), getEntityType().getDescription());

        for (AbstractEntity entity : entities) {
            entity.validateEntity(true);
        }

        log.info("Validated import entities : {} {}", entities.size(), getEntityType().getDescription());

        for (AbstractEntity entity : entities) {
            provider.persist(entity);
        }

        log.info("Persisted import entities : {} {}", entities.size(), getEntityType().getDescription());

        return entities.size();
    }

    protected String getImportDialogTitle() {
        return "Import " + getEntityType().getDescription();
    }

    protected String getImportDialogDescription() {
        return "Select a file containing " + getEntityType().getDescription().toLowerCase()
                + ". The file must be XML, XLSX or CSV.";
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

    protected String getFieldValue(Object row, String fieldName) {
        if (row == null || fieldName == null || fieldName.isBlank()) {
            return "";
        }

        try {
            Method method = row.getClass().getMethod(fieldName);
            Object value = method.invoke(row);
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException e) {
            log.debug("Unable to read field '{}' from {}: {}", fieldName, row.getClass().getSimpleName(), e.getMessage());
            return "";
        }
    }

    private ImportValidationResult loadAndValidateImportRows() throws Exception {
        LoadedImport loadedImport = loadUploadedRows();
        List<Object> rows = sortRowsById(loadedImport.rows());
        List<ValidatedImportRow> validatedRows = validateRows(rows);
        return new ImportValidationResult(
                getImportDialogTitle(),
                getImportDialogDescription(),
                getImportEntitiesButtonText(),
                buildColumns(),
                rows,
                validatedRows
        );
    }

    private LoadedImport loadUploadedRows() throws Exception {
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

        if (!(rows instanceof List<?> rowList)) {
            throw new IllegalStateException("Import loader must return a List.");
        }

        return new LoadedImport(fileName, extension, new ArrayList<>(rowList));
    }

    private List<Object> sortRowsById(List<Object> rows) {
        List<Object> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(Comparator.comparing(this::buildSortKey));
        return sortedRows;
    }

    private SortKey buildSortKey(Object row) {
        String rawId = getFieldValue(row, "id");
        ParsedId parsedId = parseId(rawId);

        if (parsedId == null) {
            return SortKey.invalid(rawId);
        }

        return SortKey.valid(parsedId.prefixRank(), parsedId.prefix(), parsedId.parts(), parsedId.internalCode(), rawId);
    }

    private ParsedId parseId(String rawId) {
        String normalizedId = rawId == null ? "" : rawId.trim();

        if (normalizedId.isBlank()) {
            return null;
        }

        List<String> prefixes = new ArrayList<>(getValidPrefixes() == null ? List.of() : getValidPrefixes());
        prefixes.removeIf(prefix -> prefix == null);
        prefixes.sort(Comparator.comparingInt(String::length).reversed());

        String prefix = "";
        for (String candidate : prefixes) {
            if (candidate.isEmpty()) {
                continue;
            }

            if (normalizedId.regionMatches(true, 0, candidate, 0, candidate.length())) {
                prefix = candidate;
                break;
            }
        }

        String coreId = normalizedId.substring(prefix.length()).trim();

        if (!ID_PATTERN.matcher(coreId).matches()) {
            return null;
        }

        String[] segments = coreId.split("\\.");
        if (segments.length == 0 || segments.length > 5) {
            return null;
        }

        List<Integer> parts = new ArrayList<>(segments.length);
        for (String segment : segments) {
            try {
                parts.add(Integer.parseInt(segment));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        int prefixRank = 0;
        for (int index = 0; index < prefixes.size(); index++) {
            String candidate = prefixes.get(index);
            if (!candidate.isEmpty() && normalizedId.regionMatches(true, 0, candidate, 0, candidate.length())) {
                prefixRank = index;
                break;
            }
        }

        return new ParsedId(prefix, parts, prefixRank, coreId, internalCode(parts));
    }

    private List<ValidatedImportRow> validateRows(List<Object> sortedRows) {
        List<ValidatedImportRow> validatedRows = new ArrayList<>();
        Map<String, Integer> lastChildByParent = new HashMap<>();
        Set<String> seenPaths = new java.util.HashSet<>();
        Set<String> seenCoreIds = new java.util.HashSet<>();

        for (Object row : sortedRows) {
            String rawId = getFieldValue(row, "id");
            ParsedId parsedId = parseId(rawId);
            List<String> errors = new ArrayList<>();

            if (parsedId == null) {
                errors.add("ID is invalid. Use digits and dots only, up to five levels.");
            } else {
                validateIdSequence(parsedId, seenPaths, lastChildByParent, errors);

                String coreId = parsedId.internalCode();
                if (!seenCoreIds.add(coreId)) {
                    errors.clear();
                    errors.add("Duplicate ID " + parsedId.displayPrefix() + coreId);
                }
            }

            for (String requiredField : getRequiredFieldNames()) {
                if ("id".equalsIgnoreCase(requiredField)) {
                    continue;
                }

                String value = getFieldValue(row, requiredField);
                if (isBlank(value)) {
                    errors.add(capitalizeFieldLabel(requiredField) + " is required.");
                }
            }

            List<String> previewValues = new ArrayList<>();
            for (String previewField : getPreviewFieldNames()) {
                previewValues.add(getFieldValue(row, previewField));
            }

            validatedRows.add(new ValidatedImportRow(
                    previewValues,
                    errors.isEmpty(),
                    String.join(" ", errors)
            ));

            if (parsedId != null) {
                seenPaths.add(parsedId.fullPathKey());
            }
        }

        return validatedRows;
    }

    private void validateIdSequence(
            ParsedId parsedId,
            Set<String> seenPaths,
            Map<String, Integer> lastChildByParent,
            List<String> errors
    ) {
        if (parsedId.parts().isEmpty()) {
            errors.add("ID is invalid.");
            return;
        }

        for (int depth = 1; depth < parsedId.parts().size(); depth++) {
            String parentPath = parsedId.pathKey(depth - 1);
            String parentKey = parentPath;

            if (!seenPaths.contains(parentKey)) {
                errors.add("Missing parent ID " + parentPath + ".");
            }
        }

        String parentPath = parsedId.parts().size() == 1
                ? ""
                : parsedId.pathKey(parsedId.parts().size() - 2);
        String parentKey = parentPath;
        Integer lastChild = lastChildByParent.get(parentKey);
        int childNumber = parsedId.parts().get(parsedId.parts().size() - 1);

        if (lastChild == null) {
            if (childNumber != 1) {
                errors.add("Missing ID " + parsedId.displayPrefix() + nextExpectedSiblingPath(parsedId, 1) + ".");
            }
        } else if (childNumber != lastChild + 1) {
            errors.add("Missing ID " + parsedId.displayPrefix() + nextExpectedSiblingPath(parsedId, lastChild + 1) + ".");
        }

        lastChildByParent.put(parentKey, childNumber);
    }

    private String internalCode(List<Integer> parts) {
        return joinParts(parts);
    }

    private String nextExpectedSiblingPath(ParsedId parsedId, int expectedChild) {
        if (parsedId.parts().isEmpty()) {
            return String.valueOf(expectedChild);
        }

        List<Integer> expectedParts = new ArrayList<>(parsedId.parts().subList(0, parsedId.parts().size() - 1));
        expectedParts.add(expectedChild);
        return joinParts(expectedParts);
    }

    private List<PreviewColumn> buildColumns() {
        List<PreviewColumn> columns = new ArrayList<>();
        for (String fieldName : getPreviewFieldNames()) {
            columns.add(new PreviewColumn(fieldName, capitalizeFieldLabel(fieldName)));
        }
        return columns;
    }

    private String joinParts(List<Integer> parts) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                builder.append('.');
            }
            builder.append(parts.get(index));
        }
        return builder.toString();
    }

    private String capitalizeFieldLabel(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return "";
        }

        return switch (fieldName.toLowerCase()) {
            case "id" -> "ID";
            case "level" -> "Level";
            case "name" -> "Name";
            case "description" -> "Description";
            default -> Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        };
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

    private record LoadedImport(String fileName, String extension, List<Object> rows) {
    }

    private record ParsedId(String prefix, List<Integer> parts, int prefixRank, String coreId, String internalCode) {
        private String displayPrefix() {
            return prefix == null || prefix.isBlank() ? "" : prefix;
        }

        private String fullPathKey() {
            return internalCode;
        }

        private String pathKey(int lastIndex) {
            if (lastIndex < 0 || lastIndex >= parts.size()) {
                return "";
            }

            StringBuilder builder = new StringBuilder();
            for (int index = 0; index <= lastIndex; index++) {
                if (index > 0) {
                    builder.append('.');
                }
                builder.append(parts.get(index));
            }
            return builder.toString();
        }
    }

    private record SortKey(boolean valid, int prefixRank, String prefix, List<Integer> parts, String internalCode, String rawId) implements Comparable<SortKey> {
        static SortKey valid(int prefixRank, String prefix, List<Integer> parts, String internalCode, String rawId) {
            return new SortKey(true, prefixRank, prefix == null ? "" : prefix, parts == null ? List.of() : List.copyOf(parts), internalCode == null ? "" : internalCode, rawId == null ? "" : rawId);
        }

        static SortKey invalid(String rawId) {
            return new SortKey(false, Integer.MAX_VALUE, "", List.of(), "", rawId == null ? "" : rawId);
        }

        @Override
        public int compareTo(SortKey other) {
            if (valid != other.valid) {
                return valid ? -1 : 1;
            }

            if (!valid) {
                return String.CASE_INSENSITIVE_ORDER.compare(rawId, other.rawId);
            }

            int prefixCompare = Integer.compare(prefixRank, other.prefixRank);
            if (prefixCompare != 0) {
                return prefixCompare;
            }

            int maxSize = Math.max(parts.size(), other.parts.size());
            for (int index = 0; index < maxSize; index++) {
                int left = index < parts.size() ? parts.get(index) : -1;
                int right = index < other.parts.size() ? other.parts.get(index) : -1;
                int partCompare = Integer.compare(left, right);
                if (partCompare != 0) {
                    return partCompare;
                }
            }

            return String.CASE_INSENSITIVE_ORDER.compare(rawId, other.rawId);
        }
    }

    private record PreviewColumn(String key, String label) {
    }

    private record ValidatedImportRow(List<String> values, boolean valid, String error) {
    }

    private record ImportValidationResult(
            String title,
            String description,
            String executeButtonText,
            List<PreviewColumn> columns,
            List<Object> importRows,
            List<ValidatedImportRow> rows
    ) {
        boolean allValid() {
            for (ValidatedImportRow row : rows) {
                if (!row.valid()) {
                    return false;
                }
            }

            return true;
        }

        String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{");
            JsonUtil.appendJsonString(json, "title", title);
            json.append(",");
            JsonUtil.appendJsonString(json, "description", description);
            json.append(",");
            JsonUtil.appendJsonString(json, "executeButtonText", executeButtonText);
            json.append(",");
            JsonUtil.appendJsonBoolean(json, "allValid", allValid());
            json.append(",");
            JsonUtil.appendJsonNumber(json, "rowCount", rows == null ? 0 : rows.size());
            json.append(",");
            JsonUtil.appendJsonNumber(json, "errorCount", errorCount());
            json.append(",");
            json.append("\"columns\":[");
            for (int index = 0; index < columns.size(); index++) {
                PreviewColumn column = columns.get(index);
                if (index > 0) {
                    json.append(",");
                }
                json.append("{");
                JsonUtil.appendJsonString(json, "key", column.key());
                json.append(",");
                JsonUtil.appendJsonString(json, "label", column.label());
                json.append("}");
            }
            json.append("],");
            json.append("\"rows\":[");
            for (int index = 0; index < rows.size(); index++) {
                ValidatedImportRow row = rows.get(index);
                if (index > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"values\":[");
                for (int valueIndex = 0; valueIndex < row.values().size(); valueIndex++) {
                    if (valueIndex > 0) {
                        json.append(",");
                    }
                    json.append("\"").append(JsonUtil.escapeJson(row.values().get(valueIndex))).append("\"");
                }
                json.append("],");
                JsonUtil.appendJsonBoolean(json, "valid", row.valid());
                json.append(",");
                JsonUtil.appendJsonString(json, "error", row.error());
                json.append("}");
            }
            json.append("]");
            json.append("}");
            return json.toString();
        }

        private int errorCount() {
            int count = 0;
            for (ValidatedImportRow row : rows) {
                if (!row.valid()) {
                    count++;
                }
            }
            return count;
        }
    }

}
