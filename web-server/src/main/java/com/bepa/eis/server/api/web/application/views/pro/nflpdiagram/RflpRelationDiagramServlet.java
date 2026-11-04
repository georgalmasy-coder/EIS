package com.bepa.eis.server.api.web.application.views.pro.nflpdiagram;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.relationdiagram.RelationDiagramSnapshotStore;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@WebServlet(name = "NflpRelationDiagramServlet", urlPatterns = {"/pro/nflprelationdiagram"})
@MultipartConfig
public class RflpRelationDiagramServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(RflpRelationDiagramServlet.class);

    private final RelationDiagramSnapshotStore snapshots = new RelationDiagramSnapshotStore();
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws jakarta.servlet.ServletException {
        String command = getCommandParameter(request);
        if (!java.util.Set.of("snapshot", "trace", "details", "search").contains(command == null ? "" : command)) {
            super.doPost(request, response); return;
        }
        WebSession session = getWebSessionFromRequest(request, response);
        if (session == null) return;
        setWebSession(session);
        long start = System.currentTimeMillis();
        try {
            response.setCharacterEncoding("UTF-8"); response.setHeader("Cache-Control", "no-store");
            if ("snapshot".equals(command)) {
                var document = new RflpRelationDiagramDocument(session, true);
                String id = snapshots.put(session, document.snapshot());
                ((Element) document.getRoot().getElementsByTagName("NflpRelationDiagram").item(0)).setAttribute("snapshotId", id);
                response.setContentType("application/xml; charset=UTF-8");
                response.getWriter().write(document.toXmlString(false));
            } else {
                var graph = snapshots.get(session, required(request, "snapshotId"));
                Object data = switch (command) {
                    case "trace" -> graph.trace(required(request, "internalId"));
                    case "details" -> graph.details(required(request, "internalId"));
                    case "search" -> graph.search(java.util.Objects.toString(request.getParameter("text"), ""));
                    default -> throw new IllegalArgumentException("Invalid command");
                };
                response.setContentType("application/json; charset=UTF-8");
                json.writeValue(response.getWriter(), java.util.Map.of("snapshotId", required(request, "snapshotId"), "data", data));
                log.info("Relation diagram {} snapshot={} entity={} took {} ms", command, request.getParameter("snapshotId"), request.getParameter("internalId"), System.currentTimeMillis() - start);
            }
        } catch (Exception error) {
            try {
                response.setStatus(error instanceof RelationDiagramSnapshotStore.Expired ? 409 : 400);
                response.setContentType("application/json; charset=UTF-8");
                json.writeValue(response.getWriter(), java.util.Map.of("error", java.util.Objects.toString(error.getMessage(), "Diagram error")));
                if (!(error instanceof RelationDiagramSnapshotStore.Expired)) log.error("Relation diagram {} failed", command, error);
            } catch (java.io.IOException writeError) { throw new jakarta.servlet.ServletException(writeError); }
        } finally {
            getPerformanceProvider().logPerformance(request.getServletPath() + "." + command, System.currentTimeMillis() - start);
        }
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }

    @Override
    public GenericXmlDocument handleOverview(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return buildRelationDiagram(webSession);
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

    private GenericXmlDocument buildRelationDiagram(WebSession webSession) {
        try {
            return new RflpRelationDiagramDocument(webSession);
        } catch (Exception e) {
            log.error("Error getting relation diagram for RFLP : {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new RuntimeException("Invalid export request");
    }

}