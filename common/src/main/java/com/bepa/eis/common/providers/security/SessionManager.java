package com.bepa.eis.common.providers.security;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.SessionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;

public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static SessionManager instance = null;

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }

        return instance;
    }

    public void login(String sessionId) {
        login(
                sessionId,
                null,
                null,
                null,
                null
        );
    }

    public void login(
            String sessionId,
            Integer userId,
            String ipAddress,
            String userAgent,
            GeoIpService.GeoIpResult geoIpResult
    ) {
        logout(sessionId);

        GeoIpService.GeoIpResult safeGeoIpResult = geoIpResult != null
                ? geoIpResult
                : GeoIpService.GeoIpResult.unknown();

        WebSession webSession = new WebSession();
        webSession.setSessionId(sessionId);
        webSession.setUserId(userId);
        webSession.setIpAddress(ipAddress);
        webSession.setUserAgent(userAgent);
        webSession.setCountryCode(safeGeoIpResult.countryCode());
        webSession.setCountryName(safeGeoIpResult.countryName());
        webSession.setRegionName(safeGeoIpResult.regionName());
        webSession.setCity(safeGeoIpResult.city());
        webSession.setLatitude(safeGeoIpResult.latitude());
        webSession.setLongitude(safeGeoIpResult.longitude());
        webSession.setLoginAt(new Date());
        webSession.setLastAccessed(new Date());

        try {
            SessionProvider sessionProvider = new SessionProvider(null);
            sessionProvider.upsertSession(webSession);

            logAuditLoginEvent(
                    sessionId,
                    "SESSION_CREATED",
                    "Login session was created",
                    "OK"
            );
        } catch (SQLException e) {
            log.error("Error inserting session: {}", e.getMessage(), e);

            logAuditLoginEvent(
                    sessionId,
                    "SESSION_CREATE_FAILED",
                    "Login session could not be created",
                    "Warning"
            );

            throw new RuntimeException(e);
        }
    }

    public void logout(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            SessionProvider sessionProvider = new SessionProvider(null);
            sessionProvider.endSession(sessionId, "LOGOUT");
            sessionProvider.deleteBySessionId(sessionId);

            logAuditLoginEvent(
                    sessionId,
                    "LOGOUT",
                    "User logged out",
                    "OK"
            );
        } catch (SQLException e) {
            log.error("Error deleting session: {}", e.getMessage(), e);

            logAuditLoginEvent(
                    sessionId,
                    "LOGOUT_FAILED",
                    "User logout failed",
                    "Warning"
            );

            throw new RuntimeException(e);
        }
    }

    public void expire(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            SessionProvider sessionProvider = new SessionProvider(null);
            sessionProvider.endSession(sessionId, "EXPIRED");
            sessionProvider.deleteBySessionId(sessionId);

            logAuditLoginEvent(
                    sessionId,
                    "SESSION_EXPIRED",
                    "Session expired",
                    "Warning"
            );
        } catch (SQLException e) {
            log.error("Error expiring session: {}", e.getMessage(), e);

            logAuditLoginEvent(
                    sessionId,
                    "SESSION_EXPIRE_FAILED",
                    "Session expiration failed",
                    "Warning"
            );

            throw new RuntimeException(e);
        }
    }

    private void logAuditLoginEvent(
            String actorEmail,
            String eventType,
            String description,
            String status
    ) {
        try {
            AuditEventProvider auditEventProvider = new AuditEventProvider(null);
            auditEventProvider.logLoginEvent(
                    safeActor(actorEmail),
                    eventType,
                    description,
                    status
            );
        } catch (Exception e) {
            log.warn("Could not write session audit event. eventType={}, actor={}", eventType, actorEmail, e);
        }
    }

    private String safeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail;
    }
}