package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.providers.entityrelation.RelationProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.MatrixXlsxExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;
import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

@WebServlet(
        name = "TraceabilityMatrixServlet",
        urlPatterns = {
                "/basis/basistraceability",
                "/basis/basistraceability/*"
        }
)
@MultipartConfig
public class TraceabilityMatrixServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(TraceabilityMatrixServlet.class);

    private static final String CONFIRM_RELATION_PATH = "/confirmrelation";
    private static final String REMOVE_CONFIRMED_RELATION_PATH = "/removeconfirmedrelation";
    private static final String MARK_RELATION_NOT_RELEVANT_PATH = "/relationnotrelevant";
    private static final String REMOVE_NOT_RELEVANT_RELATION_PATH = "/removenotrelevantrelation";

    private static final String STYLE_NORMAL = "normal";
    private static final String STYLE_YELLOW = "yellow";
    private static final String STYLE_GREEN = "green";
    private static final String STYLE_GRAY_ITALIC = "grayItalic";

    private static final String VALUE_EMPTY = "";
    private static final String VALUE_CONFIRMED_RELATION = "X";
    private static final String VALUE_NOT_RELEVANT = "NR";

    private EntityRelationProvider entityRelationProvider = null;
    private final TraceabilitySnapshotStore snapshots = new TraceabilitySnapshotStore();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String pathInfo = normalizePathInfo(request.getPathInfo());

        WebSession webSession = getWebSessionFromRequest(request, response);
        if (webSession == null) return;
        setWebSession(webSession);

        String module = request.getServletPath() + pathInfo + "." + getCommandParameter(request);
        long startTime = System.currentTimeMillis();
        boolean measuredHere = true;

        try {
            if ("snapshot".equals(getCommandParameter(request)) && pathInfo.isEmpty()) {
                TraceabilityMatrixDocument document = new TraceabilityMatrixDocument(webSession, true);
                String id = snapshots.put(webSession, document);
                Element matrix = (Element) document.getRoot().getElementsByTagName("traceabilityMatrix").item(0);
                matrix.setAttribute("snapshotId", id);
                matrix.setAttribute("blockSize", Integer.toString(TraceabilitySnapshotStore.BLOCK_SIZE));
                response.setContentType("application/xml; charset=UTF-8");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write(document.toXmlString(false));
                return;
            }
            if ("block".equals(getCommandParameter(request)) && pathInfo.isEmpty()) {
                var block = snapshots.block(webSession, getRequiredParameter(request, "snapshotId"),
                        Integer.parseInt(getRequiredParameter(request, "rowStart")),
                        Integer.parseInt(getRequiredParameter(request, "columnStart")));
                log.info("Traceability block snapshot={} rows={}:{} columns={}:{} cells={} cacheHit={}",
                        block.snapshotId(), block.rowStart(), block.rowEnd(), block.columnStart(), block.columnEnd(),
                        (block.rowEnd() - block.rowStart()) * (block.columnEnd() - block.columnStart()), block.cacheHit());
                response.setContentType("application/json; charset=UTF-8");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("Cache-Control", "no-store");
                new ObjectMapper().writeValue(response.getWriter(), block);
                return;
            }
            if ("export".equals(getCommandParameter(request)) && (pathInfo.isEmpty() || "/".equals(pathInfo))) {
                handleExport(webSession, request, response);
                return;
            }
            if ("/matchingwords".equals(pathInfo)) {
                handleMatchingWordsRequest(webSession, request, response);
                return;
            }

            if (REMOVE_CONFIRMED_RELATION_PATH.equals(pathInfo) || REMOVE_NOT_RELEVANT_RELATION_PATH.equals(pathInfo)
                    || CONFIRM_RELATION_PATH.equals(pathInfo) || MARK_RELATION_NOT_RELEVANT_PATH.equals(pathInfo)) {
                snapshots.validatePair(webSession, getRequiredParameter(request, "snapshotId"),
                        Integer.parseInt(getRequiredParameter(request, "rowId")), Integer.parseInt(getRequiredParameter(request, "columnId")));
            }

            if (REMOVE_CONFIRMED_RELATION_PATH.equals(pathInfo)) {
                handleRemoveConfirmedRelationRequest(webSession, request, response);
                return;
            }

            if (REMOVE_NOT_RELEVANT_RELATION_PATH.equals(pathInfo)) {
                handleRemoveNotRelevantRelationRequest(webSession, request, response);
                return;
            }

            if (CONFIRM_RELATION_PATH.equals(pathInfo)) {
                handleConfirmRelationRequest(webSession, request, response);
                return;
            }

            if (MARK_RELATION_NOT_RELEVANT_PATH.equals(pathInfo)) {
                handleMarkRelationNotRelevantRequest(webSession, request, response);
                return;
            }

            measuredHere = false; // GenericDataProviderServlet measures its own commands.
            super.doPost(request, response);
        } catch (TraceabilitySnapshotStore.ExpiredSnapshotException expired) {
            try {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json; charset=UTF-8");
                new ObjectMapper().writeValue(response.getWriter(), java.util.Map.of("error", expired.getMessage()));
            } catch (IOException error) { throw new ServletException(error); }
        } catch (Throwable throwable) {
            getIncidentProvider().createProviderServiceIncident(SeverityType.HIGH, module, throwable);
            log.error("Error processing traceability matrix action: {}", throwable.getMessage(), throwable);
            writeJsonError(response, throwable);
        } finally {
            if (measuredHere) getPerformanceProvider().logPerformance(module, System.currentTimeMillis() - startTime);
        }
    }



    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return buildTraceabilityMatrix(webSession);
    }

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new RuntimeException("Invalid import request");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        throw new RuntimeException("Invalid save request");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        throw new RuntimeException("Invalid list request");
    }

    @Override
    public GenericXmlDocument handleEditEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer entityId, Integer version) throws Throwable {
        throw new RuntimeException("Invalid edit request");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(WebSession webSession, HttpServletRequest request, HttpServletResponse response, Integer parentEntityId) throws Throwable {
        throw new RuntimeException("Invalid create request");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        var snapshot = new ObjectMapper().readValue(request.getInputStream(), MatrixXlsxExporter.Snapshot.class);
        byte[] data = MatrixXlsxExporter.create(snapshot);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"Traceability-matrix.xlsx\"");
        response.setHeader("Cache-Control", "no-store");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    private GenericXmlDocument buildTraceabilityMatrix(WebSession webSession) {
        try {
            return new TraceabilityMatrixDocument(webSession);
        } catch (Exception e) {
            log.error("Error getting traceability matrix of stakeholder/system requirements: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleMatchingWordsRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequirementTextMatcher matcher = new RequirementTextMatcher(webSession);
        var matches = matcher.matchingWords(
                RequirementTextMatcher.words(request.getParameter("stakeholderName"),
                        request.getParameter("stakeholderDescription")),
                RequirementTextMatcher.words(request.getParameter("systemName"),
                        request.getParameter("systemDescription")));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        new ObjectMapper().writeValue(response.getWriter(), matches);
    }

    private EntityRelationProvider getProvider(WebSession webSession) {
        if (entityRelationProvider == null) {
            entityRelationProvider = new EntityRelationProvider(webSession);
        }
        return entityRelationProvider;
    }

    private void handleConfirmRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(webSession, actionRequest);
        clearLatestIfExists(webSession, relationRecord);
        insertRelationRecord(webSession, RelationType.CONFIRMED, relationRecord, actionRequest);

        log.info(
                "Confirmed relation inserted. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode());

        writeRelationUpdate(webSession, request, response, actionRequest, RelationType.CONFIRMED);
    }

    private RelationProvider getRelationProvider(WebSession webSession) {
        return new RelationProvider(webSession);
    }

    private void writeRelationUpdate(WebSession session, HttpServletRequest request, HttpServletResponse response,
                                     TraceabilityRelationActionRequest action, RelationType expected) throws SQLException, IOException {
        // Read the saved record, rather than assuming a failed write succeeded.
        EntityRelationRecord saved = getEntityRelationRecord(session, action);
        if (saved.getRelationType() != expected) throw new SQLException("Relation change could not be verified");
        var update = snapshots.update(session, getRequiredParameter(request, "snapshotId"),
                Integer.parseInt(action.rowId()), Integer.parseInt(action.columnId()), saved);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        new ObjectMapper().writeValue(response.getWriter(), update);
    }

    private void handleRemoveConfirmedRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(webSession, actionRequest);
        clearLatestIfExists(webSession, relationRecord);
        insertRelationRecord(webSession, RelationType.DELETED, relationRecord, actionRequest);

        log.info(
                "Confirmed relation removed. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeRelationUpdate(webSession, request, response, actionRequest, RelationType.DELETED);

    }

    private void handleMarkRelationNotRelevantRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(webSession, actionRequest);
        getRelationProvider(webSession).clearLatestIfExists(relationRecord);
        insertRelationRecord(webSession, RelationType.NOT_RELEVANT, relationRecord, actionRequest);

        log.info(
                "Not relevant relation inserted. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeRelationUpdate(webSession, request, response, actionRequest, RelationType.NOT_RELEVANT);

    }

    private void handleRemoveNotRelevantRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(webSession, actionRequest);
        getRelationProvider(webSession).clearLatestIfExists(relationRecord);
        insertRelationRecord(webSession, RelationType.DELETED, relationRecord, actionRequest);

        log.info(
                "Not relevant relation removed. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeRelationUpdate(webSession, request, response, actionRequest, RelationType.DELETED);

    }

    private void clearLatestIfExists(WebSession webSession, EntityRelationRecord relationRecord) {
        if (relationRecord != null && relationRecord.getEntityRelationPK() != null) {
            getRelationProvider(webSession).clearLatestIfExists(relationRecord);
        }
    }

    private void insertRelationRecord(WebSession webSession, RelationType relationType, EntityRelationRecord relationRecord, TraceabilityRelationActionRequest actionRequest) {
        if (relationType != null && relationRecord != null && actionRequest != null) {
            getRelationProvider(webSession).insertRelationRecord(relationType, relationRecord);
        }
    }

    private EntityRelationRecord getEntityRelationRecord(WebSession webSession, TraceabilityRelationActionRequest actionRequest) throws SQLException {
        EntityRelationRecord relationRecord =  getRelationProvider(webSession).getEntityRelationByEntityTypeAndId(
                STAKEHOLDER_REQUIREMENT,
                toInteger(actionRequest.rowId),
                SYSTEM_REQUIREMENT,
                toInteger(actionRequest.columnId));

        if (relationRecord == null) {
            relationRecord = new EntityRelationRecord(webSession.getCustomerId(), webSession.getProjectId());
            relationRecord.setCreatedByUserId(webSession.getUserId());
            relationRecord.setEntityType(STAKEHOLDER_REQUIREMENT);
            relationRecord.setEntityId(toInteger(actionRequest.rowId));
            relationRecord.setRelatedEntityType(SYSTEM_REQUIREMENT);
            relationRecord.setRelatedEntityId(toInteger(actionRequest.columnId));
            relationRecord.setVersion(null);
            relationRecord.setLatest(true);
        }

        return relationRecord;
    }

    private TraceabilityRelationActionRequest parseRelationActionRequest(HttpServletRequest request) {
        String rowId = getRequiredParameter(request, "rowId");
        String rowCode = getOptionalParameter(request, "rowCode");
        String columnId = getRequiredParameter(request, "columnId");
        String columnCode = getOptionalParameter(request, "columnCode");
        String rowIndex = getOptionalParameter(request, "rowIndex");
        String columnIndex = getOptionalParameter(request, "columnIndex");
        String style = getOptionalParameter(request, "style");
        String value = getOptionalParameter(request, "value");

        return new TraceabilityRelationActionRequest(
                rowId,
                rowCode,
                columnId,
                columnCode,
                rowIndex,
                columnIndex,
                style,
                value
        );
    }

    private String getRequiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }

        return value.trim();
    }

    private String getOptionalParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);

        return value == null ? "" : value.trim();
    }

    private String normalizePathInfo(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return "";
        }

        return pathInfo.trim().toLowerCase();
    }

    private void writeJsonStatusResponse(HttpServletResponse response, String style, String value) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        response.getWriter().write(
                "{"
                        + "\"style\":\"" + escapeJson(style) + "\","
                        + "\"value\":\"" + escapeJson(value) + "\""
                        + "}"
        );
    }

    private void writeJsonError(HttpServletResponse response, Throwable throwable) throws ServletException {
        try {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");

            response.getWriter().write(
                    "{"
                            + "\"error\":\"" + escapeJson(throwable.getMessage()) + "\""
                            + "}"
            );
        } catch (IOException ioException) {
            throw new ServletException("Unable to write error response", ioException);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private record TraceabilityRelationActionRequest(
            String rowId,
            String rowCode,
            String columnId,
            String columnCode,
            String rowIndex,
            String columnIndex,
            String style,
            String value
    ) {
    }

}
