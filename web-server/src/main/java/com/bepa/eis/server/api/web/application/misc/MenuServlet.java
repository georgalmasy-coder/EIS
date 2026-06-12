package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.dataprovider.misc.MenuProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Endpoint: GET /Menu
 * Returns XML with variable number of menu items.
 */
@WebServlet(name = "MenuServlet", urlPatterns = "/Menu")
public class MenuServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(MenuServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        setXmlResponse(resp);

        String xml;
        try {
            MenuProvider menuProvider = new MenuProvider(null);
            Menu menu = menuProvider.getMenuItems( getSessionId(req) );
            xml = toXmlString(menu.toXmlDocument());

        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Error loading menu: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        resp.getWriter().write(xml);
   }

}