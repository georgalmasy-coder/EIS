package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.MatrixXlsxExporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

abstract class InterfaceMatrixServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMatrixServlet.class);

    private final InterfaceMatrixSnapshotStore snapshots = new InterfaceMatrixSnapshotStore();
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();

    abstract GenericXmlDocument buildInterfaceMatrix (WebSession webSession, EntityType entityType);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        WebSession webSession = getWebSessionFromRequest(request, response);
        if (webSession == null) return;
        setWebSession(webSession);

        String module = request.getServletPath() +  "." + getCommandParameter(request);
        long started = System.nanoTime();
        String command = getCommandParameter(request);
        try {
            if ("snapshot".equals(command) || "block".equals(command) || "view".equals(command) || "refresh".equals(command)) {
                handleLazy(webSession,request,response,command);
            } else if ("export".equals(getCommandParameter(request))) {
                handleExport(webSession, request, response);
            } else if ("remove".equals(getCommandParameter(request))) {
                handleRemove(webSession, request, response);
            } else {
                super.doPost(request, response);
            }
        } catch (Throwable throwable) {
            if (!(throwable instanceof InterfaceMatrixSnapshotStore.Expired)) {
                IncidentProvider incidentProvider = new IncidentProvider(webSession);
                incidentProvider.createProviderServiceIncident(SeverityType.HIGH, module, throwable);
                log.error("Error processing interface matrix action: {}", throwable.getMessage(), throwable);
            }
            writeJsonError(response, throwable);
            if (throwable instanceof InterfaceMatrixSnapshotStore.Expired) response.setStatus(409);
        } finally {
            if (java.util.Set.of("snapshot","block","view","refresh").contains(command == null ? "" : command))
                getPerformanceProvider().logPerformance(module, (System.nanoTime()-started)/1_000_000);
        }
    }

    private void handleLazy(WebSession session, HttpServletRequest request, HttpServletResponse response, String command) throws Exception {
        EntityType type = resolveEntityType(request);
        response.setCharacterEncoding("UTF-8"); response.setHeader("Cache-Control","no-store");
        if ("snapshot".equals(command)) {
            InterfaceMatrixDocument document = switch(type) {
                case SYSTEMS_BREAKDOWN -> new PsysInterfaceMatrixDocument(session,type,true);
                case LOGICAL_STRUCTURE -> new LsysInterfaceMatrixDocument(session,type,true);
                case FUNCTIONAL_STRUCTURE -> new FsysInterfaceMatrixDocument(session,true);
                case STAKEHOLDER_REQUIREMENT -> new StkInterfaceMatrixDocument(session,true);
                case SYSTEM_REQUIREMENT -> new SysInterfaceMatrixDocument(session,true);
                default -> throw new IllegalArgumentException("Unsupported interface entity type");
            };
            String id=snapshots.put(session,type,document.snapshotSource());
            ((Element)document.getRoot().getElementsByTagName("interfaceMatrix").item(0)).setAttribute("snapshotId",id);
            response.setContentType("application/xml; charset=UTF-8");
            response.getWriter().write(document.toXmlString(false));return;
        }
        String id=request.getParameter("snapshotId");
        var source=snapshots.get(session,type,id);
        Object data;
        if ("refresh".equals(command)) {
            String from=request.getParameter("fromEntityId"),to=request.getParameter("toEntityId");
            source.validatePair(from,to);
            var provider=new InterfaceMatrixProvider(session,type);
            var forward=MatrixInterfaceCellsData.snapshotCell(provider.getLatestInterfaceRecord(Integer.valueOf(from),Integer.valueOf(to),type));
            var reverse=MatrixInterfaceCellsData.snapshotCell(provider.getLatestInterfaceRecord(Integer.valueOf(to),Integer.valueOf(from),type));
            var update=snapshots.update(session,type,id,java.util.List.of(
                    new InterfaceMatrixSnapshotStore.Patch(from,to,forward),new InterfaceMatrixSnapshotStore.Patch(to,from,reverse)));
            id=update.snapshotId();data=update;
            log.info("Refreshed interface pair from={} to={} snapshot={}",from,to,id);
        } else if ("block".equals(command)) {
            int row=Integer.parseInt(request.getParameter("row")),column=Integer.parseInt(request.getParameter("column"));
            data=new InterfaceMatrixSnapshotStore.Block(id,row,column,source.block(row,column));
            log.info("Interface block snapshot={} row={} column={}",id,row,column);
        } else {
            String irlText=java.util.Objects.toString(request.getParameter("irlIds"),"");
            if(irlText.length()>2000) throw new IllegalArgumentException("Too many IRL filters");
            var irls=java.util.Arrays.stream(irlText.split(",")).filter(v->!v.isBlank()).collect(java.util.stream.Collectors.toSet());
            data=source.filter(Boolean.parseBoolean(request.getParameter("overdue")),irls,java.time.LocalDate.now(),
                    Boolean.parseBoolean(request.getParameter("connectedOnly")));
        }
        response.setContentType("application/json; charset=UTF-8");
        json.writeValue(response.getWriter(),java.util.Map.of("snapshotId",id,"data",data));
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
            interfaceMatrixProvider.saveInterfaceRecord(saveRecord, Boolean.parseBoolean(request.getParameter("createOnly")));

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
        var snapshot = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(request.getInputStream(), MatrixXlsxExporter.Snapshot.class);
        byte[] data = MatrixXlsxExporter.create(snapshot);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"N2-diagram.xlsx\"");
        response.setHeader("Cache-Control", "no-store");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    protected EntityType resolveEntityType(HttpServletRequest request) {
        return switch (request.getServletPath()) {
            case "/master/psys/interfacematrix" -> EntityType.SYSTEMS_BREAKDOWN;
            case "/master/lsys/interfacematrix" -> EntityType.LOGICAL_STRUCTURE;
            case "/master/fsys/interfacematrix" -> EntityType.FUNCTIONAL_STRUCTURE;
            case "/master/stk/interfacematrix" -> EntityType.STAKEHOLDER_REQUIREMENT;
            case "/master/sys/interfacematrix" -> EntityType.SYSTEM_REQUIREMENT;
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
