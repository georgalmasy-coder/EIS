package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.enums.PageType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "FrontendPerformanceServlet", urlPatterns = "/api/performance/frontend")
public class FrontendPerformanceServlet extends GenericServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSession session = getWebSessionFromRequest(request, response);
        if (session == null) return;

        PageType page = PageType.mapToType(request.getParameter("page"));
        if (page == PageType.NONE || "admindashboard".equalsIgnoreCase(page.getPageName())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        long duration;
        try {
            duration = Long.parseLong(request.getParameter("durationMs"));
            if (duration < 0 || duration > Integer.MAX_VALUE) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // All identity columns come from the authenticated session, never the client.
        new PerformanceProvider(session).logPerformance("frontend." + page.getPageName(), duration);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
