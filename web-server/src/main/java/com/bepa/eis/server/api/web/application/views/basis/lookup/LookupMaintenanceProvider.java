package com.bepa.eis.server.api.web.application.views.basis.lookup;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.web.application.cache.LookupCache;
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

public class LookupMaintenanceProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(LookupMaintenanceProvider.class);

    private static final String LOOKUP_CACHE_ALIAS = EhcacheProvider.LOOKUP_CACHE_ALIAS;

    private static final String SELECT_LOOKUP_TYPES_SQL =
            "SELECT LookupTypeId, LookupTypeDesc " +
            "FROM dbo.LOOKUP_TYPE " +
            "ORDER BY LookupTypeDesc, LookupTypeId";

    private static final String SELECT_LOOKUP_TYPE_BY_ID_SQL =
            "SELECT LookupTypeId, LookupTypeDesc " +
            "FROM dbo.LOOKUP_TYPE " +
            "WHERE LookupTypeId = ?";

    private static final String SELECT_LOOKUPS_SQL =
            "SELECT LookupId, LookupType, LookupCode, LookupDescription, Color, DisplayOrder, Active " +
            "FROM dbo.LOOKUP_TABLE " +
            "WHERE LookupType = ? " +
            "ORDER BY CASE WHEN DisplayOrder IS NULL THEN 1 ELSE 0 END, DisplayOrder, LookupCode, LookupId";

    private static final String SELECT_ALL_LOOKUPS_SQL =
            "SELECT LookupId, LookupType, LookupCode, LookupDescription, Color, DisplayOrder, Active " +
            "FROM dbo.LOOKUP_TABLE " +
            "ORDER BY LookupType, CASE WHEN DisplayOrder IS NULL THEN 1 ELSE 0 END, DisplayOrder, LookupCode, LookupId";

    private static final String SELECT_LOOKUP_BY_ID_SQL =
            "SELECT LookupId, LookupType, LookupCode, LookupDescription, Color, DisplayOrder, Active " +
            "FROM dbo.LOOKUP_TABLE " +
            "WHERE LookupId = ?";

    private static final String SELECT_DUPLICATE_CODE_SQL =
            "SELECT TOP 1 LookupId " +
            "FROM dbo.LOOKUP_TABLE " +
            "WHERE LookupType = ? " +
            "  AND LOWER(LTRIM(RTRIM(LookupCode))) = LOWER(LTRIM(RTRIM(?))) ";

    private static final String INSERT_LOOKUP_SQL =
            "INSERT INTO dbo.LOOKUP_TABLE (LookupType, LookupCode, LookupDescription, Color, DisplayOrder, Active) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_LOOKUP_WITH_ID_SQL =
            "INSERT INTO dbo.LOOKUP_TABLE (LookupId, LookupType, LookupCode, LookupDescription, Color, DisplayOrder, Active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_LOOKUP_TYPE_WITH_ID_SQL =
            "INSERT INTO dbo.LOOKUP_TYPE (LookupTypeId, LookupTypeDesc) " +
            "VALUES (?, ?)";

    private static final String UPDATE_LOOKUP_TYPE_SQL =
            "UPDATE dbo.LOOKUP_TYPE " +
            "SET LookupTypeDesc = ? " +
            "WHERE LookupTypeId = ?";

    private static final String UPDATE_LOOKUP_SQL =
            "UPDATE dbo.LOOKUP_TABLE " +
            "SET LookupType = ?, LookupCode = ?, LookupDescription = ?, Color = ?, DisplayOrder = ?, Active = ? " +
            "WHERE LookupId = ?";

    public LookupMaintenanceProvider(WebSession webSession) {
        super(webSession);
    }

    public List<LookupTypeRow> getLookupTypes() {
        List<LookupTypeRow> rows = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUP_TYPES_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapLookupType(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error getting lookup types", e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    public LookupTypeRow getLookupTypeById(Integer lookupTypeId) {
        if (lookupTypeId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUP_TYPE_BY_ID_SQL)) {

            setInt(statement, lookupTypeId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapLookupType(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting lookup type {}", lookupTypeId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public List<LookupRow> getLookups(Integer lookupTypeId) {
        List<LookupRow> rows = new ArrayList<>();

        if (lookupTypeId == null) {
            return rows;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUPS_SQL)) {

            setInt(statement, lookupTypeId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapLookup(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error getting lookup rows for type {}", lookupTypeId, e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    public List<LookupRow> getAllLookups() {
        List<LookupRow> rows = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_LOOKUPS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapLookup(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error getting all lookup rows", e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    public LookupRow getLookupById(Integer lookupId) {
        if (lookupId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUP_BY_ID_SQL)) {

            setInt(statement, lookupId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapLookup(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting lookup row {}", lookupId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public LookupRow saveLookup(LookupRow lookupRow) {
        if (lookupRow == null) {
            throw new IllegalArgumentException("Lookup row is required.");
        }

        Integer lookupTypeId = lookupRow.lookupTypeId();
        String lookupCode = safeText(lookupRow.lookupCode());
        String lookupDescription = safeText(lookupRow.lookupDescription());
        String color = normalizeColor(lookupRow.color());
        Integer displayOrder = lookupRow.displayOrder();
        boolean active = lookupRow.active();
        Integer lookupId = lookupRow.lookupId();

        validateLookupInput(lookupTypeId, lookupCode, lookupDescription, color);
        ensureLookupTypeExists(lookupTypeId);
        ensureUniqueLookupCode(lookupTypeId, lookupCode, lookupId);

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            Integer savedLookupId;

            try {
                if (lookupId == null) {
                    savedLookupId = insertLookup(connection, lookupTypeId, lookupCode, lookupDescription, color, displayOrder, active);
                } else {
                    savedLookupId = updateLookup(connection, lookupId, lookupTypeId, lookupCode, lookupDescription, color, displayOrder, active);
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            EhcacheProvider.clearCache(LOOKUP_CACHE_ALIAS, Integer.class, LookupCache.class);

            return getLookupById(savedLookupId);
        } catch (SQLException e) {
            log.error("Error saving lookup row {}", lookupRow, e);
            throw new RuntimeException(e);
        }
    }

    public ImportResult importLookupData(List<LookupTypeRow> lookupTypes, List<LookupRow> lookups) {
        if (lookupTypes == null || lookups == null) {
            throw new IllegalArgumentException("Lookup import data is required.");
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            int typeInserted = 0;
            int typeUpdated = 0;
            int lookupInserted = 0;
            int lookupUpdated = 0;

            try {
                setIdentityInsert(connection, "dbo.LOOKUP_TYPE", true);
                try {
                    for (LookupTypeRow lookupType : lookupTypes) {
                        if (lookupType.lookupTypeId() == null) {
                            throw new IllegalArgumentException("LookupTypeId is required for import.");
                        }
                        if (getLookupTypeById(connection, lookupType.lookupTypeId()) == null) {
                            insertLookupTypeWithId(connection, lookupType);
                            typeInserted++;
                        } else {
                            updateLookupType(connection, lookupType);
                            typeUpdated++;
                        }
                    }
                } finally {
                    setIdentityInsert(connection, "dbo.LOOKUP_TYPE", false);
                }

                setIdentityInsert(connection, "dbo.LOOKUP_TABLE", true);
                try {
                    for (LookupRow lookup : lookups) {
                        validateLookupInput(lookup.lookupTypeId(), safeText(lookup.lookupCode()), safeText(lookup.lookupDescription()), normalizeColor(lookup.color()));
                        if (lookup.lookupId() == null) {
                            throw new IllegalArgumentException("LookupId is required for import.");
                        }
                        if (getLookupById(connection, lookup.lookupId()) == null) {
                            insertLookupWithId(connection, lookup);
                            lookupInserted++;
                        } else {
                            updateLookup(connection, lookup.lookupId(), lookup.lookupTypeId(), safeText(lookup.lookupCode()),
                                    safeText(lookup.lookupDescription()), normalizeColor(lookup.color()), lookup.displayOrder(), lookup.active());
                            lookupUpdated++;
                        }
                    }
                } finally {
                    setIdentityInsert(connection, "dbo.LOOKUP_TABLE", false);
                }

                connection.commit();
                EhcacheProvider.clearCache(LOOKUP_CACHE_ALIAS, Integer.class, LookupCache.class);
                return new ImportResult(typeInserted, typeUpdated, lookupInserted, lookupUpdated);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error importing lookup data", e);
            throw new RuntimeException(e);
        }
    }

    private LookupTypeRow getLookupTypeById(Connection connection, Integer lookupTypeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUP_TYPE_BY_ID_SQL)) {
            setInt(statement, lookupTypeId, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapLookupType(resultSet) : null;
            }
        }
    }

    private LookupRow getLookupById(Connection connection, Integer lookupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LOOKUP_BY_ID_SQL)) {
            setInt(statement, lookupId, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapLookup(resultSet) : null;
            }
        }
    }

    private Integer insertLookup(
            Connection connection,
            Integer lookupTypeId,
            String lookupCode,
            String lookupDescription,
            String color,
            Integer displayOrder,
            boolean active
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_LOOKUP_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setInt(statement, lookupTypeId, 1);
            setString(statement, lookupCode, 2);
            setString(statement, lookupDescription, 3);
            setString(statement, color, 4);
            setInt(statement, displayOrder, 5);
            setBoolean(statement, active, 6);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert lookup row failed.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not read generated lookup id.");
    }

    private void insertLookupTypeWithId(Connection connection, LookupTypeRow lookupType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_LOOKUP_TYPE_WITH_ID_SQL)) {
            setInt(statement, lookupType.lookupTypeId(), 1);
            setString(statement, safeText(lookupType.lookupTypeDesc()), 2);
            statement.executeUpdate();
        }
    }

    private void updateLookupType(Connection connection, LookupTypeRow lookupType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LOOKUP_TYPE_SQL)) {
            setString(statement, safeText(lookupType.lookupTypeDesc()), 1);
            setInt(statement, lookupType.lookupTypeId(), 2);
            statement.executeUpdate();
        }
    }

    private void insertLookupWithId(Connection connection, LookupRow lookup) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_LOOKUP_WITH_ID_SQL)) {
            setInt(statement, lookup.lookupId(), 1);
            setInt(statement, lookup.lookupTypeId(), 2);
            setString(statement, safeText(lookup.lookupCode()), 3);
            setString(statement, safeText(lookup.lookupDescription()), 4);
            setString(statement, normalizeColor(lookup.color()), 5);
            setInt(statement, lookup.displayOrder(), 6);
            setBoolean(statement, lookup.active(), 7);
            statement.executeUpdate();
        }
    }

    private void setIdentityInsert(Connection connection, String tableName, boolean enabled) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET IDENTITY_INSERT " + tableName + " " + (enabled ? "ON" : "OFF"));
        }
    }

    private Integer updateLookup(
            Connection connection,
            Integer lookupId,
            Integer lookupTypeId,
            String lookupCode,
            String lookupDescription,
            String color,
            Integer displayOrder,
            boolean active
    ) throws SQLException {
        if (lookupId == null) {
            throw new IllegalArgumentException("LookupId is required for update.");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LOOKUP_SQL)) {
            setInt(statement, lookupTypeId, 1);
            setString(statement, lookupCode, 2);
            setString(statement, lookupDescription, 3);
            setString(statement, color, 4);
            setInt(statement, displayOrder, 5);
            setBoolean(statement, active, 6);
            setInt(statement, lookupId, 7);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Lookup row was not updated.");
            }
        }

        return lookupId;
    }

    private void ensureLookupTypeExists(Integer lookupTypeId) {
        if (lookupTypeId == null) {
            throw new IllegalArgumentException("LookupType is required.");
        }

        if (getLookupTypeById(lookupTypeId) == null) {
            throw new IllegalArgumentException("Unknown lookup type: " + lookupTypeId);
        }
    }

    private void ensureUniqueLookupCode(Integer lookupTypeId, String lookupCode, Integer excludeLookupId) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(buildDuplicateLookupSql(excludeLookupId))) {

            setInt(statement, lookupTypeId, 1);
            setString(statement, lookupCode, 2);

            if (excludeLookupId != null) {
                setInt(statement, excludeLookupId, 3);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("LookupCode must be unique within the selected lookup type.");
                }
            }
        } catch (SQLException e) {
            log.error("Error checking unique lookup code for type {} and code {}", lookupTypeId, lookupCode, e);
            throw new RuntimeException(e);
        }
    }

    private String buildDuplicateLookupSql(Integer excludeLookupId) {
        if (excludeLookupId == null) {
            return SELECT_DUPLICATE_CODE_SQL;
        }

        return SELECT_DUPLICATE_CODE_SQL + " AND LookupId <> ?";
    }

    private LookupTypeRow mapLookupType(ResultSet resultSet) throws SQLException {
        return new LookupTypeRow(
                resultSet.getInt("LookupTypeId"),
                resultSet.getString("LookupTypeDesc")
        );
    }

    private LookupRow mapLookup(ResultSet resultSet) throws SQLException {
        return new LookupRow(
                resultSet.getInt("LookupId"),
                resultSet.getInt("LookupType"),
                resultSet.getString("LookupCode"),
                resultSet.getString("LookupDescription"),
                resultSet.getString("Color"),
                getNullableInt(resultSet, "DisplayOrder"),
                resultSet.getBoolean("Active")
        );
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private void validateLookupInput(
            Integer lookupTypeId,
            String lookupCode,
            String lookupDescription,
            String color
    ) {
        if (lookupTypeId == null) {
            throw new IllegalArgumentException("LookupType is required.");
        }

        if (lookupCode.isBlank()) {
            throw new IllegalArgumentException("LookupCode is required.");
        }

        if (lookupCode.length() > 50) {
            throw new IllegalArgumentException("LookupCode must be maximum 50 characters.");
        }

        if (lookupDescription.length() > 255) {
            throw new IllegalArgumentException("LookupDescription must be maximum 255 characters.");
        }

        if (color != null && color.length() > 20) {
            throw new IllegalArgumentException("Color must be maximum 20 characters.");
        }
    }

    private String normalizeColor(String color) {
        String normalized = safeText(color);

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.matches("(?i)^#[0-9a-f]{3}$") || normalized.matches("(?i)^#[0-9a-f]{6}$") || normalized.matches("^[a-zA-Z]+$")) {
            return normalized;
        }

        throw new IllegalArgumentException("Color must be a browser-supported color name or hex value.");
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    public record LookupTypeRow(Integer lookupTypeId, String lookupTypeDesc) {
    }

    public record LookupRow(
            Integer lookupId,
            Integer lookupTypeId,
            String lookupCode,
            String lookupDescription,
            String color,
            Integer displayOrder,
            boolean active
    ) {
    }

    public record ImportResult(int lookupTypesInserted, int lookupTypesUpdated, int lookupsInserted, int lookupsUpdated) {
        public int totalRows() {
            return lookupTypesInserted + lookupTypesUpdated + lookupsInserted + lookupsUpdated;
        }
    }
}
