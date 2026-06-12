package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.DTO.CustomerProject;
import com.bepa.eis.server.dataprovider.misc.CustomerProjectProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Endpoint: GET /api/customersprojects
 * Returns customers and their projects as XML.
 *
 * Note: This implementation returns static XML as an example.
 * Replace it with database-backed logic when ready.
 */
@WebServlet(name = "CustomersProjectsServlet", urlPatterns = "/api/customersprojects")
public class CustomersProjectsServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(CustomersProjectsServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        setXmlResponse(resp);

        String xml;
        try {
            CustomerProjectProvider customerProjectProvider = new CustomerProjectProvider(null);
            CustomerProject customerProject = customerProjectProvider.getCustomerProject( getSessionId(req) );
            xml = toXmlString(customerProject.toXmlDocument());

        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Error loading customer and projects: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        resp.getWriter().write(xml);
    }

}