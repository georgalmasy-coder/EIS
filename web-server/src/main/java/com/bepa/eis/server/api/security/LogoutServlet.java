package com.bepa.eis.server.api.security;

import com.bepa.eis.common.providers.security.SessionManager;
import com.bepa.eis.server.api.generic.GenericServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "LogoutServlet", urlPatterns = "/api/logout")
public class LogoutServlet extends GenericServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logout(req, resp);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String sessionId = resolveSessionId(req, session);

        if (sessionId != null && !sessionId.isBlank()) {
            SessionManager.getInstance().logout(sessionId);
        }

        if (session != null) {
            session.invalidate();
        }

        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");
        resp.sendRedirect(req.getContextPath() + "/index.html");
    }

    private String resolveSessionId(HttpServletRequest req, HttpSession session) {
        String sessionId = null;

        if (session != null) {
            Object sessionIdAttribute = session.getAttribute("sessionID");

            if (sessionIdAttribute != null) {
                sessionId = String.valueOf(sessionIdAttribute);
            }
        }

        if ((sessionId == null || sessionId.isBlank())) {
            sessionId = getSessionId(req);
        }

        return sessionId;
    }
}