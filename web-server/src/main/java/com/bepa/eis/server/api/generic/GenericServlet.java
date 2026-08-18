package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.server.api.web.application.enums.theme.Theme;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            } catch (SQLException e) {

                log.error("Error getting session for page viewer: {}", e.getMessage(), e);
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
        log.info("Created XML : {}", xmlAsString);

        return xmlAsString;
    }

    /**
     * Defaults to pretty-printed XML.
     */
    public String toXmlString(Document doc) throws ParserConfigurationException, TransformerException {
        return toXmlString(doc, true);
    }

}
