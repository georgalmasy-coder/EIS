package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.scheduling.CleanupJobSettings;
import com.bepa.eis.common.scheduling.CronSchedule;
import com.bepa.eis.common.scheduling.LogCleanupSettings;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Shared, persisted observations from the web and integration server. */
public class ComponentHealthProvider extends GenericProvider {
    public static final String MAIL_JOB = "job.mail";
    public static final String WORKFLOW_JOB = "job.customer-workflow";
    public static final String SMTP = "integration.smtp";
    public static final String CVR = "integration.cvr";
    public static final String VIES = "integration.vies";
    public static final String GEO_IP = "integration.geo-ip";

    public ComponentHealthProvider() { super(null); }

    public static void record(String component, boolean success) {
        record(component, success ? "OK" : "Critical");
    }

    public static void record(String component, String status) {
        try {
            new AuditEventProvider(null).logEvent(new AuditEventProvider.AuditEvent(
                    "system", "COMPONENT_HEALTH", "COMPONENT", component,
                    "Execution observation", status));
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(ComponentHealthProvider.class).warn("Could not record component health: {}", component, e);
        }
    }

    public List<Component> load() throws SQLException {
        List<Definition> definitions = new ArrayList<>();
        List<Component> result = new ArrayList<>();
        try {
            CleanupJobSettings logs = LogCleanupSettings.load().job();
            if (logs.enabled()) definitions.add(new Definition(LogCleanupSettings.COMPONENT_ID, LogCleanupSettings.LABEL,
                    true, 0, logs.cron(), logs.zone()));
        } catch (RuntimeException e) {
            result.add(new Component(LogCleanupSettings.COMPONENT_ID, LogCleanupSettings.LABEL, "Critical", null,
                    "Invalid log cleanup configuration"));
        }
        for (CleanupJobSettings.Type type : CleanupJobSettings.Type.values()) {
            try {
                CleanupJobSettings settings = CleanupJobSettings.load(type);
                if (settings.enabled()) definitions.add(new Definition(type.componentId(), type.label(), true,
                        0, settings.cron(), settings.zone()));
            } catch (RuntimeException e) {
                result.add(new Component(type.componentId(), type.label(), "Critical", null, "Invalid cleanup job configuration"));
            }
        }
        addScheduledJob(definitions, result, MAIL_JOB, "Mail queue job", GlobalConfiguration::getMailQueueJobSchedule);
        addScheduledJob(definitions, result, WORKFLOW_JOB, "Customer workflow job", GlobalConfiguration::getCustomerWorkflowJobSchedule);
        if (GlobalConfiguration.isMailQueueJobEnabled()) definitions.add(new Definition(SMTP, "SMTP email", false, 86400));
        definitions.add(new Definition(CVR, "CVR lookup", false, 86400));
        if (GlobalConfiguration.isViesValidationEnabled()) definitions.add(new Definition(VIES, "VIES VAT validation", false, 86400));
        definitions.add(new Definition(GEO_IP, "Geo-IP lookup", false, 86400));
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT TOP (1) EventTime, Status, GETDATE() AS ObservedNow FROM AUDIT_EVENT
                     WHERE EventType = 'COMPONENT_HEALTH' AND EntityType = 'COMPONENT' AND EntityId = ?
                     ORDER BY EventTime DESC, CASE WHEN Status = 'Critical' THEN 0 ELSE 1 END
                     """)) {
            for (Definition definition : definitions) {
                statement.setString(1, definition.id());
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        result.add(new Component(definition.id(), definition.label(), "Unknown", null, "No execution recorded"));
                    } else {
                        LocalDateTime time = rows.getTimestamp("EventTime").toLocalDateTime();
                        LocalDateTime now = rows.getTimestamp("ObservedNow").toLocalDateTime();
                        result.add(evaluate(definition, rows.getString("Status"), time, now));
                    }
                }
            }
        }
        return result;
    }

    public static Component evaluate(Definition definition, String status, LocalDateTime time, LocalDateTime now) {
        return evaluate(definition, status, time, now, Instant.now());
    }

    private static void addScheduledJob(List<Definition> definitions, List<Component> result, String id, String label,
            java.util.function.Supplier<com.bepa.eis.common.scheduling.CronJobSettings> supplier) {
        try {
            var settings = supplier.get();
            if (settings.enabled()) definitions.add(new Definition(id, label, true, 0, settings.cron(), settings.zone()));
        } catch (RuntimeException e) {
            result.add(new Component(id, label, "Critical", null, "Invalid cron job configuration"));
        }
    }

    public static Component evaluate(Definition definition, String status, LocalDateTime time, LocalDateTime now, Instant current) {
        if (time == null) return new Component(definition.id(), definition.label(), "Unknown", null, "No execution recorded");
        long ageSeconds = Duration.between(time, now).getSeconds();
        boolean stale;
        if (definition.cron() != null) {
            // Derive age using database timestamps, then apply the configured cron timezone.
            Instant observed = current.minusSeconds(ageSeconds);
            stale = current.isAfter(new CronSchedule(definition.cron(), definition.zone()).nextAfter(observed).plusSeconds(300));
        } else {
            stale = ageSeconds > definition.maxAgeSeconds();
        }
        String currentStatus = stale ? (definition.job() ? "Critical" : "Unknown") : status;
        return new Component(definition.id(), definition.label(), currentStatus, time.toString(),
                stale ? (definition.job() ? "Job has missed its expected interval" : "No observation in the last 24 hours")
                        : "Warning".equals(status) ? "Job completed with workflow errors"
                        : "Last execution " + ("OK".equals(status) ? "succeeded" : "failed"));
    }

    /** Unknown components are excluded; coverage must be shown with the score. */
    public static Integer score(List<Component> components) {
        int measured = 0;
        int points = 0;
        for (Component component : components) {
            switch (component.status()) {
                case "OK" -> { measured++; points += 100; }
                case "Warning" -> { measured++; points += 50; }
                case "Critical" -> measured++;
                default -> { }
            }
        }
        return measured == 0 ? null : Math.round((float) points / measured);
    }

    public record Definition(String id, String label, boolean job, long maxAgeSeconds, String cron, ZoneId zone) {
        public Definition(String id, String label, boolean job, long maxAgeSeconds) {
            this(id, label, job, maxAgeSeconds, null, null);
        }
    }
    public record Component(String id, String label, String status, String lastObservedAt, String description) {}
}
