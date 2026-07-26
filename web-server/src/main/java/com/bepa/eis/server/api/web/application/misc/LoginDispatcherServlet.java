package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.security.LoginServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Endpoint: GET /Menu
 * Returns XML with variable number of menu items and admin menu-editor data.
 */
@WebServlet(name = "LoginDispatcherServlet", urlPatterns = "/LoginRedirect")
public class LoginDispatcherServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginDispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            WebSession webSession = getSession(request);
            response.sendRedirect(LoginServlet.getRedirectUrl(webSession));
        } catch (Exception e) {
            log.error("Error loading menu editor data: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }

}
