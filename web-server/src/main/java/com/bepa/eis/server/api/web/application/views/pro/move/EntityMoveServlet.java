package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.EisDataSourceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet(
        name = "EntityMoveServlet",
        urlPatterns = {
                "/basis/psys/move",
                "/pro/lsys/move",
                "/pro/fsys/move",
                "/basis/stk/move",
                "/basis/sys/move"
        }
)
public class EntityMoveServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(EntityMoveServlet.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String module = getModule(request);
        long startTime = System.currentTimeMillis();
        MoveRequest moveRequest;
        try {
            WebSession webSession = getWebSessionFromRequest(request, response);
            setWebSession(webSession);

            moveRequest = OBJECT_MAPPER.readValue(request.getInputStream(), MoveRequest.class);
            validate(moveRequest);

            AbstractMoveEntity moveEntity = getMoveEntity(moveRequest, request.getServletPath());
            moveEntities(moveEntity);

            getPerformanceProvider().logPerformance(module, System.currentTimeMillis() - startTime);

        } catch (Throwable throwable) {
            getIncidentProvider().createProviderServiceIncident(SeverityType.HIGH, module, throwable);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, throwable.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("OK");
    }

    private void moveEntities(AbstractMoveEntity moveEntity) throws SQLException {
        moveEntity.getAllEntities();
        moveEntity.getEntitiesToMove();

        Connection connection = getDataSource().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            moveEntity.moveEntitiesInHierarchy(connection);

            connection.commit();
//            connection.rollback();
        } catch (Exception ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                log.warn("Rollback failed while saving interface record", rollbackError);
            }
            throw new RuntimeException(ex);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {
                // ignore
            }

            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }

    }

    private AbstractMoveEntity getMoveEntity(MoveRequest request, String url) {
        return switch (url) {
            case "/basis/psys/move" -> new SystemBreakdownMoveEntity(getWebSession(), request);
            case "/pro/lsys/move" -> new LogicalStructureMoveEntity(getWebSession(), request);
            case "/pro/fsys/move" -> new FunctionalStructureMoveEntity(getWebSession(), request);
            case "/basis/stk/move" -> new StakeholderRequirementMoveEntity(getWebSession(), request);
            case "/basis/sys/move" -> new SystemRequirementMoveEntity(getWebSession(), request);
            default -> throw new IllegalArgumentException("No provider configured for move endpoint: " + url);
        };
    }

    private void validate(MoveRequest request) {
        if (request == null || request.fromEntityId() == null) {
            throw new IllegalArgumentException("fromEntityId is required.");
        }
        if (request.toEntityId() != null && request.fromEntityId().equals(request.toEntityId())) {
            throw new IllegalArgumentException("An entity cannot be moved to itself.");
        }
        if (isBlank(request.fromCode())) {
            throw new IllegalArgumentException("fromCode is required.");
        }
        boolean rootTarget = request.toEntityId() == null && isBlank(request.toCode());
        boolean entityTarget = request.toEntityId() != null && !isBlank(request.toCode());
        if (!rootTarget && !entityTarget) {
            throw new IllegalArgumentException("toEntityId and toCode must both identify an entity or both be null for Root.");
        }

        // request seems valid so log the request
        logMoveRequest(request);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void logMoveRequest(MoveRequest request) {
        String logMessage = request.toString();
        log.info(logMessage);
    }

    protected static DataSource getDataSource() {
        return EisDataSourceProvider.getDataSource();
    }

}
