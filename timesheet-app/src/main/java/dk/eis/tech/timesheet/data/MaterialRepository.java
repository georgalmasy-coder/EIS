package dk.eis.tech.timesheet.data;

import dk.eis.tech.timesheet.model.MaterialEntryRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaterialRepository {

    public List<MaterialEntryRecord> findByCustomerAndMonth(long customerId, int year, int month) throws SQLException {
        String sql = """
                SELECT id, customer_id, entry_date, quantity, unit, short_description, unit_price, is_deleted,
                       created_at, updated_at
                FROM dbo.material_entries
                WHERE customer_id = ? AND is_deleted = 0
                  AND YEAR(entry_date) = ? AND MONTH(entry_date) = ?
                ORDER BY entry_date, short_description
                """;
        List<MaterialEntryRecord> entries = new ArrayList<>();
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
            }
        }
        return entries;
    }

    public Optional<MaterialEntryRecord> findById(long id) throws SQLException {
        String sql = """
                SELECT id, customer_id, entry_date, quantity, unit, short_description, unit_price, is_deleted,
                       created_at, updated_at
                FROM dbo.material_entries
                WHERE id = ? AND is_deleted = 0
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public long insert(long customerId, MaterialEntryRecord entry) throws SQLException {
        String sql = """
                INSERT INTO dbo.material_entries (customer_id, entry_date, quantity, unit, short_description, unit_price)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setDate(2, Date.valueOf(entry.entryDate()));
            statement.setBigDecimal(3, entry.quantity());
            statement.setString(4, entry.unit());
            statement.setString(5, entry.shortDescription());
            statement.setBigDecimal(6, entry.unitPrice());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create material entry");
    }

    public void update(long id, MaterialEntryRecord entry) throws SQLException {
        String sql = """
                UPDATE dbo.material_entries
                SET entry_date = ?, quantity = ?, unit = ?, short_description = ?, unit_price = ?, updated_at = SYSDATETIME()
                WHERE id = ? AND is_deleted = 0
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(entry.entryDate()));
            statement.setBigDecimal(2, entry.quantity());
            statement.setString(3, entry.unit());
            statement.setString(4, entry.shortDescription());
            statement.setBigDecimal(5, entry.unitPrice());
            statement.setLong(6, id);
            statement.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dbo.material_entries
                     SET is_deleted = 1, updated_at = SYSDATETIME()
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private MaterialEntryRecord map(ResultSet resultSet) throws SQLException {
        return new MaterialEntryRecord(
                resultSet.getLong("id"),
                resultSet.getLong("customer_id"),
                resultSet.getDate("entry_date").toLocalDate(),
                resultSet.getBigDecimal("quantity"),
                resultSet.getString("unit"),
                resultSet.getString("short_description"),
                resultSet.getBigDecimal("unit_price"),
                resultSet.getBoolean("is_deleted"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
