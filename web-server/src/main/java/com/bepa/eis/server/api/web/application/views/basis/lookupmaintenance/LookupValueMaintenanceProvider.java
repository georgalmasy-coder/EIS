package com.bepa.eis.server.api.web.application.views.basis.lookupmaintenance;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.project.ClassificationType;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LookupValueMaintenanceProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(LookupValueMaintenanceProvider.class);

    private static final String SELECT_TRL_ROWS_SQL =
            "SELECT TRLId, CustomerId, ProjectId, TRLLevel, TRLName, TRLDescription, Active, Color " +
            "FROM dbo.TRL " +
            "WHERE CustomerId = ? AND ProjectId = ? " +
            "ORDER BY TRLLevel";

    private static final String SELECT_IRL_ROWS_SQL =
            "SELECT IRLPK, CustomerId, ProjectId, IrlId, IRLCode, IRLLevel, IRLName, IRLDescription, Active, Color " +
            "FROM dbo.IRL " +
            "WHERE CustomerId = ? AND ProjectId = ? " +
            "ORDER BY " + irlSortExpression();


    private static final String SELECT_SRL_ROWS_SQL =
            "SELECT SrlId, CustomerId, ProjectId, SRLLevel, SRLName, SRLDescription, Active, Color " +
            "FROM dbo.SRL " +
            "WHERE CustomerId = ? AND ProjectId = ? " +
            "ORDER BY SRLLevel";

    private static final String SELECT_CLASSIFICATION_ROWS_SQL =
            "SELECT ClassificationId, CustomerId, ProjectId, ClassId, Code, Description, Example, UsageExample, Active " +
            "FROM dbo.CLASSIFICATION " +
            "WHERE CustomerId = ? AND ProjectId = ? " +
            "ORDER BY Code";

    private static final String UPDATE_TRL_SQL =
            "UPDATE dbo.TRL " +
            "SET TRLName = ?, TRLDescription = ?, Active = ?, Color = ? " +
            "WHERE TRLId = ? AND CustomerId = ? AND ProjectId = ?";

    private static final String UPDATE_IRL_SQL =
            "UPDATE dbo.IRL " +
            "SET IRLName = ?, IRLDescription = ?, Active = ?, Color = ? " +
            "WHERE IRLPK = ? AND CustomerId = ? AND ProjectId = ?";

    private static final String UPDATE_SRL_SQL =
            "UPDATE dbo.SRL " +
            "SET SRLName = ?, SRLDescription = ?, Active = ?, Color = ? " +
            "WHERE SrlId = ? AND CustomerId = ? AND ProjectId = ?";

    private static final String DEACTIVATE_TRL_SQL =
            "UPDATE dbo.TRL " +
            "SET Active = 0 " +
            "WHERE CustomerId = ? AND ProjectId = ? AND TRLLevel > ?";

    private static final String DEACTIVATE_SRL_SQL =
            "UPDATE dbo.SRL " +
            "SET Active = 0 " +
            "WHERE CustomerId = ? AND ProjectId = ? AND SRLLevel > ?";

    private static final String DEACTIVATE_IRL_SQL =
            "UPDATE dbo.IRL " +
            "SET Active = 0 " +
            "WHERE CustomerId = ? AND ProjectId = ? AND " + irlSortExpression("IRLCode") + " > ?";

    private static final String INSERT_CLASSIFICATION_SQL =
            "INSERT INTO dbo.CLASSIFICATION (" +
                    "CustomerId, " +
                    "ProjectId, " +
                    "ClassId, " +
                    "Code, " +
                    "Description, " +
                    "Example, " +
                    "UsageExample, " +
                    "Active" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_CLASSIFICATION_SQL =
            "UPDATE dbo.CLASSIFICATION " +
            "SET Code = ?, Description = ?, Example = ?, UsageExample = ?, Active = ? " +
            "WHERE ClassificationId = ? AND CustomerId = ? AND ProjectId = ?";

    public LookupValueMaintenanceProvider(WebSession webSession) {
        super(webSession);
    }

    public LookupMaintenanceData getLookupMaintenanceData() {
        Integer customerId = requireCustomerId();
        Integer projectId = requireProjectId();

        return new LookupMaintenanceData(
                getTrlRows(customerId, projectId),
                getIrlRows(customerId, projectId),
                getSrlRows(customerId, projectId),
                getClassificationRows(customerId, projectId)
        );
    }

    public List<LookupRow> getTrlRows(Integer customerId, Integer projectId) {
        return loadRows(customerId, projectId, SELECT_TRL_ROWS_SQL, this::mapTrlRow);
    }

    public List<LookupRow> getIrlRows(Integer customerId, Integer projectId) {
        return loadRows(customerId, projectId, SELECT_IRL_ROWS_SQL, this::mapIrlRow);
    }

    public List<LookupRow> getSrlRows(Integer customerId, Integer projectId) {
        return loadRows(customerId, projectId, SELECT_SRL_ROWS_SQL, this::mapSrlRow);
    }

    public List<ClassificationRow> getClassificationRows(Integer customerId, Integer projectId) {
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureClassificationRows(connection, customerId, projectId);
                List<ClassificationRow> rows = loadClassificationRows(connection, customerId, projectId);
                connection.commit();
                return rows;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error loading classification rows", e);
            throw new RuntimeException(e);
        }
    }

    public void saveLookupRow(LookupRow lookupRow) {
        if (lookupRow == null) {
            throw new IllegalArgumentException("Lookup row is required.");
        }

        Integer customerId = requireCustomerId();
        Integer projectId = requireProjectId();
        LookupType lookupType = lookupRow.lookupType();

        if (lookupType == null) {
            throw new IllegalArgumentException("LookupType is required.");
        }

        validateRow(lookupRow);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                switch (lookupType) {
                    case TRL -> saveTrlRow(connection, customerId, projectId, lookupRow);
                    case IRL -> saveIrlRow(connection, customerId, projectId, lookupRow);
                    case SRL -> saveSrlRow(connection, customerId, projectId, lookupRow);
                    case CLASSIFICATION -> throw new IllegalArgumentException("Classification rows must be saved through saveClassificationRow.");
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            EhcacheProvider.clearCacheEntry(customerId);
        } catch (SQLException e) {
            log.error("Error saving lookup row {}", lookupRow, e);
            throw new RuntimeException(e);
        }
    }

    private void saveTrlRow(Connection connection, Integer customerId, Integer projectId, LookupRow lookupRow) throws SQLException {
        if (lookupRow.rowId() == null) {
            throw new IllegalArgumentException("TRL row id is required.");
        }

        updateRow(connection, UPDATE_TRL_SQL, lookupRow, customerId, projectId);

        if (!lookupRow.active()) {
            deactivateFollowingTrlRows(connection, customerId, projectId, lookupRow.level());
        }
    }

    private void saveIrlRow(Connection connection, Integer customerId, Integer projectId, LookupRow lookupRow) throws SQLException {
        if (lookupRow.rowId() == null) {
            throw new IllegalArgumentException("IRL row id is required.");
        }

        updateRow(connection, UPDATE_IRL_SQL, lookupRow, customerId, projectId);

        if (!lookupRow.active()) {
            deactivateFollowingIrlRows(connection, customerId, projectId, resolveIrlOrder(lookupRow.code()));
        }
    }

    private void saveSrlRow(Connection connection, Integer customerId, Integer projectId, LookupRow lookupRow) throws SQLException {
        if (lookupRow.rowId() == null) {
            throw new IllegalArgumentException("SRL row id is required.");
        }

        updateRow(connection, UPDATE_SRL_SQL, lookupRow, customerId, projectId);

        if (!lookupRow.active()) {
            deactivateFollowingSrlRows(connection, customerId, projectId, lookupRow.level());
        }
    }

    public void saveClassificationRow(ClassificationRow classificationRow) {
        if (classificationRow == null) {
            throw new IllegalArgumentException("Classification row is required.");
        }

        Integer customerId = requireCustomerId();
        Integer projectId = requireProjectId();
        validateClassificationRow(classificationRow);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureClassificationCodeIsUnique(connection, customerId, projectId, classificationRow);
                if (classificationRow.classificationId() == null) {
                    insertClassificationRow(connection, customerId, projectId, classificationRow);
                } else {
                    updateClassificationRow(connection, customerId, projectId, classificationRow);
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            EhcacheProvider.clearCacheEntry(customerId);
        } catch (SQLException e) {
            log.error("Error saving classification row {}", classificationRow, e);
            throw new RuntimeException(e);
        }
    }

    private void ensureClassificationCodeIsUnique(
            Connection connection,
            Integer customerId,
            Integer projectId,
            ClassificationRow row
    ) throws SQLException {
        String sql = "SELECT COUNT(1) FROM dbo.CLASSIFICATION WITH (UPDLOCK, HOLDLOCK) " +
                "WHERE CustomerId = ? AND ProjectId = ? AND Code = ? " +
                "AND (? IS NULL OR ClassificationId <> ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);
            setString(statement, normalizeClassificationCode(row.code()), 3);

            if (row.classificationId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                setInt(statement, row.classificationId(), 4);
                setInt(statement, row.classificationId(), 5);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    throw new IllegalArgumentException("A classification with code '" + normalizeClassificationCode(row.code()) +
                            "' already exists for the current project.");
                }
            }
        }
    }

    private void updateClassificationRow(
            Connection connection,
            Integer customerId,
            Integer projectId,
            ClassificationRow classificationRow
    ) throws SQLException {
        if (classificationRow.classificationId() == null) {
            throw new IllegalArgumentException("Classification row id is required.");
        }

        String usageExample = isClassificationGroupCode(classificationRow.code())
                ? normalizeNullableText(classificationRow.usageExample())
                : null;

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CLASSIFICATION_SQL)) {
            setString(statement, normalizeClassificationCode(classificationRow.code()), 1);
            setString(statement, safeText(classificationRow.description()), 2);
            setString(statement, normalizeNullableText(classificationRow.example()), 3);
            setString(statement, usageExample, 4);
            setBoolean(statement, classificationRow.active(), 5);
            setInt(statement, classificationRow.classificationId(), 6);
            setInt(statement, customerId, 7);
            setInt(statement, projectId, 8);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Classification row was not found for current customer and project.");
            }
        }
    }

    private void insertClassificationRow(Connection connection, Integer customerId, Integer projectId, ClassificationRow row) throws SQLException {
        int classId;
        String nextIdSql = "SELECT COALESCE(MAX(ClassId), 0) + 1 FROM dbo.CLASSIFICATION WITH (UPDLOCK, HOLDLOCK) WHERE CustomerId = ? AND ProjectId = ?";
        try (PreparedStatement statement = connection.prepareStatement(nextIdSql)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Could not determine the next ClassId.");
                classId = resultSet.getInt(1);
            }
        }

        String usageExample = isClassificationGroupCode(row.code())
                ? normalizeNullableText(row.usageExample())
                : null;

        try (PreparedStatement statement = connection.prepareStatement(INSERT_CLASSIFICATION_SQL)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);
            setInt(statement, classId, 3);
            setString(statement, normalizeClassificationCode(row.code()), 4);
            setString(statement, safeText(row.description()), 5);
            setString(statement, normalizeNullableText(row.example()), 6);
            setString(statement, usageExample, 7);
            setBoolean(statement, row.active(), 8);
            statement.executeUpdate();
        }
    }

    private boolean isClassificationGroupCode(String code) {
        return safeText(code).matches("(?i)^[a-z]_?$");
    }

    private void ensureClassificationRows(Connection connection, Integer customerId, Integer projectId) throws SQLException {
        if (classificationRowsExist(connection, customerId, projectId)) {
            return;
        }

        insertDefaultClassificationRows(connection, customerId, projectId);
        EhcacheProvider.clearCacheEntry(customerId);
    }

    private boolean classificationRowsExist(Connection connection, Integer customerId, Integer projectId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM dbo.CLASSIFICATION WHERE CustomerId = ? AND ProjectId = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private void insertDefaultClassificationRows(Connection connection, Integer customerId, Integer projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CLASSIFICATION_SQL)) {
            for (ClassificationType classificationType : ClassificationType.values()) {
                if (classificationType == ClassificationType.INVALID_CLASS) {
                    continue;
                }

                setInt(statement, customerId, 1);
                setInt(statement, projectId, 2);
                statement.setInt(3, classificationType.getClassId());
                statement.setString(4, classificationType.getCode());
                statement.setString(5, classificationType.getCodeDescription());
                statement.setString(6, normalizeNullableText(classificationType.getCodeExample()));
                statement.setString(7, normalizeNullableText(classificationType.getCodeUsageExample()));
                statement.setBoolean(8, classificationType.isActive());
                statement.executeUpdate();
            }
        }
    }

    private void updateRow(
            Connection connection,
            String sql,
            LookupRow lookupRow,
            Integer customerId,
            Integer projectId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setString(statement, safeText(lookupRow.name()), 1);
            setString(statement, safeText(lookupRow.description()), 2);
            setBoolean(statement, lookupRow.active(), 3);
            setString(statement, normalizeColor(lookupRow.color()), 4);
            setInt(statement, lookupRow.rowId(), 5);
            setInt(statement, customerId, 6);
            setInt(statement, projectId, 7);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Lookup row was not found for current customer and project.");
            }
        }
    }

    private void deactivateFollowingTrlRows(Connection connection, Integer customerId, Integer projectId, Integer level) throws SQLException {
        deactivateFollowingRows(connection, DEACTIVATE_TRL_SQL, customerId, projectId, level);
    }

    private void deactivateFollowingSrlRows(Connection connection, Integer customerId, Integer projectId, Integer level) throws SQLException {
        deactivateFollowingRows(connection, DEACTIVATE_SRL_SQL, customerId, projectId, level);
    }

    private void deactivateFollowingIrlRows(Connection connection, Integer customerId, Integer projectId, Integer order) throws SQLException {
        deactivateFollowingRows(connection, DEACTIVATE_IRL_SQL, customerId, projectId, order);
    }

    private void deactivateFollowingRows(Connection connection, String sql, Integer customerId, Integer projectId, Integer order) throws SQLException {
        if (order == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);
            setInt(statement, order, 3);
            statement.executeUpdate();
        }
    }

    private List<LookupRow> loadRows(
            Integer customerId,
            Integer projectId,
            String sql,
            RowMapper mapper
    ) {
        List<LookupRow> rows = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading lookup rows", e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    private List<ClassificationRow> loadClassificationRows(Connection connection, Integer customerId, Integer projectId) throws SQLException {
        List<ClassificationRow> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLASSIFICATION_ROWS_SQL)) {
            setInt(statement, customerId, 1);
            setInt(statement, projectId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapClassificationRow(resultSet));
                }
            }
        }

        return rows;
    }

    private LookupRow mapTrlRow(ResultSet resultSet) throws SQLException {
        return new LookupRow(
                LookupType.TRL,
                resultSet.getInt("TRLId"),
                resultSet.getInt("TRLLevel"),
                "",
                resultSet.getString("TRLName"),
                resultSet.getString("TRLDescription"),
                resultSet.getBoolean("Active"),
                resultSet.getString("Color")
        );
    }

    private LookupRow mapIrlRow(ResultSet resultSet) throws SQLException {
        return new LookupRow(
                LookupType.IRL,
                resultSet.getInt("IRLPK"),
                resultSet.getInt("IRLLevel"),
                resultSet.getString("IRLCode"),
                resultSet.getString("IRLName"),
                resultSet.getString("IRLDescription"),
                resultSet.getBoolean("Active"),
                resultSet.getString("Color")
        );
    }

    private LookupRow mapSrlRow(ResultSet resultSet) throws SQLException {
        return new LookupRow(
                LookupType.SRL,
                resultSet.getInt("SrlId"),
                resultSet.getInt("SRLLevel"),
                "",
                resultSet.getString("SRLName"),
                resultSet.getString("SRLDescription"),
                resultSet.getBoolean("Active"),
                resultSet.getString("Color")
        );
    }

    private ClassificationRow mapClassificationRow(ResultSet resultSet) throws SQLException {
        return new ClassificationRow(
                resultSet.getInt("ClassificationId"),
                resultSet.getInt("ClassId"),
                resultSet.getString("Code"),
                resultSet.getString("Description"),
                resultSet.getString("Example"),
                resultSet.getString("UsageExample"),
                resultSet.getBoolean("Active")
        );
    }

    private void validateRow(LookupRow lookupRow) {
        String name = safeText(lookupRow.name());
        String description = safeText(lookupRow.description());
        String color = normalizeColor(lookupRow.color());

        if (lookupRow.rowId() == null) {
            throw new IllegalArgumentException("LookupId is required.");
        }

        if (lookupRow.level() == null) {
            throw new IllegalArgumentException("LookupLevel is required.");
        }

        if (lookupRow.lookupType() == LookupType.IRL && safeText(lookupRow.code()).isBlank()) {
            throw new IllegalArgumentException("IRLCode is required.");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }

        switch (lookupRow.lookupType()) {
            case TRL -> {
                if (name.length() > 75) {
                    throw new IllegalArgumentException("TRL name must be maximum 75 characters.");
                }
                if (description.length() > 4000) {
                    throw new IllegalArgumentException("TRL description must be maximum 4000 characters.");
                }
            }
            case IRL -> {
                if (name.length() > 75) {
                    throw new IllegalArgumentException("IRL name must be maximum 75 characters.");
                }
                if (description.length() > 255) {
                    throw new IllegalArgumentException("IRL description must be maximum 255 characters.");
                }
            }
            case SRL -> {
                if (name.length() > 255) {
                    throw new IllegalArgumentException("SRL name must be maximum 255 characters.");
                }
                if (description.length() > 255) {
                    throw new IllegalArgumentException("SRL description must be maximum 255 characters.");
                }
            }
        }

        if (color != null && color.length() > 20) {
            throw new IllegalArgumentException("Color must be maximum 20 characters.");
        }

        if (lookupRow.lookupType() == LookupType.IRL) {
            resolveIrlOrder(lookupRow.code());
        }
    }

    private void validateClassificationRow(ClassificationRow classificationRow) {
        String code = normalizeClassificationCode(classificationRow.code());

        if (code.isBlank()) {
            throw new IllegalArgumentException("Code is required.");
        }

        if (safeText(classificationRow.description()).isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }

        if (!code.matches("[A-Z][A-Z0-9_]?$")) {
            throw new IllegalArgumentException("Code must start with A-Z and may be followed by A-Z, 0-9 or _.");
        }

        if (safeText(classificationRow.description()).length() > 255) {
            throw new IllegalArgumentException("Description must be maximum 255 characters.");
        }

    }

    private Integer requireCustomerId() {
        if (getWebSession() == null || getWebSession().getCustomerId() == null) {
            throw new IllegalStateException("CustomerId is missing from the current session.");
        }

        return getWebSession().getCustomerId();
    }

    private Integer requireProjectId() {
        if (getWebSession() == null || getWebSession().getProjectId() == null) {
            throw new IllegalStateException("ProjectId is missing from the current session.");
        }

        return getWebSession().getProjectId();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeColor(String color) {
        String normalized = safeText(color);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeNullableText(String value) {
        String normalized = safeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeClassificationCode(String value) {
        return safeText(value).toUpperCase(Locale.ROOT);
    }

    private Integer resolveIrlOrder(String code) {
        if (code == null) {
            throw new IllegalArgumentException("IRLCode is required.");
        }

        return switch (code.trim()) {
            case "-" -> 0;
            case "0" -> 1;
            case "1" -> 2;
            case "2" -> 3;
            case "3" -> 4;
            case "4" -> 5;
            case "5" -> 6;
            case "6" -> 7;
            case "7" -> 8;
            case "8" -> 9;
            case "9" -> 10;
            default -> throw new IllegalArgumentException("Unsupported IRLCode: " + code);
        };
    }

    private static String irlSortExpression() {
        return irlSortExpression("IRLCode");
    }

    private static String irlSortExpression(String columnName) {
        return "CASE " + columnName + " " +
                "WHEN '-' THEN 0 " +
                "WHEN '0' THEN 1 " +
                "WHEN '1' THEN 2 " +
                "WHEN '2' THEN 3 " +
                "WHEN '3' THEN 4 " +
                "WHEN '4' THEN 5 " +
                "WHEN '5' THEN 6 " +
                "WHEN '6' THEN 7 " +
                "WHEN '7' THEN 8 " +
                "WHEN '8' THEN 9 " +
                "WHEN '9' THEN 10 " +
                "ELSE 999 END";
    }

    @FunctionalInterface
    private interface RowMapper {
        LookupRow map(ResultSet resultSet) throws SQLException;
    }

    public record LookupMaintenanceData(
            List<LookupRow> trlRows,
            List<LookupRow> irlRows,
            List<LookupRow> srlRows,
            List<ClassificationRow> classificationRows
    ) {
    }

    public enum LookupType {
        TRL,
        IRL,
        SRL,
        CLASSIFICATION
    }

    public record LookupRow(
            LookupType lookupType,
            Integer rowId,
            Integer level,
            String code,
            String name,
            String description,
            boolean active,
            String color
    ) {
    }

    public record ClassificationRow(
            Integer classificationId,
            Integer classId,
            String code,
            String description,
            String example,
            String usageExample,
            boolean active
    ) {
    }
}
