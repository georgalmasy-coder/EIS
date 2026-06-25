package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.common.providers.entityrelation.RelationProvider;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.EntityRelation;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityRelations;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.formula.functions.T;
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String pathInfo = normalizePathInfo(request.getPathInfo());

        WebSession webSession = getWebSessionFromRequest(request);
        setWebSession(webSession);

        String module = request.getServletPath() +  "." + getCommandParameter(request);
        long startTime = System.currentTimeMillis();

        try {
            if (REMOVE_CONFIRMED_RELATION_PATH.equals(pathInfo)) {
                handleRemoveConfirmedRelationRequest(webSession, request, response);
                PerformanceProvider performanceProvider = new PerformanceProvider(getWebSession());
                performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);
                return;
            }

            if (REMOVE_NOT_RELEVANT_RELATION_PATH.equals(pathInfo)) {
                handleRemoveNotRelevantRelationRequest(webSession, request, response);
                PerformanceProvider performanceProvider = new PerformanceProvider(getWebSession());
                performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);
                return;
            }

            if (CONFIRM_RELATION_PATH.equals(pathInfo)) {
                handleConfirmRelationRequest(webSession, request, response);
                PerformanceProvider performanceProvider = new PerformanceProvider(getWebSession());
                performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);
                return;
            }

            if (MARK_RELATION_NOT_RELEVANT_PATH.equals(pathInfo)) {
                handleMarkRelationNotRelevantRequest(webSession, request, response);
                PerformanceProvider performanceProvider = new PerformanceProvider(getWebSession());
                performanceProvider.logPerformance(module, System.currentTimeMillis() - startTime);
                return;
            }

            super.doPost(request, response);
        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);
            log.error("Error processing traceability matrix action: {}", throwable.getMessage(), throwable);
            writeJsonError(response, throwable);
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
        throw new RuntimeException("Invalid export request");
    }

    private GenericXmlDocument buildTraceabilityMatrix(WebSession webSession) {
        try {
            return new TraceabilityMatrixDocument(webSession);
        } catch (Exception e) {
            log.error("Error getting traceability matrix of stakeholder/system requirements: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private EntityRelationProvider getProvider(WebSession webSession) {
        if (entityRelationProvider == null) {
            entityRelationProvider = new EntityRelationProvider(webSession);
        }
        return entityRelationProvider;
    }

    private void handleConfirmRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(actionRequest);
        clearLatestIfExists(relationRecord);
        insertRelationRecord(RelationType.CONFIRMED, relationRecord, actionRequest);

        log.info(
                "Confirmed relation inserted. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode());

        writeJsonStatusResponse(response, STYLE_GREEN, VALUE_CONFIRMED_RELATION);
    }

    private RelationProvider getRelationProvider() {
        return new RelationProvider(getWebSession());
    }

    private void handleRemoveConfirmedRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(actionRequest);
        clearLatestIfExists(relationRecord);
        insertRelationRecord(RelationType.DELETED, relationRecord, actionRequest);

        log.info(
                "Confirmed relation removed. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeJsonStatusResponse(response, STYLE_YELLOW, VALUE_EMPTY);

    }

    private void handleMarkRelationNotRelevantRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(actionRequest);
        getRelationProvider().clearLatestIfExists(relationRecord);
        insertRelationRecord(RelationType.NOT_RELEVANT, relationRecord, actionRequest);

        log.info(
                "Not relevant relation inserted. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeJsonStatusResponse(response, STYLE_GRAY_ITALIC, VALUE_NOT_RELEVANT);

    }

    private void handleRemoveNotRelevantRelationRequest(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        EntityRelationRecord relationRecord =  getEntityRelationRecord(actionRequest);
        getRelationProvider().clearLatestIfExists(relationRecord);
        insertRelationRecord(RelationType.DELETED, relationRecord, actionRequest);

        log.info(
                "Not relevant relation removed. rowId={}, rowCode={}, columnId={}, columnCode={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode()
        );

        writeJsonStatusResponse(response, STYLE_YELLOW, VALUE_EMPTY);

    }

    private void clearLatestIfExists(EntityRelationRecord relationRecord) {
        if (relationRecord != null && relationRecord.getEntityRelationPK() != null) {
            getRelationProvider().clearLatestIfExists(relationRecord);
        }
    }

    private void insertRelationRecord(RelationType relationType, EntityRelationRecord relationRecord, TraceabilityRelationActionRequest actionRequest) {
        if (relationType != null && relationRecord != null && actionRequest != null) {
            getRelationProvider().insertRelationRecord(relationType, relationRecord);
        }
    }

    private EntityRelationRecord getEntityRelationRecord(TraceabilityRelationActionRequest actionRequest) throws SQLException {
        EntityRelationRecord relationRecord =  getRelationProvider().getEntityRelationByEntityTypeAndId(
                STAKEHOLDER_REQUIREMENT,
                toInteger(actionRequest.rowId),
                SYSTEM_REQUIREMENT,
                toInteger(actionRequest.columnId));

        if (relationRecord == null) {
            relationRecord = new EntityRelationRecord(getWebSession().getCustomerId(), getWebSession().getProjectId());
            relationRecord.setCreatedByUserId(getWebSession().getUserId());
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