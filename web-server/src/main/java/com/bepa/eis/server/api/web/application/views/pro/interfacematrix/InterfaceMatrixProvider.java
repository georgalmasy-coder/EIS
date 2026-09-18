package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceMatrixProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMatrixProvider.class);

    private static final String SELECT_LATEST_INTERFACES_SQL = """
            SELECT
                [InterfacePK],
                [CustomerId],
                [ProjectId],
                [EntityType],
                [Version],
                [Latest],
                [ChangedByUserId],
                [ChangedDateTime],
                [FromEntityId],
                [ToEntityId],
                [IrlId],
                [NextIrlMeeting],
                [ClassificationIds]
            FROM [dbo].[INTERFACES]
            WHERE [CustomerId] = ?
              AND [ProjectId] = ?
              AND [EntityType] = ?
              AND [Latest] = 1
            ORDER BY [FromEntityId], [ToEntityId], [Version]
            """;

    private static final String SELECT_LATEST_INTERFACE_COUNT_SQL = """
            SELECT COUNT(*) AS InterfaceCount
            FROM [dbo].[INTERFACES]
            WHERE [CustomerId] = ?
              AND [ProjectId] = ?
              AND [EntityType] = ?
              AND [Latest] = 1
            """;

    private static final String SELECT_LATEST_INTERFACE_SQL = """
            SELECT TOP 1
                [InterfacePK],
                [CustomerId],
                [ProjectId],
                [EntityType],
                [Version],
                [Latest],
                [ChangedByUserId],
                [ChangedDateTime],
                [FromEntityId],
                [ToEntityId],
                [IrlId],
                [NextIrlMeeting],
                [ClassificationIds]
            FROM [dbo].[INTERFACES]
            WHERE [CustomerId] = ?
              AND [ProjectId] = ?
              AND [EntityType] = ?
              AND [FromEntityId] = ?
              AND [ToEntityId] = ?
              AND [Latest] = 1
            ORDER BY [Version] DESC, [InterfacePK] DESC
            """;

    private static final String SELECT_ALL_INTERFACES_SQL = """
            SELECT
                [InterfacePK], [CustomerId], [ProjectId], [EntityType], [Version], [Latest],
                [ChangedByUserId], [ChangedDateTime], [FromEntityId], [ToEntityId], [IrlId],
                [NextIrlMeeting], [ClassificationIds]
            FROM [dbo].[INTERFACES]
            WHERE [CustomerId] = ?
              AND [ProjectId] = ?
              AND [EntityType] = ?
            ORDER BY [InterfacePK]
            """;

    private static final String SELECT_LATEST_INTERFACES_BY_FROM_SQL = """
            SELECT
                I.[InterfacePK], I.[CustomerId], I.[ProjectId], I.[EntityType], I.[Version], I.[Latest],
                I.[ChangedByUserId], I.[ChangedDateTime], I.[FromEntityId], I.[ToEntityId], I.[IrlId],
                I.[NextIrlMeeting], I.[ClassificationIds],
                REVERSE_I.[IrlId] AS ReverseIrlId,
                REVERSE_I.[ClassificationIds] AS ReverseClassificationIds,
                CODE_EE.[StringValue] AS ToEntityCode,
                NAME_EE.[StringValue] AS ToEntityName
            FROM [dbo].[INTERFACES] I
            LEFT JOIN [dbo].[INTERFACES] REVERSE_I
              ON REVERSE_I.[CustomerId] = I.[CustomerId]
             AND REVERSE_I.[ProjectId] = I.[ProjectId]
             AND REVERSE_I.[EntityType] = I.[EntityType]
             AND REVERSE_I.[FromEntityId] = I.[ToEntityId]
             AND REVERSE_I.[ToEntityId] = I.[FromEntityId]
             AND REVERSE_I.[Latest] = 1
            LEFT JOIN [dbo].[ENTITY] E
              ON E.[CustomerId] = I.[CustomerId]
             AND E.[ProjectId] = I.[ProjectId]
             AND E.[EntityType] = I.[EntityType]
             AND E.[EntityId] = I.[ToEntityId]
             AND E.[Latest] = 1
            LEFT JOIN [dbo].[ENTITY_ELEMENT] CODE_EE
              ON CODE_EE.[CustomerId] = E.[CustomerId]
             AND CODE_EE.[ProjectId] = E.[ProjectId]
             AND CODE_EE.[EntityType] = E.[EntityType]
             AND CODE_EE.[EntityId] = E.[EntityId]
             AND CODE_EE.[Version] = E.[Version]
             AND CODE_EE.[EntityDataElementType] = ?
            LEFT JOIN [dbo].[ENTITY_ELEMENT] NAME_EE
              ON NAME_EE.[CustomerId] = E.[CustomerId]
             AND NAME_EE.[ProjectId] = E.[ProjectId]
             AND NAME_EE.[EntityType] = E.[EntityType]
             AND NAME_EE.[EntityId] = E.[EntityId]
             AND NAME_EE.[Version] = E.[Version]
             AND NAME_EE.[EntityDataElementType] = ?
            WHERE I.[CustomerId] = ?
              AND I.[ProjectId] = ?
              AND I.[EntityType] = ?
              AND I.[FromEntityId] = ?
              AND I.[Latest] = 1
            ORDER BY I.[ToEntityId], I.[Version]
            """;

    private static final String SELECT_SYSTEM_TRL_BY_PROJECT_SQL = """
            SELECT
                E.[EntityId],
                TRL_EE.[IntegerValue] AS TrlId
            FROM [dbo].[ENTITY] E
            LEFT JOIN [dbo].[ENTITY_ELEMENT] TRL_EE
              ON TRL_EE.[CustomerId] = E.[CustomerId]
             AND TRL_EE.[ProjectId] = E.[ProjectId]
             AND TRL_EE.[EntityType] = E.[EntityType]
             AND TRL_EE.[EntityId] = E.[EntityId]
             AND TRL_EE.[Version] = E.[Version]
             AND TRL_EE.[EntityDataElementType] = ?
            WHERE E.[CustomerId] = ?
              AND E.[ProjectId] = ?
              AND E.[EntityType] = ?
              AND E.[Latest] = 1
            """;

    private static final String UPDATE_LATEST_FALSE_SQL = """
            UPDATE [dbo].[INTERFACES]
            SET [Latest] = 0
            WHERE [CustomerId] = ?
              AND [ProjectId] = ?
              AND [EntityType] = ?
              AND [FromEntityId] = ?
              AND [ToEntityId] = ?
              AND [Latest] = 1
            """;

    private static final String INSERT_INTERFACE_SQL = """
            INSERT INTO [dbo].[INTERFACES] (
                [CustomerId],
                [ProjectId],
                [EntityType],
                [Version],
                [Latest],
                [ChangedByUserId],
                [ChangedDateTime],
                [FromEntityId],
                [ToEntityId],
                [IrlId],
                [NextIrlMeeting],
                [ClassificationIds]
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final EntityType entityType;

    public InterfaceMatrixProvider(WebSession webSession, EntityType entityType) {
        super(webSession);
        if (entityType == null) {
            throw new IllegalArgumentException("EntityType is required.");
        }
        this.entityType = entityType;
    }

    public List<InterfaceRecord> getLatestInterfaceRecords(EntityType entityType) throws SQLException {
        validateSession();
        return getLatestInterfaceRecords(getWebSession().getCustomerId(), getWebSession().getProjectId(), entityType);
    }

    public int getLatestInterfaceRecordCount(EntityType entityType) throws SQLException {
        validateSession();
        return getLatestInterfaceRecordCount(getWebSession().getCustomerId(), getWebSession().getProjectId(), entityType);
    }

    public List<InterfaceRecord> getAllInterfaceRecords(EntityType entityType) throws SQLException {
        validateSession();
        return getLatestInterfaceRecords(getWebSession().getCustomerId(), getWebSession().getProjectId(), entityType);
    }

    public List<EditInterfaceRecord> getLatestInterfaceRecordsByFrom(Integer fromEntityId, EntityType entityType) throws SQLException {
        validateSession();

        if (fromEntityId == null || entityType == null) {
            throw new IllegalArgumentException("FromEntityId and EntityType are required.");
        }

        List<EditInterfaceRecord> records = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_INTERFACES_BY_FROM_SQL)) {
            setInt(ps, entityType.getEntityCodeColumn().getId(), 1);
            setInt(ps, entityType.getEntityNameColumn().getId(), 2);
            setInt(ps, getWebSession().getCustomerId(), 3);
            setInt(ps, getWebSession().getProjectId(), 4);
            setInt(ps, entityType.getId(), 5);
            setInt(ps, fromEntityId, 6);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterfaceRecord interfaceRecord = mapRecord(rs);
                    records.add(new EditInterfaceRecord(
                            interfaceRecord.fromEntityId(),
                            interfaceRecord.toEntityId(),
                            rs.getString("ToEntityCode"),
                            rs.getString("ToEntityName"),
                            interfaceRecord.irlId(),
                            getNullableInteger(rs, "ReverseIrlId"),
                            interfaceRecord.nextIrlMeeting(),
                            interfaceRecord.classificationIds(),
                            rs.getString("ReverseClassificationIds"),
                            null,
                            null
                    ));
                }
            }

            if (entityType == EntityType.SYSTEMS_BREAKDOWN && !records.isEmpty()) {
                records = enrichWithSystemTrlIds(connection, records);
            }
        }

        return records;
    }

    private List<EditInterfaceRecord> enrichWithSystemTrlIds(
            Connection connection,
            List<EditInterfaceRecord> records
    ) throws SQLException {
        Map<Integer, Integer> trlIdsByEntityId = getSystemTrlIdsByEntityId(connection);
        List<EditInterfaceRecord> enrichedRecords = new ArrayList<>();

        for (EditInterfaceRecord record : records) {
            enrichedRecords.add(new EditInterfaceRecord(
                    record.fromEntityId(),
                    record.toEntityId(),
                    record.toEntityCode(),
                    record.toEntityName(),
                    record.irlId(),
                    record.reverseIrlId(),
                    record.nextIrlMeeting(),
                    record.classificationIds(),
                    record.reverseClassificationIds(),
                    trlIdsByEntityId.get(record.fromEntityId()),
                    trlIdsByEntityId.get(record.toEntityId())
            ));
        }

        return enrichedRecords;
    }

    private Map<Integer, Integer> getSystemTrlIdsByEntityId(Connection connection) throws SQLException {
        Map<Integer, Integer> trlIdsByEntityId = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_SYSTEM_TRL_BY_PROJECT_SQL)) {
            setInt(ps, EntityDataElement.TRLID.getId(), 1);
            setInt(ps, getWebSession().getCustomerId(), 2);
            setInt(ps, getWebSession().getProjectId(), 3);
            setInt(ps, EntityType.SYSTEMS_BREAKDOWN.getId(), 4);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer trlId = getNullableInteger(rs, "TrlId");

                    if (trlId != null) {
                        trlIdsByEntityId.put(rs.getInt("EntityId"), trlId);
                    }
                }
            }
        }

        return trlIdsByEntityId;
    }

    public List<InterfaceRecord> getLatestInterfaceRecords(Integer customerId, Integer projectId, EntityType entityType) throws SQLException {
        if (customerId == null || projectId == null) {
            throw new IllegalArgumentException("CustomerId and ProjectId are required.");
        }

        List<InterfaceRecord> records = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_INTERFACES_SQL)) {
            setInt(ps, customerId, 1);
            setInt(ps, projectId, 2);
            setInt(ps, entityType.getId(), 3);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRecord(rs));
                }
            }
        }

        return records;
    }

    public int getLatestInterfaceRecordCount(Integer customerId, Integer projectId, EntityType entityType) throws SQLException {
        if (customerId == null || projectId == null || entityType == null) {
            throw new IllegalArgumentException("CustomerId, ProjectId and EntityType are required.");
        }

        try (Connection connection = getDataSource().getConnection();
            PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_INTERFACE_COUNT_SQL)) {
            setInt(ps, customerId, 1);
            setInt(ps, projectId, 2);
            setInt(ps, entityType.getId(), 3);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("InterfaceCount");
                }
            }
        }

        return 0;
    }

    public InterfaceRecord getLatestInterfaceRecord(Integer fromEntityId, Integer toEntityId, EntityType entityType) throws SQLException {
        validateSession();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_INTERFACE_SQL)) {
            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, fromEntityId, 4);
            setInt(ps, toEntityId, 5);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }

        return null;
    }

    public InterfaceRecord saveInterfaceRecord(InterfaceSaveRecord saveRecord) throws SQLException {
        validateSession();

        if (saveRecord == null) {
            throw new IllegalArgumentException("Interface save record is required.");
        }

        if (saveRecord.fromEntityId() == null || saveRecord.toEntityId() == null) {
            throw new IllegalArgumentException("FromEntityId and ToEntityId are required.");
        }

        if (saveRecord.irlId() == null) {
            throw new IllegalArgumentException("IrlId is required.");
        }

        Connection connection = getDataSource().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            InterfaceRecord latest = getLatestInterfaceRecord(connection, saveRecord.fromEntityId(), saveRecord.toEntityId());
            int nextVersion = latest == null ? 1 : latest.version() + 1;
            Timestamp changedDateTime = Timestamp.valueOf(LocalDateTime.now());

            if (latest != null) {
                markLatestAsHistorical(connection, saveRecord.fromEntityId(), saveRecord.toEntityId());
            }

            insertInterfaceRecord(
                    connection,
                    new InterfaceRecord(
                            null,
                            getWebSession().getCustomerId(),
                            getWebSession().getProjectId(),
                            entityType.getId(),
                            nextVersion,
                            true,
                            getWebSession().getUserId(),
                            changedDateTime,
                            saveRecord.fromEntityId(),
                            saveRecord.toEntityId(),
                            saveRecord.irlId(),
                            saveRecord.nextIrlMeeting(),
                            saveRecord.classificationIds()
                    )
            );

            connection.commit();

            InterfaceRecord saved = getLatestInterfaceRecord(connection, saveRecord.fromEntityId(), saveRecord.toEntityId());
            if (saved == null) {
                throw new SQLException("Could not load saved interface record.");
            }

            return saved;
        } catch (SQLException | RuntimeException ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                log.warn("Rollback failed while saving interface record", rollbackError);
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {
                // ignore
            }

            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    public boolean removeInterfaceRecord(Integer fromEntityId, Integer toEntityId) throws SQLException {
        validateSession();

        if (fromEntityId == null || toEntityId == null) {
            throw new IllegalArgumentException("FromEntityId and ToEntityId are required.");
        }

        try (Connection connection = getDataSource().getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                int updated;
                try (PreparedStatement ps = connection.prepareStatement(UPDATE_LATEST_FALSE_SQL)) {
                    setInt(ps, getWebSession().getCustomerId(), 1);
                    setInt(ps, getWebSession().getProjectId(), 2);
                    setInt(ps, entityType.getId(), 3);
                    setInt(ps, fromEntityId, 4);
                    setInt(ps, toEntityId, 5);
                    updated = ps.executeUpdate();
                }
                connection.commit();
                return updated > 0;
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    log.warn("Rollback failed while removing interface record", rollbackError);
                }
                throw ex;
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                    // ignore
                }
            }
        }
    }

    private InterfaceRecord getLatestInterfaceRecord(Connection connection, Integer fromEntityId, Integer toEntityId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_INTERFACE_SQL)) {
            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, fromEntityId, 4);
            setInt(ps, toEntityId, 5);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }

        return null;
    }

    private void markLatestAsHistorical(Connection connection, Integer fromEntityId, Integer toEntityId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_LATEST_FALSE_SQL)) {
            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);
            setInt(ps, fromEntityId, 4);
            setInt(ps, toEntityId, 5);
            ps.executeUpdate();
        }
    }

    private void insertInterfaceRecord(Connection connection, InterfaceRecord record) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_INTERFACE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;

            setInt(ps, record.customerId(), index++);
            setInt(ps, record.projectId(), index++);
            setInt(ps, record.entityType(), index++);
            setInt(ps, record.version(), index++);
            ps.setBoolean(index++, record.latest());
            setInt(ps, record.changedByUserId(), index++);
            ps.setTimestamp(index++, record.changedDateTime());
            setInt(ps, record.fromEntityId(), index++);
            setInt(ps, record.toEntityId(), index++);
            setInt(ps, record.irlId(), index++);
            setLocalDateOrNull(ps, record.nextIrlMeeting(), index++);
            setString(ps, record.classificationIds(), index);

            ps.executeUpdate();
        }
    }

    public void copyInterfacesForMovedEntities(Connection connection,
                                               Map<Integer, Integer> entityIdMapping) throws SQLException {
        if (entityIdMapping == null || entityIdMapping.isEmpty()) {
            return;
        }

        List<InterfaceRecord> recordsToCopy = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_INTERFACES_SQL)) {
            setInt(ps, getWebSession().getCustomerId(), 1);
            setInt(ps, getWebSession().getProjectId(), 2);
            setInt(ps, entityType.getId(), 3);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterfaceRecord record = mapRecord(rs);
                    if (entityIdMapping.containsKey(record.fromEntityId())
                            || entityIdMapping.containsKey(record.toEntityId())) {
                        recordsToCopy.add(record);
                    }
                }
            }
        }

        for (InterfaceRecord source : recordsToCopy) {
            insertInterfaceRecord(connection, new InterfaceRecord(
                    null,
                    source.customerId(),
                    source.projectId(),
                    source.entityType(),
                    source.version(),
                    source.latest(),
                    source.changedByUserId(),
                    source.changedDateTime(),
                    entityIdMapping.getOrDefault(source.fromEntityId(), source.fromEntityId()),
                    entityIdMapping.getOrDefault(source.toEntityId(), source.toEntityId()),
                    source.irlId(),
                    source.nextIrlMeeting(),
                    source.classificationIds()
            ));
        }
    }

    private void setLocalDateOrNull(PreparedStatement ps, String value, int index) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, java.sql.Types.TIMESTAMP);
            return;
        }

        LocalDate date = LocalDate.parse(value.trim());
        ps.setTimestamp(index, Timestamp.valueOf(date.atStartOfDay()));
    }

    private InterfaceRecord mapRecord(ResultSet rs) throws SQLException {
        Timestamp nextIrlMeetingTimestamp = rs.getTimestamp("NextIrlMeeting");

        return new InterfaceRecord(
                rs.getInt("InterfacePK"),
                rs.getInt("CustomerId"),
                rs.getInt("ProjectId"),
                rs.getInt("EntityType"),
                rs.getInt("Version"),
                rs.getBoolean("Latest"),
                rs.getInt("ChangedByUserId"),
                rs.getTimestamp("ChangedDateTime"),
                rs.getInt("FromEntityId"),
                rs.getInt("ToEntityId"),
                rs.getInt("IrlId"),
                nextIrlMeetingTimestamp != null ? nextIrlMeetingTimestamp.toLocalDateTime().toLocalDate().toString() : "",
                rs.getString("ClassificationIds")
        );
    }

    private void validateSession() {
        if (getWebSession() == null || getWebSession().getCustomerId() == null || getWebSession().getProjectId() == null) {
            throw new IllegalStateException("Web session is missing customer or project context.");
        }
    }

    public record InterfaceSaveRecord(
            Integer fromEntityId,
            Integer toEntityId,
            Integer irlId,
            String nextIrlMeeting,
            String classificationIds
    ) {
    }

    public record InterfaceRecord(
            Integer interfacePk,
            Integer customerId,
            Integer projectId,
            Integer entityType,
            Integer version,
            Boolean latest,
            Integer changedByUserId,
            Timestamp changedDateTime,
            Integer fromEntityId,
            Integer toEntityId,
            Integer irlId,
            String nextIrlMeeting,
            String classificationIds
    ) {
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    public record EditInterfaceRecord(
            Integer fromEntityId,
            Integer toEntityId,
            String toEntityCode,
            String toEntityName,
            Integer irlId,
            Integer reverseIrlId,
            String nextIrlMeeting,
            String classificationIds,
            String reverseClassificationIds,
            Integer fromTrlId,
            Integer toTrlId
    ) {
    }
}
