package com.bepa.eis.server.api.security;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.customer.CustomerAccessProvider;
import com.bepa.eis.common.providers.security.SessionManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(filterName = "AuthFilter",
        urlPatterns = {
                "/web/*",
                "/web/view/*",
                "/home.html",
                "/select-project.html",
                "/app/*",
                "/api/customersprojects/*",
                "/api/secure/*",
                "/admin/*",
                "/admin/api/*",
                "/temp/index.html"
        })
public class AuthFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        addNoCacheHeaders(resp);

        if (GlobalConfiguration.isUdvMode()) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("sessionID") != null;

        if (!loggedIn) {
            handleUnauthenticated(req, resp, session);
            return;
        }

        if (!isCustomerAccessAllowed(session)) {
            handleCustomerAccessDenied(req, resp, session);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isCustomerAccessAllowed(HttpSession session) {
        Integer customerId = resolveCustomerId(session);

        /*
         * If no customer has been selected yet, we do not block access here.
         * This allows users to reach pages such as customer/project selection.
         */
        if (customerId == null) {
            return true;
        }

        CustomerAccessProvider customerAccessProvider = new CustomerAccessProvider(null);

        return customerAccessProvider.isCustomerLoginAllowed(customerId);
    }

    private void handleUnauthenticated(
            HttpServletRequest req,
            HttpServletResponse resp,
            HttpSession session
    ) throws IOException {
        String sessionId = resolveSessionId(req, session);

        if (sessionId != null && !sessionId.isBlank()) {
            SessionManager.getInstance().expire(sessionId);
        }

        if (isApiRequest(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"message\":\"Unauthorized\"}");
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/login.html");
    }

    private void handleCustomerAccessDenied(
            HttpServletRequest req,
            HttpServletResponse resp,
            HttpSession session
    ) throws IOException {
        String sessionId = resolveSessionId(req, session);

        if (sessionId != null && !sessionId.isBlank()) {
            SessionManager.getInstance().expire(sessionId);
        }

        if (session != null) {
            session.invalidate();
        }

        if (isApiRequest(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"message\":\"Customer account access is not allowed\"}");
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/login.html?error=customer-access-denied");
    }

    private boolean isApiRequest(HttpServletRequest req) {
        String path = req.getRequestURI();

        if (path == null) {
            return false;
        }

        return path.contains("/api/");
    }

    private String resolveSessionId(
            HttpServletRequest req,
            HttpSession session
    ) {
        if (session != null) {
            Object sessionIdAttribute = session.getAttribute("sessionID");

            if (sessionIdAttribute != null) {
                return String.valueOf(sessionIdAttribute);
            }
        }

        String headerSessionId = req.getHeader("X-Session-ID");

        if (headerSessionId != null && !headerSessionId.isBlank()) {
            return headerSessionId;
        }

        return null;
    }

    private Integer resolveCustomerId(HttpSession session) {
        if (session == null) {
            return null;
        }

        Integer customerId = asInteger(session.getAttribute("customerId"));

        if (customerId != null) {
            return customerId;
        }

        customerId = asInteger(session.getAttribute("CustomerId"));

        if (customerId != null) {
            return customerId;
        }

        customerId = asInteger(session.getAttribute("CUSTOMER_ID"));

        if (customerId != null) {
            return customerId;
        }

        Object webSession = session.getAttribute("webSession");

        if (webSession != null) {
            customerId = invokeIntegerGetter(webSession, "getCustomerId");

            if (customerId != null) {
                return customerId;
            }
        }

        webSession = session.getAttribute("WebSession");

        if (webSession != null) {
            customerId = invokeIntegerGetter(webSession, "getCustomerId");

            if (customerId != null) {
                return customerId;
            }
        }

        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integerValue) {
            return integerValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }

        try {
            String text = String.valueOf(value).trim();

            if (text.isEmpty()) {
                return null;
            }

            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer invokeIntegerGetter(
            Object target,
            String methodName
    ) {
        try {
            Object value = target.getClass()
                    .getMethod(methodName)
                    .invoke(target);

            return asInteger(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void addNoCacheHeaders(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
    }
}