package com.bepa.eis.common.providers.security;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Date;

public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager INSTANCE = new SessionManager();
    private final SessionProvider sessions;
    private final UserProvider users;

    public SessionManager() { this(new SessionProvider(null), new UserProvider(null)); }

    SessionManager(SessionProvider sessions, UserProvider users) {
        this.sessions = sessions;
        this.users = users;
    }

    public static SessionManager getInstance() { return INSTANCE; }

    public void login(String sessionId, Integer userId, String ipAddress, String userAgent,
                      GeoIpService.GeoIpResult geoIpResult) {
        if (!SessionIdGenerator.isValid(sessionId) || userId == null || userId <= 0) {
            throw new IllegalArgumentException("A unique session id and user id are required");
        }
        GeoIpService.GeoIpResult geo = geoIpResult != null ? geoIpResult : GeoIpService.GeoIpResult.unknown();
        WebSession ws = new WebSession();
        ws.setSessionId(sessionId);
        ws.setUserId(userId);
        ws.setEmail(users.getEmailByUserId(userId));
        ws.setIpAddress(ipAddress);
        ws.setUserAgent(userAgent);
        ws.setCountryCode(geo.countryCode());
        ws.setCountryName(geo.countryName());
        ws.setRegionName(geo.regionName());
        ws.setCity(geo.city());
        ws.setLatitude(geo.latitude());
        ws.setLongitude(geo.longitude());
        ws.setLoginAt(new Date());
        ws.setLastAccessed(new Date());
        try {
            // Creating one login must never delete another login for the same user.
            sessions.createSession(ws);
            audit(ws, "SESSION_CREATED", "Login session was created", "OK");
        } catch (SQLException e) {
            audit(ws, "SESSION_CREATE_FAILED", "Login session could not be created", "Warning");
            throw new IllegalStateException("Unable to create session", e);
        }
    }

    public void logout(String sessionId) { end(sessionId, "LOGOUT", "LOGOUT", "User logged out", "OK"); }
    public void expire(String sessionId) { end(sessionId, "EXPIRED", "SESSION_EXPIRED", "Session expired", "Warning"); }

    private void end(String sessionId, String reason, String eventType, String description, String status) {
        if (!SessionIdGenerator.isValid(sessionId)) return;
        WebSession ws = null;
        try {
            SessionProvider provider = sessions;
            ws = provider.getBySessionId(sessionId);
            provider.endSession(sessionId, reason);
            provider.deleteBySessionId(sessionId);
            if (ws != null) audit(ws, eventType, description, status);
        } catch (SQLException e) {
            audit(ws, eventType + "_FAILED", "Session could not be ended", "Warning");
            throw new IllegalStateException("Unable to end session", e);
        }
    }

    protected void audit(WebSession ws, String eventType, String description, String status) {
        try {
            new AuditEventProvider(ws).logLoginEvent(ws == null ? "unknown" : ws.getEmail(), eventType, description, status);
        } catch (Exception e) {
            log.warn("Could not write session audit event. eventType={}", eventType, e);
        }
    }
}
