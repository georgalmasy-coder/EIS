package com.bepa.eis.server.api.security;

import com.bepa.eis.common.providers.UserProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@WebServlet(name = "PasswordResetRequestServlet", urlPatterns = "/api/security/password-reset-request")
public class PasswordResetRequestServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestServlet.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String ACCEPTED_MESSAGE =
            "If an active account exists for this email address, you will receive a password reset link shortly. Check your inbox and spam folder.";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JsonNode body;
        try {
            body = mapper.readTree(request.getInputStream());
        } catch (IOException e) {
            writeResult(response, HttpServletResponse.SC_BAD_REQUEST, false, "Enter a valid email address.");
            return;
        }
        String email = body == null ? "" : body.path("email").asText("").trim();
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            writeResult(response, HttpServletResponse.SC_BAD_REQUEST, false, "Enter a valid email address.");
            return;
        }
        String baseUrl;
        try {
            baseUrl = RequestBaseUrl.from(request);
        } catch (IllegalArgumentException e) {
            log.error("Could not determine the public URL for the password reset request.", e);
            writeResult(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, false,
                    "Password reset is temporarily unavailable. Please try again later.");
            return;
        }

        UserProvider users = createUserProvider();
        Integer userId = users.getUserIdByEmail(email);
        if (userId != null) {
            UserProvider.UserAdministrationRow user = users.getUserAdministrationRow(userId);
            if (user != null && user.active() && !users.sendPasswordResetLink(userId, null, baseUrl)) {
                log.error("Could not queue a requested password reset email. userId={}", userId);
            }
        }
        // The public response does not reveal whether the account exists.
        writeResult(response, HttpServletResponse.SC_ACCEPTED, true, ACCEPTED_MESSAGE);
    }

    private void writeResult(HttpServletResponse response, int status, boolean success, String message) throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), Map.of("success", success, "message", message));
    }

    protected UserProvider createUserProvider() {
        return new UserProvider(null);
    }
}
