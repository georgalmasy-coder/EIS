package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.EventType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(EventProvider.class);

    private static final String INSERT_EVENT_SQL =
            "INSERT INTO EVENT " +
                    " (EventType, EventCreated, EventCreatedBy, EventDescription, CustomerId, ProjectId, EntityType, EntityId) " +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String GET_RECENT_EVENTS_SQL = """
            SELECT TOP 20
                E.EventCreated,
                E.EventType,
                E.EventDescription,
                ISNULL(P.ProjectName, '') AS ProjectName,
                E.EventProcessed
            FROM EVENT E
            LEFT JOIN PROJECT P
                ON E.ProjectId = P.ProjectId
               AND E.CustomerId = P.CustomerId
            ORDER BY E.EventCreated DESC
            """;

    public EventProvider(WebSession webSession) {
        super(webSession);
    }

    public void createEntityChangeEvent(EntityType entityType, Integer entityId, String eventDescription) {
        createEvent(EventType.ENTITY_MODIFIED_EVENT, entityType, entityId, eventDescription);
    }

    public List<RecentEvent> getRecentEvent() throws SQLException {
        List<RecentEvent> recentEvents = new ArrayList<>();

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_RECENT_EVENTS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp eventCreated = rs.getTimestamp("EventCreated");
                int eventTypeId = rs.getInt("EventType");
                EventType eventType = EventType.valueOf(eventTypeId);
                String eventDescription = rs.getString("EventDescription");
                String projectName = rs.getString("ProjectName");
                Timestamp eventProcessed = rs.getTimestamp("EventProcessed");

                recentEvents.add(new RecentEvent(
                        toIsoString(eventCreated),
                        eventType != null ? eventType.getDescription() : "Unknown",
                        eventDescription,
                        projectName != null ? projectName : "",
                        eventProcessed != null ? "Ok" : "Awating"
                ));
            }
        }

        return recentEvents;
    }

    private void createEvent(EventType eventType,
                             EntityType entityType,
                             Integer entityId,
                             String eventDescription) {

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(INSERT_EVENT_SQL)) {

                ps.setInt(1, eventType.getId());
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setInt(3, getWebSession().getUserId());
                ps.setString(4, eventDescription);
                ps.setInt(5, getWebSession().getCustomerId());
                ps.setInt(6, getWebSession().getProjectId());
                ps.setInt(7, entityType.getId());
                ps.setInt(8, entityId);

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    throw new SQLException("Insert event failed : " + eventType.getDescription() + " " + entityType.getDescription() + " " + eventDescription);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create event", e);
        }
    }

    private String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }

    public record RecentEvent(
            String time,
            String type,
            String description,
            String project,
            String status
    ) {
    }
}