package com.bepa.eis.server.api.security;

import com.bepa.eis.common.providers.UserProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "PasswordResetServlet", urlPatterns = {
        "/api/security/password-reset"
})
public class PasswordResetServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetServlet.class);

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        String token = safeText(request.getParameter("token"));
        UserProvider userProvider = createUserProvider();
        boolean valid = userProvider.validatePasswordResetToken(token);

        writeXml(response, HttpServletResponse.SC_OK, buildValidationXml(valid));
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            Document document = factory
                    .newDocumentBuilder()
                    .parse(request.getInputStream());

            Element root = document.getDocumentElement();
            String token = text(root, "token");
            String newPassword = passwordText(root, "newPassword");
            String confirmPassword = passwordText(root, "confirmPassword");

            if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
                writeXml(response, HttpServletResponse.SC_BAD_REQUEST, buildResultXml(false, "Password must be at least 8 characters."));
                return;
            }

            if (newPassword.length() > 128) {
                writeXml(response, HttpServletResponse.SC_BAD_REQUEST, buildResultXml(false, "Password must be no more than 128 characters."));
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                writeXml(response, HttpServletResponse.SC_BAD_REQUEST, buildResultXml(false, "Passwords do not match."));
                return;
            }

            UserProvider userProvider = createUserProvider();
            boolean success = userProvider.completePasswordReset(token, newPassword);

            writeXml(
                    response,
                    success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST,
                    buildResultXml(success, success ? "Password updated." : "Password reset failed.")
            );
        } catch (Exception e) {
            log.warn("Password reset request failed.", e);
            writeXml(response, HttpServletResponse.SC_BAD_REQUEST, buildResultXml(false, "Password reset failed. Please request a new link or try again."));
        }
    }

    private String buildValidationXml(boolean valid) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><passwordReset><valid>" + valid + "</valid></passwordReset>";
    }

    private String buildResultXml(boolean success, String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><passwordResetResult><success>" + success + "</success><message>" + escapeXml(message) + "</message></passwordResetResult>";
    }

    private String text(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return "";
        }

        Element child = (Element) parent.getElementsByTagName(tagName).item(0);
        if (child == null) {
            return "";
        }

        return child.getTextContent() == null ? "" : child.getTextContent().trim();
    }

    protected UserProvider createUserProvider() {
        return new UserProvider(null);
    }

    // Passwords must retain leading/trailing spaces, just as in login.
    private String passwordText(Element parent, String tagName) {
        Element child = (Element) parent.getElementsByTagName(tagName).item(0);
        return child == null || child.getTextContent() == null ? "" : child.getTextContent();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void writeXml(
            HttpServletResponse response,
            int status,
            String xml
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(xml == null ? "" : xml);
    }
}
