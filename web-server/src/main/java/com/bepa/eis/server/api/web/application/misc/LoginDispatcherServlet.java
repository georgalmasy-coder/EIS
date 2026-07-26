package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider.MenuRow;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Endpoint: GET /Menu
 * Returns XML with variable number of menu items and admin menu-editor data.
 */
@WebServlet(name = "LoginDispatcherServlet", urlPatterns = "/LoginDispatcher")
public class LoginDispatcherServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginDispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            WebSession webSession = getSession(request);

            User user = CustomerLookupCache.getUser(webSession, webSession.getUserId());

            if (user == null) {
                response.sendRedirect("/index.html");
                return;
            }

            UserRoles userRole = user.getUserRole();

            if (user.getUserRole() == UserRoles.BEPA_SYSTEM_ADMINISTRATOR) {
                response.sendRedirect("/web/view?page=admindashboard");
            } else {
                response.sendRedirect("/web/view?page=myprojects");
            }
        } catch (Exception e) {
            log.error("Error loading menu editor data: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }

}
