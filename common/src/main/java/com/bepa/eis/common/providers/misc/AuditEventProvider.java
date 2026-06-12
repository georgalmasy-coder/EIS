package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditEventProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProvider.class);

    private static final String INSERT_AUDIT_EVENT_SQL = """
            INSERT INTO [dbo].[AUDIT_EVENT] (
                EventTime,
                ActorEmail,
                EventType,
                EntityType,
                EntityId,
                Description,
                Status
            )
            VALUES (
                GETDATE(),
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """;

    public AuditEventProvider(WebSession webSession) {
        super(webSession);
    }

    public boolean logEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_AUDIT_EVENT_SQL)) {

            statement.setString(1, safeText(auditEvent.actorEmail(), "system"));
            statement.setString(2, safeText(auditEvent.eventType(), "UNKNOWN"));
            statement.setString(3, safeText(auditEvent.entityType(), "SYSTEM"));
            statement.setString(4, safeText(auditEvent.entityId(), "unknown"));
            statement.setString(5, safeText(auditEvent.description(), auditEvent.eventType()));
            statement.setString(6, safeText(auditEvent.status(), "OK"));

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn(
                    "Could not insert audit event. eventType={}, entityType={}, entityId={}",
                    auditEvent.eventType(),
                    auditEvent.entityType(),
                    auditEvent.entityId(),
                    e
            );
            return false;
        }
    }

    public boolean logSystemEvent(
            String eventType,
            String description,
            String status
    ) {
        return logEvent(new AuditEvent(
                "system",
                eventType,
                "SYSTEM",
                eventType,
                description,
                status
        ));
    }

    public boolean logUserEvent(
            String actorEmail,
            String eventType,
            String targetUserEmail,
            String description,
            String status
    ) {
        return logEvent(new AuditEvent(
                actorEmail,
                eventType,
                "USER",
                targetUserEmail,
                description,
                status
        ));
    }

    public boolean logMfaEvent(
            String actorEmail,
            String eventType,
            String targetUserEmail,
            String description,
            String status
    ) {
        return logEvent(new AuditEvent(
                actorEmail,
                eventType,
                "USER",
                targetUserEmail,
                description,
                status
        ));
    }

    public boolean logLoginEvent(
            String actorEmail,
            String eventType,
            String description,
            String status
    ) {
        return logEvent(new AuditEvent(
                actorEmail,
                eventType,
                "LOGIN",
                actorEmail,
                description,
                status
        ));
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public record AuditEvent(
            String actorEmail,
            String eventType,
            String entityType,
            String entityId,
            String description,
            String status
    ) {
    }
}