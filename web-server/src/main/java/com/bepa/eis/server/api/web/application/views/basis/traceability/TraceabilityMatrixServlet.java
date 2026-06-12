package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.EntityRelation;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityRelations;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.common.enums.entity.EntityType;
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

import static com.bepa.eis.common.enums.entity.EntityType.*;

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

    private static final String REMOVE_RELATION_PATH = "/removerelation";
    private static final String CONFIRM_RELATION_PATH = "/confirmrelation";

    private static final String DEFAULT_STYLE = "normal";
    private static final String STYLE_YELLOW = "yellow";
    private static final String CONFIRMED_STYLE = "green";
    private static final String CONFIRMED_VALUE = "X";

    private EntityRelationProvider entityRelationProvider = null;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String pathInfo = normalizePathInfo(request.getPathInfo());

        try {
            if (REMOVE_RELATION_PATH.equals(pathInfo)) {
                handleRemoveRelationRequest(request, response);
                return;
            }

            if (CONFIRM_RELATION_PATH.equals(pathInfo)) {
                handleConfirmRelationRequest(request, response);
                return;
            }

            super.doPost(request, response);
        } catch (Exception e) {
            log.error("Error processing traceability matrix action: {}", e.getMessage(), e);
            writeJsonError(response, e);
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

    private void handleRemoveRelationRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        WebSession webSession = getWebSessionFromRequest(request);
        EntityRelation entityRelation = getConfirmedRelation(webSession, actionRequest);

        if (entityRelation == null) {
            log.warn("No confirmed relation found for rowId={}, rowCode={}, columnId={}, columnCode={}",
                    actionRequest.rowId(),
                    actionRequest.rowCode(),
                    actionRequest.columnId(),
                    actionRequest.columnCode());
        } else {
            getProvider(webSession).removeEntityRelation(entityRelation.getEntityRelationPK().getValue());
            log.info("Confirmed relation removed for rowId={}, rowCode={}, columnId={}, columnCode={}",
                    actionRequest.rowId(),
                    actionRequest.rowCode(),
                    actionRequest.columnId(),
                    actionRequest.columnCode());
            writeJsonStatusResponse(response, STYLE_YELLOW, "");
        }

        log.info(
                "Remove traceability relation requested. rowId={}, rowCode={}, columnId={}, columnCode={}, rowIndex={}, columnIndex={}, style={}, value={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode(),
                actionRequest.rowIndex(),
                actionRequest.columnIndex(),
                actionRequest.style(),
                actionRequest.value()
        );

        /*
         * TODO:
         * Persist relation removal here when the relation storage/API is ready.
         *
         * Current behavior:
         * Return the new cell status so the client can update the matrix cell without reloading
         * the complete XML matrix.
         */
        //writeJsonStatusResponse(response, DEFAULT_STYLE, "");
    }

    private void handleConfirmRelationRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TraceabilityRelationActionRequest actionRequest = parseRelationActionRequest(request);

        WebSession webSession = getWebSessionFromRequest(request);
        EntityRelation entityRelation = getConfirmedRelation(webSession, actionRequest);

        if (entityRelation != null) {
            // There is already a confirmed relation, so we need to confirm it again
            log.warn("No confirmed relation found for rowId={}, rowCode={}, columnId={}, columnCode={}",
                    actionRequest.rowId(),
                    actionRequest.rowCode(),
                    actionRequest.columnId(),
                    actionRequest.columnCode());
        } else {
            Integer stakeholderRequirementId = Integer.parseInt(actionRequest.rowId());
            Integer systemRequirementId = Integer.parseInt(actionRequest.columnId());
            getProvider(webSession).insertEntityRelation(STAKEHOLDER_REQUIREMENT, stakeholderRequirementId,
                    SYSTEM_REQUIREMENT, systemRequirementId);
            log.info("New confirmed relation inserted rowId={}, rowCode={}, columnId={}, columnCode={}",
                    actionRequest.rowId(),
                    actionRequest.rowCode(),
                    actionRequest.columnId(),
                    actionRequest.columnCode());
            writeJsonStatusResponse(response, CONFIRMED_STYLE, CONFIRMED_VALUE);
        }
/*
        log.info(
                "Confirm traceability relation requested. rowId={}, rowCode={}, columnId={}, columnCode={}, rowIndex={}, columnIndex={}, style={}, value={}",
                actionRequest.rowId(),
                actionRequest.rowCode(),
                actionRequest.columnId(),
                actionRequest.columnCode(),
                actionRequest.rowIndex(),
                actionRequest.columnIndex(),
                actionRequest.style(),
                actionRequest.value()
        );
*/
        /*
         * TODO:
         * Persist relation confirmation here when the relation storage/API is ready.
         *
         * Current behavior:
         * Return the new cell status so the client can update the matrix cell without reloading
         * the complete XML matrix.
         */
    }

    private EntityRelation getConfirmedRelation(WebSession webSession, TraceabilityRelationActionRequest actionRequest) {
        EntityRelationProvider entityRelationProvider2 = new EntityRelationProvider(webSession);
        EntityRelations entityRelations = null;
        try {
            entityRelations = getProvider(webSession).getEntityRelationsByEntityId(
                    EntityType.STAKEHOLDER_REQUIREMENT,
                    Integer.parseInt(actionRequest.rowId()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (AbstractField field : entityRelations.getElements()) {
            EntityRelation entityRelation = (EntityRelation) field;

            log.debug("Entity Relation : {}", entityRelation);

            if (entityRelation.getRelatedEntityType() == EntityType.SYSTEM_REQUIREMENT) {
                if (entityRelation.getRelatedEntityId().getValue() == Integer.parseInt(actionRequest.columnId())) {
                    log.debug("Entity Relation found: {}", entityRelation);
                    return entityRelation;
                }
            }

        }

        log.debug("Entity Relation not found - action request: {}", actionRequest);
        return null;
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

    private void writeJsonError(HttpServletResponse response, Exception exception) throws ServletException {
        try {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");

            response.getWriter().write(
                    "{"
                            + "\"error\":\"" + escapeJson(exception.getMessage()) + "\""
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