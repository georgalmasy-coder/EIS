package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
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
import java.util.List;

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

    private static final String SELECT_NEXT_INTERFACE_PK_SQL = """
            SELECT ISNULL(MAX([InterfacePK]), 0) + 1 AS NextInterfacePK
            FROM [dbo].[INTERFACES] WITH (UPDLOCK, HOLDLOCK)
            """;

    private static final String INSERT_INTERFACE_SQL = """
            INSERT INTO [dbo].[INTERFACES] (
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
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final EntityType entityType;

    public InterfaceMatrixProvider(WebSession webSession, EntityType entityType) {
        super(webSession);
        if (entityType == null) {
            throw new IllegalArgumentException("EntityType is required.");
        }
        this.entityType = entityType;
    }

    public List<InterfaceRecord> getLatestInterfaceRecords() throws SQLException {
        validateSession();
        return getLatestInterfaceRecords(getWebSession().getCustomerId(), getWebSession().getProjectId());
    }

    public List<InterfaceRecord> getAllInterfaceRecords() throws SQLException {
        validateSession();
        return getLatestInterfaceRecords(getWebSession().getCustomerId(), getWebSession().getProjectId());
    }

    public List<InterfaceRecord> getLatestInterfaceRecords(Integer customerId, Integer projectId) throws SQLException {
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

    public InterfaceRecord getLatestInterfaceRecord(Integer fromEntityId, Integer toEntityId) throws SQLException {
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
            int interfacePk = getNextInterfacePk(connection);
            Timestamp changedDateTime = Timestamp.valueOf(LocalDateTime.now());

            if (latest != null) {
                markLatestAsHistorical(connection, saveRecord.fromEntityId(), saveRecord.toEntityId());
            }

            insertInterfaceRecord(
                    connection,
                    new InterfaceRecord(
                            interfacePk,
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

    private int getNextInterfacePk(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_NEXT_INTERFACE_PK_SQL);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("NextInterfacePK");
            }
        }

        throw new SQLException("Could not determine next InterfacePK.");
    }

    private void insertInterfaceRecord(Connection connection, InterfaceRecord record) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_INTERFACE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;

            setInt(ps, record.interfacePk(), index++);
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
}
