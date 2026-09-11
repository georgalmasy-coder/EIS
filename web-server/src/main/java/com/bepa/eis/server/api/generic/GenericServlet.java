package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.misc.IncidentProvider;
import com.bepa.eis.common.providers.misc.PerformanceProvider;
import com.bepa.eis.server.api.web.application.enums.theme.Theme;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.sql.SQLException;

public class GenericServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GenericServlet.class);

    private WebSession webSession;

    public WebSession getSession(HttpServletRequest request) {
        WebSession ws;
        if (GlobalConfiguration.isUdvMode()) {
            ws = new WebSession();
            ws.setId(1);
            ws.setSessionId("georg.almasy@gmail.com");
            ws.setCustomerId(1);
            ws.setProjectId(1);
            ws.setUserId(1);
            ws.setThemeId(Theme.fromId(String.valueOf(GlobalConfiguration.getThemeId())).getCssId());
            return ws;
        } else {
            try {
                SessionProvider sessionProvider = new SessionProvider(null);
                ws = sessionProvider.getBySessionId(getSessionId(request));

                HttpSession httpSession = request.getSession(false);
                if (httpSession != null && ws != null && ws.getThemeId() != null) {
                    httpSession.setAttribute("eis.theme.id", ws.getThemeId());
                }

            } catch (SQLException e) {

                log.info("Error getting session for page viewer: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return ws;
    }

    public String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return (String) session.getAttribute("sessionID");
    }

    public void setXmlResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=UTF-8");

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    /**
     * Convenience method that serializes into an XML string.
     *
     * @param prettyPrint if true, indents the output (human-readable)
     */
    public String toXmlString(Document doc, boolean prettyPrint) throws ParserConfigurationException, TransformerException {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Common Xalan property (works in the usual JDK transformer)
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        String xmlAsString = writer.toString();
        log.debug("Created XML : {}", xmlAsString);

        return xmlAsString;
    }

    /**
     * Defaults to pretty-printed XML.
     */
    public String toXmlString(Document doc) throws ParserConfigurationException, TransformerException {
        return toXmlString(doc, true);
    }

    public WebSession getWebSessionFromRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException {
        if (GlobalConfiguration.isUdvMode()) {
            try {
                return getWebSession(null);
            } catch (SQLException impossible) {
                throw new ServletException(impossible);
            }
        }

        String sessionId = getSessionIdFromRequest(request);
        if (sessionId == null || sessionId.isBlank()) {
            redirectToSessionExpired(request, response);
            return null;
        }

        try {
            WebSession resolvedSession = getWebSession(sessionId);
            if (resolvedSession == null) {
                redirectToSessionExpired(request, response);
                return null;
            }

            HttpSession httpSession = request.getSession(false);
            if (httpSession != null && resolvedSession.getThemeId() != null) {
                httpSession.setAttribute("eis.theme.id", resolvedSession.getThemeId());
            }
            return resolvedSession;
        } catch (SQLException e) {
            log.warn("Session could not be read; redirecting to session-expired page. sessionId={}", sessionId, e);
            redirectToSessionExpired(request, response);
            return null;
        }
    }

    public String getSessionIdFromRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("sessionID");
    }

    private void redirectToSessionExpired(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException {
        try {
            response.sendRedirect(request.getContextPath() + "/session-expired.html");
        } catch (IOException e) {
            throw new ServletException("Unable to redirect to the session-expired page", e);
        }
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void setWebSession(WebSession webSession) {
        this.webSession = webSession;
    }

    public WebSession getWebSession(String sessionId) throws SQLException {
        WebSession ws;
        if (GlobalConfiguration.isUdvMode()) {
            ws = new WebSession();
            ws.setId(1);
            ws.setSessionId("georg.almasy@mail.com");
            ws.setCustomerId(GlobalConfiguration.getDefaultCustomerId());
            ws.setProjectId(GlobalConfiguration.getDefaultProjectId());
            ws.setUserId(1);
            return ws;
        } else {
            SessionProvider sessionProvider = new SessionProvider(null);
            ws = sessionProvider.getBySessionId(sessionId);
        }
        return ws;
    }

    public String getCommandParameter(HttpServletRequest request) {
        String command = request.getParameter("cmd");
        return command != null ? command.trim().toLowerCase() : "";
    }

    public String getModule(HttpServletRequest request) {
        return request.getServletPath() +  "." + getCommandParameter(request);
    }

    public IncidentProvider getIncidentProvider() {
        return new IncidentProvider(webSession);
    }

    public PerformanceProvider getPerformanceProvider () {
        return new PerformanceProvider(getWebSession());
    }

}
