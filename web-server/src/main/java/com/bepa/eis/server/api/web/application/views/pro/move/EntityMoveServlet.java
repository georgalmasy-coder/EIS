package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.server.api.generic.GenericServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(
        name = "EntityMoveServlet",
        urlPatterns = {
                "/master/psys/move",
                "/master/lsys/move",
                "/master/fsys/move",
                "/master/stk/move",
                "/master/sys/move"
        }
)
public class EntityMoveServlet extends GenericServlet {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MoveRequest moveRequest;
        try {
            moveRequest = OBJECT_MAPPER.readValue(request.getInputStream(), MoveRequest.class);
            validate(moveRequest);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("OK");
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
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MoveRequest(Integer fromEntityId, String fromCode, Integer toEntityId, String toCode) {
    }
}
