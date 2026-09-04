package com.bepa.eis.server.api.security;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.UserPreferenceProvider;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.security.GeoIpService;
import com.bepa.eis.common.providers.security.LoginActivityLogger;
import com.bepa.eis.common.providers.security.MfaConfig;
import com.bepa.eis.common.providers.security.MfaTotpService;
import com.bepa.eis.common.providers.security.SessionManager;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@WebServlet(
        name = "LoginServlet",
        urlPatterns = {
                "/api/login",
                "/api/login/mfa/verify",
                "/api/login/mfa/setup/verify"
        }
)
public class LoginServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);

    private static final String PRE_AUTH_TOKEN_SESSION_KEY = "mfaPreAuthToken";
    private static final String PRE_AUTH_EXPIRES_SESSION_KEY = "mfaPreAuthExpires";
    private static final String PRE_AUTH_USER_ID_SESSION_KEY = "mfaPreAuthUserId";
    private static final String PRE_AUTH_USERNAME_SESSION_KEY = "mfaPreAuthUsername";
    private static final String PRE_AUTH_IP_ADDRESS_SESSION_KEY = "mfaPreAuthIpAddress";
    private static final String PRE_AUTH_USER_AGENT_SESSION_KEY = "mfaPreAuthUserAgent";
    private static final String PRE_AUTH_MFA_SETUP_REQUIRED_SESSION_KEY = "mfaSetupRequired";
    private static final String PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY = "mfaVerificationAttempts";
    private static final String PRE_AUTH_MFA_SETUP_SECRET_SESSION_KEY = "mfaSetupSecret";

    private DataSource dataSource;
    private GeoIpService geoIpService;
    private MfaTotpService mfaTotpService;

    @Override
    public void init() throws ServletException {
        try {
            InitialContext context = new InitialContext();

            this.dataSource = (DataSource) context.lookup(GlobalConfiguration.getJndiName());
            this.geoIpService = new GeoIpService();
            this.mfaTotpService = new MfaTotpService();

            log.info(
                    "LoginServlet initialized. Configuration file: {}, MFA mode: {}",
                    GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                    MfaConfig.getMfaMode()
            );
        } catch (Exception e) {
            throw new ServletException("Failed to lookup DataSource: " + GlobalConfiguration.getJndiName(), e);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String servletPath = req.getServletPath();

        if ("/api/login/mfa/verify".equals(servletPath)) {
            if (MfaConfig.isMfaDisabled()) {
                clearPreAuthSession(req);

                log.info(
                        "Rejected MFA verify request because MFA is globally disabled. Configuration file: {}, MFA mode: {}",
                        GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                        MfaConfig.getMfaMode()
                );

                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            handleMfaVerify(
                    req,
                    resp,
                    false
            );
            return;
        }

        if ("/api/login/mfa/setup/verify".equals(servletPath)) {
            if (MfaConfig.isMfaDisabled()) {
                clearPreAuthSession(req);

                log.info(
                        "Rejected MFA setup verify request because MFA is globally disabled. Configuration file: {}, MFA mode: {}",
                        GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                        MfaConfig.getMfaMode()
                );

                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            handleMfaVerify(
                    req,
                    resp,
                    true
            );
            return;
        }

        handlePasswordLogin(
                req,
                resp
        );
    }

    private void handlePasswordLogin(
            HttpServletRequest req,
            HttpServletResponse response
    ) throws IOException {
        String body = new String(
                req.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        String username = extractJsonString(
                body,
                "username"
        );

        String password = extractJsonString(
                body,
                "password"
        );

        String ipAddress = getClientIpAddress(req);
        String userAgent = req.getHeader("User-Agent");
        GeoIpService.GeoIpResult geoIpResult = safeGeoLookup(ipAddress);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            logFailedLoginAttempt(
                    null,
                    username,
                    null,
                    "Missing username or password",
                    ipAddress,
                    userAgent,
                    geoIpResult,
                    20,
                    "Login request was missing username or password"
            );

            logAuditLoginEvent(
                    username,
                    "LOGIN_FAILED",
                    "Login request was missing username or password",
                    "Warning"
            );

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        UserProvider userProvider = new UserProvider(null);

        UserProvider.LoginValidationResult validationResult = userProvider.validateLogin(
                username,
                password
        );

        if (!validationResult.valid()) {
            logFailedLoginAttempt(
                    validationResult.userId(),
                    username,
                    null,
                    validationResult.failureReason(),
                    ipAddress,
                    userAgent,
                    geoIpResult,
                    40,
                    "Invalid login attempt"
            );

            logAuditLoginEvent(
                    username,
                    "LOGIN_FAILED",
                    validationResult.failureReason(),
                    "Warning"
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String message = "Account is locked".equalsIgnoreCase(validationResult.failureReason())
                    ? "User Account Locked - please contact your administrator"
                    : "Invalid username or password";
            response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
            return;
        }

        UserProvider.UserMfaState userMfaState = userProvider.getUserMfaStateByUserId(validationResult.userId());

        if (userMfaState == null) {
            log.warn("Could not load MFA state for userId: {}", validationResult.userId());

            logAuditLoginEvent(
                    username,
                    "LOGIN_FAILED",
                    "Could not load MFA state for user",
                    "Warning"
            );

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        List<CustomerRecord> customers = userProvider.getCustomersByUserId(validationResult.userId());
        MfaConfig.CustomerMfaPolicy customerMfaPolicy = resolveCustomerMfaPolicy(customers);

        boolean mfaRequired = MfaConfig.isMfaRequired(
                customerMfaPolicy,
                userMfaState.userMfaPolicy(),
                userMfaState.mfaEnabled()
        );

        log.info(
                "MFA login decision for user '{}': configFile={}, globalMode={}, customerPolicy={}, userPolicy={}, userMfaEnabled={}, userMfaConfigured={}, mfaRequired={}",
                username,
                GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                MfaConfig.getMfaMode(),
                customerMfaPolicy,
                userMfaState.userMfaPolicy(),
                userMfaState.mfaEnabled(),
                userMfaState.isMfaConfigured(),
                mfaRequired
        );

        if (mfaRequired) {
            startMfaFlow(
                    req,
                    response,
                    validationResult,
                    userMfaState,
                    username,
                    ipAddress,
                    userAgent
            );
            return;
        }

        createAuthenticatedSession(
                req,
                validationResult.userId(),
                username,
                ipAddress,
                userAgent,
                geoIpResult,
                false,
                null
        );

        logAuditLoginEvent(
                username,
                "LOGIN_SUCCESS",
                MfaConfig.isMfaDisabled()
                        ? "Login successful without MFA because MFA is globally disabled"
                        : "Login successful without MFA",
                "OK"
        );

        WebSession webSession = getSession(req);

        if (webSession != null && webSession.getUserId() != null) {
            redirectToPage(
                    response,
                    webSession,
                    username
            );
/*
            if (webSession.getCustomerId() != null) {
                log.info(
                        "User '{}' logged in with customerId: {}",
                        username,
                        webSession.getCustomerId()
                );
            } else {
                log.info("User '{}' logged in", username);
                resp.sendRedirect("/select-project.html");
                return;
            }

            if (webSession.getProjectId() == null) {
                log.info(
                        "User '{}' logged in with projectId: {}",
                        username,
                        webSession.getProjectId()
                );
                resp.sendRedirect("/web/view?page=myprojects");
            }
*/
        }

        response.setHeader(
                "Cache-Control",
                "no-store"
        );

        response.setHeader(
                "Pragma",
                "no-cache"
        );

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private MfaConfig.CustomerMfaPolicy resolveCustomerMfaPolicy(List<CustomerRecord> customers) {
        if (customers == null || customers.isEmpty()) {
            return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        }

        boolean hasOptional = false;
        boolean hasDisabled = false;

        for (CustomerRecord customer : customers) {
            if (customer == null) {
                continue;
            }

            MfaConfig.CustomerMfaPolicy customerMfaPolicy = MfaConfig.parseCustomerMfaPolicy(customer.getCustomerMfaPolicy());

            if (customerMfaPolicy == MfaConfig.CustomerMfaPolicy.REQUIRED) {
                return MfaConfig.CustomerMfaPolicy.REQUIRED;
            }

            if (customerMfaPolicy == MfaConfig.CustomerMfaPolicy.OPTIONAL) {
                hasOptional = true;
            }

            if (customerMfaPolicy == MfaConfig.CustomerMfaPolicy.DISABLED) {
                hasDisabled = true;
            }
        }

        if (hasOptional) {
            return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        }

        if (hasDisabled) {
            return MfaConfig.CustomerMfaPolicy.DISABLED;
        }

        return MfaConfig.CustomerMfaPolicy.OPTIONAL;
    }

    private void redirectToPage(
            HttpServletResponse response,
            WebSession webSession,
            String username
    ) throws IOException {
        UserProvider userProvider = new UserProvider(webSession);
        List<CustomerRecord> customers = userProvider.getCustomersByUserId(webSession.getUserId());

        try {
            if (customers.size() == 1) {
                CustomerRecord customer = customers.get(0);

                webSession.setCustomerId(customer.getCustomerId());
                restoreSelectedProject(webSession);

                response.sendRedirect(getRedirectUrl(webSession));
                return;
            }

            log.info("User '{}' logged in", username);
            response.sendRedirect("/select-project.html");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static public String getRedirectUrl(WebSession webSession) {
        User user = CustomerLookupCache.getUser(webSession, webSession.getUserId());

        if (user == null) {
            return "/index.html";
        }

        UserRoles userRole = user.getUserRole();

        return userRole == UserRoles.BEPA_SYSTEM_ADMINISTRATOR ? "/web/view?page=admindashboard" : "/web/view?page=myprojects";
    }

    public static void restoreSelectedProject(WebSession webSession) throws SQLException {
        if (webSession == null || webSession.getCustomerId() == null || webSession.getUserId() == null) {
            return;
        }

        UserPreferenceProvider preferenceProvider = new UserPreferenceProvider(webSession);
        Integer rememberedProjectId = preferenceProvider.getSelectedProjectId(webSession.getUserId());
        Integer projectId = null;

        if (rememberedProjectId != null) {
            ProjectProvider projectProvider = new ProjectProvider(webSession);
            List<ProjectRecord> projects = projectProvider.getLatestProjectsByCustomerAndUserId(
                    webSession.getCustomerId(),
                    webSession.getUserId()
            );

            boolean projectIsAvailable = projects.stream()
                    .anyMatch(project -> rememberedProjectId.equals(project.getProjectId()));

            if (projectIsAvailable) {
                projectId = rememberedProjectId;
            }
        }

        webSession.setProjectId(projectId);
        new SessionProvider(webSession).updateSessionInfo(webSession);
        if (rememberedProjectId != null && projectId == null) {
            preferenceProvider.setSelectedProjectId(webSession.getUserId(), null);
        }
    }

    private void startMfaFlow(
            HttpServletRequest req,
            HttpServletResponse resp,
            UserProvider.LoginValidationResult validationResult,
            UserProvider.UserMfaState userMfaState,
            String username,
            String ipAddress,
            String userAgent
    ) throws IOException {
        if (MfaConfig.isMfaDisabled()) {
            log.warn(
                    "MFA flow was requested for user '{}', but MFA is globally disabled. Continuing without MFA. Configuration file: {}, MFA mode: {}",
                    username,
                    GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                    MfaConfig.getMfaMode()
            );

            GeoIpService.GeoIpResult geoIpResult = safeGeoLookup(ipAddress);

            createAuthenticatedSession(
                    req,
                    validationResult.userId(),
                    username,
                    ipAddress,
                    userAgent,
                    geoIpResult,
                    false,
                    null
            );

            logAuditLoginEvent(
                    username,
                    "LOGIN_SUCCESS",
                    "Login successful without MFA because MFA is globally disabled",
                    "OK"
            );

            resp.setHeader(
                    "Cache-Control",
                    "no-store"
            );

            resp.setHeader(
                    "Pragma",
                    "no-cache"
            );

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        HttpSession oldSession = req.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = req.getSession(true);
        String mfaToken = UUID.randomUUID().toString();
        Instant expires = Instant.now().plusSeconds(MfaConfig.getPreAuthTokenValidMinutes() * 60L);

        boolean mfaSetupRequired = userMfaState.requiresMfaSetup();

        session.setAttribute(PRE_AUTH_TOKEN_SESSION_KEY, mfaToken);
        session.setAttribute(PRE_AUTH_EXPIRES_SESSION_KEY, expires);
        session.setAttribute(PRE_AUTH_USER_ID_SESSION_KEY, validationResult.userId());
        session.setAttribute(PRE_AUTH_USERNAME_SESSION_KEY, username);
        session.setAttribute(PRE_AUTH_IP_ADDRESS_SESSION_KEY, ipAddress);
        session.setAttribute(PRE_AUTH_USER_AGENT_SESSION_KEY, userAgent);
        session.setAttribute(PRE_AUTH_MFA_SETUP_REQUIRED_SESSION_KEY, mfaSetupRequired);
        session.setAttribute(PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY, 0);
        session.setMaxInactiveInterval(MfaConfig.getPreAuthTokenValidMinutes() * 60);

        resp.setHeader(
                "Cache-Control",
                "no-store"
        );

        resp.setHeader(
                "Pragma",
                "no-cache"
        );

        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(HttpServletResponse.SC_OK);

        if (mfaSetupRequired) {
            String setupSecret = mfaTotpService.generateSecret();

            String otpAuthUri = mfaTotpService.buildOtpAuthUri(
                    MfaConfig.getIssuer(),
                    username,
                    setupSecret
            );

            String qrCodeUrl = mfaTotpService.buildOtpAuthQrCodeDataUri(otpAuthUri);

            session.setAttribute(PRE_AUTH_MFA_SETUP_SECRET_SESSION_KEY, setupSecret);

            logAuditMfaEvent(
                    username,
                    "MFA_SETUP_STARTED",
                    username,
                    "MFA setup started",
                    "OK"
            );

            resp.getWriter().write("""
                    {
                      "status": "MFA_SETUP_REQUIRED",
                      "mfaToken": "%s",
                      "qrCodeUrl": "%s",
                      "manualEntryKey": "%s",
                      "otpauthUri": "%s"
                    }
                    """.formatted(
                    escapeJson(mfaToken),
                    escapeJson(qrCodeUrl),
                    escapeJson(setupSecret),
                    escapeJson(otpAuthUri)
            ));
            return;
        }

        logAuditMfaEvent(
                username,
                "MFA_REQUIRED",
                username,
                "MFA verification required",
                "OK"
        );

        resp.getWriter().write("""
                {
                  "status": "MFA_REQUIRED",
                  "mfaToken": "%s"
                }
                """.formatted(escapeJson(mfaToken)));
    }

    private void handleMfaVerify(
            HttpServletRequest req,
            HttpServletResponse resp,
            boolean setupVerification
    ) throws IOException {
        if (MfaConfig.isMfaDisabled()) {
            clearPreAuthSession(req);

            log.info(
                    "MFA verification was requested, but MFA is globally disabled. Configuration file: {}, MFA mode: {}",
                    GlobalConfiguration.getConfigurationFile().getAbsolutePath(),
                    MfaConfig.getMfaMode()
            );

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String body = new String(
                req.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        String mfaToken = extractJsonString(
                body,
                "mfaToken"
        );

        String code = extractJsonString(
                body,
                "code"
        );

        HttpSession session = req.getSession(false);

        if (session == null || !isValidPreAuthSession(session, mfaToken, setupVerification)) {
            String username = getPreAuthUsername(session);

            logAuditMfaEvent(
                    username,
                    setupVerification ? "MFA_SETUP_FAILED" : "MFA_VERIFICATION_FAILED",
                    username,
                    "Invalid or expired MFA pre-auth session",
                    "Warning"
            );

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int attempts = getMfaAttempts(session);

        Integer userId = (Integer) session.getAttribute(PRE_AUTH_USER_ID_SESSION_KEY);
        String username = (String) session.getAttribute(PRE_AUTH_USERNAME_SESSION_KEY);
        String ipAddress = (String) session.getAttribute(PRE_AUTH_IP_ADDRESS_SESSION_KEY);
        String userAgent = (String) session.getAttribute(PRE_AUTH_USER_AGENT_SESSION_KEY);

        if (attempts >= MfaConfig.getMaxVerificationAttempts()) {
            logAuditMfaEvent(
                    username,
                    setupVerification ? "MFA_SETUP_LOCKED" : "MFA_VERIFICATION_LOCKED",
                    username,
                    "Too many MFA verification attempts",
                    "Warning"
            );

            resp.setStatus(429);
            return;
        }

        UserProvider userProvider = new UserProvider(null);

        if (setupVerification) {
            String setupSecret = (String) session.getAttribute(PRE_AUTH_MFA_SETUP_SECRET_SESSION_KEY);
            boolean codeValid = mfaTotpService.verifyCode(setupSecret, code);

            if (!codeValid) {
                session.setAttribute(PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY, attempts + 1);

                logAuditMfaEvent(
                        username,
                        "MFA_SETUP_FAILED",
                        username,
                        "MFA setup code could not be verified",
                        "Warning"
                );

                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean mfaEnabled = userProvider.enableMfaAfterSetup(
                    userId,
                    setupSecret
            );

            if (!mfaEnabled) {
                log.warn("Could not enable MFA after setup for userId: {}", userId);

                logAuditMfaEvent(
                        username,
                        "MFA_SETUP_FAILED",
                        username,
                        "MFA setup code was verified, but MFA could not be enabled",
                        "Warning"
                );

                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            logAuditMfaEvent(
                    username,
                    "MFA_ENABLED",
                    username,
                    "User enabled two-factor authentication",
                    "OK"
            );
        } else {
            UserProvider.UserMfaState userMfaState = userProvider.getUserMfaStateByUserId(userId);

            if (userMfaState == null || !userMfaState.isMfaConfigured()) {
                logAuditMfaEvent(
                        username,
                        "MFA_VERIFICATION_FAILED",
                        username,
                        "MFA verification failed because MFA was not configured",
                        "Warning"
                );

                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean codeValid = mfaTotpService.verifyCode(
                    userMfaState.mfaSecretEncrypted(),
                    code
            );

            if (!codeValid) {
                session.setAttribute(PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY, attempts + 1);

                logAuditMfaEvent(
                        username,
                        "MFA_VERIFICATION_FAILED",
                        username,
                        "MFA verification code was invalid",
                        "Warning"
                );

                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean updated = userProvider.updateMfaLastVerified(userId);

            if (!updated) {
                log.warn("Could not update MFA last verified timestamp for userId: {}", userId);
            }

            logAuditMfaEvent(
                    username,
                    "MFA_VERIFICATION_SUCCESS",
                    username,
                    "MFA verification successful",
                    "OK"
            );
        }

        GeoIpService.GeoIpResult geoIpResult = safeGeoLookup(ipAddress);

        createAuthenticatedSession(
                req,
                userId,
                username,
                ipAddress,
                userAgent,
                geoIpResult,
                true,
                true
        );

        logAuditLoginEvent(
                username,
                "LOGIN_SUCCESS",
                "Login successful with MFA",
                "OK"
        );

        resp.setHeader(
                "Cache-Control",
                "no-store"
        );

        resp.setHeader(
                "Pragma",
                "no-cache"
        );

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private boolean isValidPreAuthSession(
            HttpSession session,
            String mfaToken,
            boolean setupVerification
    ) {
        if (session == null) {
            return false;
        }

        if (mfaToken == null || mfaToken.isBlank()) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(PRE_AUTH_TOKEN_SESSION_KEY);

        if (!mfaToken.equals(sessionToken)) {
            return false;
        }

        Instant expires = (Instant) session.getAttribute(PRE_AUTH_EXPIRES_SESSION_KEY);

        if (expires == null || Instant.now().isAfter(expires)) {
            return false;
        }

        Integer userId = (Integer) session.getAttribute(PRE_AUTH_USER_ID_SESSION_KEY);
        String username = (String) session.getAttribute(PRE_AUTH_USERNAME_SESSION_KEY);

        if (userId == null || username == null || username.isBlank()) {
            return false;
        }

        Boolean mfaSetupRequired = (Boolean) session.getAttribute(PRE_AUTH_MFA_SETUP_REQUIRED_SESSION_KEY);

        if (setupVerification) {
            if (!Boolean.TRUE.equals(mfaSetupRequired)) {
                return false;
            }

            String setupSecret = (String) session.getAttribute(PRE_AUTH_MFA_SETUP_SECRET_SESSION_KEY);
            return setupSecret != null && !setupSecret.isBlank();
        }

        return !Boolean.TRUE.equals(mfaSetupRequired);
    }

    private int getMfaAttempts(HttpSession session) {
        Object value = session.getAttribute(PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY);

        if (value instanceof Integer attempts) {
            return attempts;
        }

        return 0;
    }

    private String getPreAuthUsername(HttpSession session) {
        if (session == null) {
            return "unknown";
        }

        Object value = session.getAttribute(PRE_AUTH_USERNAME_SESSION_KEY);

        if (value instanceof String username && !username.isBlank()) {
            return username;
        }

        return "unknown";
    }

    private void createAuthenticatedSession(
            HttpServletRequest req,
            Integer userId,
            String username,
            String ipAddress,
            String userAgent,
            GeoIpService.GeoIpResult geoIpResult,
            boolean mfaRequired,
            Boolean mfaPassed
    ) {
        HttpSession oldSession = req.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = req.getSession(true);
        String sessionId = username;

        session.setAttribute(
                "sessionID",
                sessionId
        );

        session.setMaxInactiveInterval(GlobalConfiguration.getSessionTimeoutMinutes() * 60);

        SessionManager.getInstance().login(
                sessionId,
                userId,
                ipAddress,
                userAgent,
                geoIpResult
        );

        logSuccessfulLoginAttempt(
                userId,
                username,
                sessionId,
                ipAddress,
                userAgent,
                geoIpResult,
                mfaRequired,
                mfaPassed
        );
    }

    private void clearPreAuthSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        if (session != null) {
            session.removeAttribute(PRE_AUTH_TOKEN_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_EXPIRES_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_USER_ID_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_USERNAME_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_IP_ADDRESS_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_USER_AGENT_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_MFA_SETUP_REQUIRED_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_MFA_ATTEMPTS_SESSION_KEY);
            session.removeAttribute(PRE_AUTH_MFA_SETUP_SECRET_SESSION_KEY);
        }
    }

    private void logSuccessfulLoginAttempt(
            Integer userId,
            String email,
            String sessionId,
            String ipAddress,
            String userAgent,
            GeoIpService.GeoIpResult geoIpResult,
            boolean mfaRequired,
            Boolean mfaPassed
    ) {
        try (Connection connection = dataSource.getConnection()) {
            LoginActivityLogger logger = new LoginActivityLogger(connection);

            logger.logLoginAttempt(LoginActivityLogger.LoginAttempt.successful(
                    userId,
                    email,
                    sessionId,
                    ipAddress,
                    userAgent,
                    geoIpResult,
                    mfaRequired,
                    mfaPassed
            ));
        } catch (SQLException e) {
            log.warn("Could not log successful login attempt for user: {}", email, e);
        }
    }

    private void logFailedLoginAttempt(
            Integer userId,
            String email,
            String sessionId,
            String failureReason,
            String ipAddress,
            String userAgent,
            GeoIpService.GeoIpResult geoIpResult,
            Integer riskScore,
            String riskReason
    ) {
        try (Connection connection = dataSource.getConnection()) {
            LoginActivityLogger logger = new LoginActivityLogger(connection);

            logger.logLoginAttempt(LoginActivityLogger.LoginAttempt.failed(
                    userId,
                    email,
                    sessionId,
                    failureReason,
                    ipAddress,
                    userAgent,
                    geoIpResult,
                    riskScore,
                    riskReason
            ));
        } catch (SQLException e) {
            log.warn("Could not log failed login attempt for user: {}", email, e);
        }
    }

    private void logAuditLoginEvent(
            String actorEmail,
            String eventType,
            String description,
            String status
    ) {
        AuditEventProvider auditEventProvider = new AuditEventProvider(null);

        auditEventProvider.logLoginEvent(
                safeActor(actorEmail),
                eventType,
                description,
                status
        );
    }

    private void logAuditMfaEvent(
            String actorEmail,
            String eventType,
            String targetUserEmail,
            String description,
            String status
    ) {
        AuditEventProvider auditEventProvider = new AuditEventProvider(null);

        auditEventProvider.logMfaEvent(
                safeActor(actorEmail),
                eventType,
                safeActor(targetUserEmail),
                description,
                status
        );
    }

    private String safeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail;
    }

    private GeoIpService.GeoIpResult safeGeoLookup(String ipAddress) {
        try {
            return geoIpService.lookup(ipAddress);
        } catch (Exception e) {
            log.warn("GeoIP lookup failed for IP address: {}", ipAddress, e);
            return GeoIpService.GeoIpResult.unknown();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private static String extractJsonString(
            String json,
            String key
    ) {
        if (json == null || key == null) {
            return null;
        }

        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);

        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(
                ':',
                keyIndex
        );

        if (colonIndex < 0) {
            return null;
        }

        int firstQuoteIndex = json.indexOf(
                '"',
                colonIndex + 1
        );

        if (firstQuoteIndex < 0) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int index = firstQuoteIndex + 1; index < json.length(); index++) {
            char current = json.charAt(index);

            if (escaped) {
                value.append(current);
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return value.toString();
            }

            value.append(current);
        }

        return null;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
