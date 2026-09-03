package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
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


/*
@WebServlet(
        name = "InterfaceMatrixServlet",
        urlPatterns = {
                "/pro/psys/interfacematrix",
                "/pro/psys/interfacematrix/*"
        }
)
@MultipartConfig

 */
abstract class InterfaceMatrixServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMatrixServlet.class);

    abstract GenericXmlDocument buildInterfaceMatrix (WebSession webSession, EntityType entityType);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        WebSession webSession = getWebSessionFromRequest(request);
        setWebSession(webSession);

        String module = request.getServletPath() +  "." + getCommandParameter(request);
        try {
            if ("remove".equals(getCommandParameter(request))) {
                handleRemove(webSession, request, response);
            } else {
                super.doPost(request, response);
            }
        } catch (Throwable throwable) {
            IncidentProvider incidentProvider = new IncidentProvider(webSession);
            incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);
            log.error("Error processing traceability matrix action: {}", throwable.getMessage(), throwable);
            writeJsonError(response, throwable);
        }
    }

    private void handleRemove(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        Integer fromEntityId = toInteger(request.getParameter("fromEntityId"));
        Integer toEntityId = toInteger(request.getParameter("toEntityId"));

        if (fromEntityId == null || toEntityId == null) {
            throw new IllegalArgumentException("fromEntityId and toEntityId are required.");
        }

        try {
            EntityType entityType = resolveEntityType(request);
            InterfaceMatrixProvider provider = new InterfaceMatrixProvider(webSession, entityType);
            if (!provider.removeInterfaceRecord(fromEntityId, toEntityId)) {
                throw new IllegalArgumentException("Interface was not found or has already been removed.");
            }
            response.setStatus(HttpServletResponse.SC_OK);
            log.info("Removed interface cell. fromEntityId={}, toEntityId={}", fromEntityId, toEntityId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove interface cell", e);
        }
    }

/*
    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return buildInterfaceMatrix(webSession, resolveEntityType(request));
    }
*/
    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new RuntimeException("Invalid import request");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        try {
            Element matrixElement = rootElement;
            if (!"interfaceMatrix".equalsIgnoreCase(rootElement.getTagName())) {
                matrixElement = firstChild(rootElement, "interfaceMatrix");
            }

            if (matrixElement == null) {
                throw new IllegalArgumentException("Missing interfaceMatrix element.");
            }

            Element cellElement = firstChild(matrixElement, "cell");
            if (cellElement == null) {
                cellElement = matrixElement;
            }

            InterfaceMatrixProvider.InterfaceSaveRecord saveRecord = parseSaveRecord(cellElement);
            EntityType entityType = resolveEntityType(request);
            InterfaceMatrixProvider interfaceMatrixProvider = new InterfaceMatrixProvider(webSession, entityType);
            interfaceMatrixProvider.saveInterfaceRecord(saveRecord);

            log.info(
                    "Saved interface cell. fromEntityId={}, toEntityId={}, irlId={}",
                    saveRecord.fromEntityId(),
                    saveRecord.toEntityId(),
                    saveRecord.irlId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save interface cell", e);
        }
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

/*
    protected GenericXmlDocument buildInterfaceMatrix(WebSession webSession, EntityType entityType) {
        try {
            return new PsysInterfaceMatrixDocument(webSession, entityType);
        } catch (Exception e) {
            try {
                logIncidentError("InterfaceMatrixServlet", e);
            } catch (Throwable throwable) {
                // Ignore error
            }

            log.error("Error getting interface management matrix document: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
*/

    protected EntityType resolveEntityType(HttpServletRequest request) {
        return switch (request.getServletPath()) {
            case "/pro/psys/interfacematrix" -> EntityType.SYSTEMS_BREAKDOWN;
            default -> throw new IllegalArgumentException(
                    "No EntityType configured for interface matrix endpoint: " + request.getServletPath()
            );
        };
    }

    private InterfaceMatrixProvider.InterfaceSaveRecord parseSaveRecord(Element cellElement) {
        Integer fromEntityId = intValue(cellElement, "fromEntityId");
        Integer toEntityId = intValue(cellElement, "toEntityId");
        Integer irlId = intValue(cellElement, "irlId");
        String nextIrlMeeting = textValue(cellElement, "nextIrlMeeting");
        String classificationIds = textValue(cellElement, "classificationIds");

        if (fromEntityId == null) {
            throw new IllegalArgumentException("Missing required field: fromEntityId");
        }

        if (toEntityId == null) {
            throw new IllegalArgumentException("Missing required field: toEntityId");
        }

        if (irlId == null) {
            throw new IllegalArgumentException("Missing required field: irlId");
        }

        return new InterfaceMatrixProvider.InterfaceSaveRecord(
                fromEntityId,
                toEntityId,
                irlId,
                nextIrlMeeting,
                classificationIds
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

}
