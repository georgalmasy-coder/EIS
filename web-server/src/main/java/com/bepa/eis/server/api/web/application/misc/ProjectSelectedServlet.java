package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.UserPreferenceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Endpoint: POST /api/projectselected
 * Updates the selected customerId and projectId for the current session.
 *
 * Expects form fields:
 * - customerId
 * - projectId
 */
@WebServlet(name = "ProjectSelectedServlet", urlPatterns = "/api/projectselected")
public class ProjectSelectedServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(ProjectSelectedServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sessionId = getSessionId(req);
        WebSession webSession;

        Integer customerId = stringToInteger(req.getParameter("customerId"));
        Integer projectId = stringToInteger(req.getParameter("projectId"));

        if (customerId == null || projectId == null) {
            logAuditEvent(
                    sessionId,
                    "USER_CONTEXT_CHANGE_FAILED",
                    "Invalid customer or project selection. customerId=" + customerId + ", projectId=" + projectId,
                    "Warning"
            );

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            SessionProvider sessionProvider = new SessionProvider(null);
            webSession = sessionProvider.getBySessionId(sessionId);
            webSession.setCustomerId(customerId);
            webSession.setProjectId(projectId);

            boolean updated = sessionProvider.updateSessionInfo(webSession);

            if (updated) {
                new UserPreferenceProvider(webSession).setSelectedProjectId(webSession.getUserId(), projectId);
                logAuditEvent(
                        sessionId,
                        "USER_CONTEXT_CHANGED",
                        "User selected customerId=" + customerId + ", projectId=" + projectId,
                        "OK"
                );
            } else {
                logAuditEvent(
                        sessionId,
                        "USER_CONTEXT_CHANGE_FAILED",
                        "Session context update did not update any rows. customerId=" + customerId + ", projectId=" + projectId,
                        "Warning"
                );
            }
        } catch (SQLException e) {
            log.error("Error updating session: {}", e.getMessage(), e);

            logAuditEvent(
                    sessionId,
                    "USER_CONTEXT_CHANGE_FAILED",
                    "Database error while selecting customerId=" + customerId + ", projectId=" + projectId,
                    "Warning"
            );

            throw new RuntimeException(e);
        }

        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void logAuditEvent(
            String actorEmail,
            String eventType,
            String description,
            String status
    ) {
        try {
            AuditEventProvider auditEventProvider = new AuditEventProvider(null);

            auditEventProvider.logEvent(new AuditEventProvider.AuditEvent(
                    safeActor(actorEmail),
                    eventType,
                    "SESSION",
                    safeActor(actorEmail),
                    description,
                    status
            ));
        } catch (Exception e) {
            log.warn("Could not write project selection audit event. eventType={}, actor={}", eventType, actorEmail, e);
        }
    }

    private String safeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail;
    }

    private static Integer stringToInteger(String string) {
        Integer integer = null;

        if (string != null) {
            if (!string.trim().isEmpty()) {
                try {
                    integer = Integer.parseInt(string.trim());
                } catch (Exception e) {
                    integer = null;
                }
            }
        }

        return integer;
    }
}
