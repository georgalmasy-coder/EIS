package com.bepa.eis.server.api.security;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.security.SessionIdGenerator;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/** Keeps database activity current for all authenticated HTTP requests, including API routes. */
@WebFilter(filterName = "SessionActivityFilter", urlPatterns = "/*")
public class SessionActivityFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        if (!GlobalConfiguration.isUdvMode() && session != null
                && session.getAttribute("sessionID") instanceof String sessionId) {
            try {
                if (!SessionIdGenerator.isValid(sessionId) || !new SessionProvider(null).recordActivity(sessionId,
                        GlobalConfiguration.getSessionTimeoutMinutes())) {
                    session.invalidate();
                    resp.setHeader("Cache-Control", "no-store");
                    if (req.getRequestURI().contains("/api/")) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.setContentType("application/json;charset=UTF-8");
                        resp.getWriter().write("{\"message\":\"Session expired\"}");
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/session-expired.html");
                    }
                    return;
                }
            } catch (SQLException e) {
                throw new ServletException("Unable to record session activity", e);
            }
        }
        chain.doFilter(request, response);
    }
}
