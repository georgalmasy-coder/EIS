package com.bepa.eis.server.api.security;

import com.bepa.eis.common.enums.mail.MailTemplateType;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.mail.MailTemplateRenderer;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Standalone endpoint tests using fake users: no database writes or real emails. */
public final class PasswordResetFlowTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("eis-password-reset-test-");
        Path config = directory.resolve("eis-global.properties");
        Files.writeString(config, "customer.workflow.portal.base.url=https://unused-config.example.test\n"
                + "mail.template.folder=" + directory.toString().replace('\\', '/') + "\n");
        System.setProperty("eis.config.file", config.toString());

        FakeUsers users = new FakeUsers();
        PasswordResetRequestServlet requestServlet = new PasswordResetRequestServlet() {
            @Override protected UserProvider createUserProvider() { return users; }
        };
        Exchange invalid = new Exchange("{\"email\":\"invalid\"}");
        requestServlet.doPost(invalid.request(), invalid.response());
        check(invalid.status == 400 && users.sent == 0, "Invalid email does not create mail");
        Exchange known = new Exchange("{\"email\":\" person@example.test \"}");
        requestServlet.doPost(known.request(), known.response());
        check(known.status == 202 && users.sent == 1, "Known active user receives a reset request");
        check("person@example.test".equals(users.email), "Email is normalized");
        check("https://portal.example.test/eis".equals(users.baseUrl), "Link uses the request URL instead of configuration");

        users.known = false;
        Exchange unknown = new Exchange("{\"email\":\"unknown@example.test\"}");
        requestServlet.doPost(unknown.request(), unknown.response());
        check(unknown.status == known.status && unknown.body.toString().equals(known.body.toString()),
                "Unknown and known accounts have the same public response");
        check(users.sent == 1, "Unknown accounts do not create mail");
        users.known = true;
        users.active = false;
        Exchange inactive = new Exchange("{\"email\":\"person@example.test\"}");
        requestServlet.doPost(inactive.request(), inactive.response());
        check(users.sent == 1 && inactive.status == 202, "Inactive accounts do not create mail");

        PasswordResetServlet resetServlet = new PasswordResetServlet() {
            @Override protected UserProvider createUserProvider() { return users; }
        };
        int resets = users.resets;
        for (String password : new String[]{"short", "        ", "x".repeat(129)}) {
            Exchange exchange = new Exchange(resetXml(password, password));
            resetServlet.doPost(exchange.request(), exchange.response());
            check(exchange.status == 400 && users.resets == resets, "Invalid passwords do not update users");
        }
        Exchange mismatch = new Exchange(resetXml("New-password-123", "Different-password-123"));
        resetServlet.doPost(mismatch.request(), mismatch.response());
        check(mismatch.status == 400 && users.resets == resets, "Confirmation must match exactly");
        String password = "  Æøå<&-password  ";
        Exchange reset = new Exchange(resetXml(password, password));
        resetServlet.doPost(reset.request(), reset.response());
        check(reset.status == 200 && users.resets == resets + 1, "Valid password reaches reset provider");
        check(password.equals(users.password), "Passwords preserve Unicode, XML characters and edge spaces");
        check("test-token".equals(users.token), "Email token is passed to the reset provider");
        check(reset.sessions == 0, "Reset never creates an authenticated session");
        users.resetAllowed = false;
        Exchange used = new Exchange(resetXml(password, password));
        resetServlet.doPost(used.request(), used.response());
        check(used.status == 400 && used.body.toString().contains("<success>false</success>"),
                "Expired or already-used tokens cannot produce a success response");

        MailTemplateRenderer renderer = new MailTemplateRenderer();
        String link = "https://portal.example.test/eis/enter-new-password.html?token=test-token";
        var template = renderer.render(MailTemplateType.PASSWORD_RESET, Map.of("resetLink", link));
        check(template.getSourceFileName().startsWith("classpath:"), "WAR includes a default mail template");
        check(template.getBody().contains(link) && !template.getBody().contains("{{resetLink}}"),
                "Default mail contains the reset link");
        Files.writeString(directory.resolve("password-reset.mail"),
                "Subject: Custom reset\nContent-Type: text/plain; charset=UTF-8\n\nOpen {{resetLink}}\n");
        template = renderer.render(MailTemplateType.PASSWORD_RESET, Map.of("resetLink", link));
        check("Custom reset".equals(template.getSubject()), "Existing external template overrides the default");
        Files.delete(directory.resolve("password-reset.mail"));
        Files.delete(config);
        Files.delete(directory);
        System.out.println("Password reset endpoint and mail template regression tests passed.");
    }

    private static String resetXml(String password, String confirmation) {
        return "<passwordReset><token>test-token</token><newPassword>" + escapeXml(password)
                + "</newPassword><confirmPassword>" + escapeXml(confirmation) + "</confirmPassword></passwordReset>";
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeUsers extends UserProvider {
        boolean known = true;
        boolean active = true;
        boolean resetAllowed = true;
        int sent;
        int resets;
        String email;
        String baseUrl;
        String password;
        String token;

        FakeUsers() { super(null); }
        @Override public Integer getUserIdByEmail(String email) {
            this.email = email;
            return known ? 1 : null;
        }
        @Override public UserAdministrationRow getUserAdministrationRow(Integer userId) {
            return new UserAdministrationRow(userId, "PP", "Person", email, null, null, active,
                    UserRoles.PROJECT_MEMBER, null, null, false, false, null, "DEFAULT", false,
                    null, null, null, null, null, null, null);
        }
        @Override public boolean sendPasswordResetLink(Integer userId, Integer createdByUserId, String baseUrl) {
            sent++;
            this.baseUrl = baseUrl;
            return true;
        }
        @Override public boolean completePasswordReset(String token, String password) {
            resets++;
            this.token = token;
            this.password = password;
            return resetAllowed;
        }
    }

    private static final class Exchange {
        final String input;
        final StringWriter body = new StringWriter();
        int status;
        int sessions;

        Exchange(String input) { this.input = input; }
        HttpServletRequest request() {
            ByteArrayInputStream bytes = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            ServletInputStream stream = new ServletInputStream() {
                @Override public int read() { return bytes.read(); }
                @Override public boolean isFinished() { return bytes.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
            return proxy(HttpServletRequest.class, (method, args) -> switch (method) {
                case "getInputStream" -> stream;
                case "getScheme" -> "https";
                case "getServerName" -> "portal.example.test";
                case "getServerPort" -> 443;
                case "getContextPath" -> "/eis";
                case "getSession" -> { sessions++; yield null; }
                default -> null;
            });
        }
        HttpServletResponse response() {
            return proxy(HttpServletResponse.class, (method, args) -> switch (method) {
                case "setStatus" -> { status = (Integer) args[0]; yield null; }
                case "getWriter" -> new PrintWriter(body);
                default -> null;
            });
        }
    }

    private interface Invocation { Object invoke(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args)));
    }
}
