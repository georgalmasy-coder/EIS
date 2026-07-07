package dk.eis.tech.timesheet.data;

import dk.eis.tech.timesheet.model.TimeEntryRecord;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeEntryRepository {

    public List<TimeEntryRecord> findByCustomerAndMonth(long customerId, int year, int month) throws SQLException {
        String sql = """
                SELECT t.id, t.customer_id, t.activity_id, t.entry_date, t.hours, t.note, t.is_deleted,
                       t.created_at, t.updated_at, a.short_description, a.long_description
                FROM dbo.time_entries t
                JOIN dbo.activities a ON a.id = t.activity_id
                WHERE t.customer_id = ? AND t.is_deleted = 0
                  AND YEAR(t.entry_date) = ? AND MONTH(t.entry_date) = ?
                ORDER BY t.entry_date, a.short_description
                """;
        List<TimeEntryRecord> entries = new ArrayList<>();
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

    public List<TimeEntryRecord> findByCustomerAndDate(long customerId, LocalDate date) throws SQLException {
        String sql = """
                SELECT t.id, t.customer_id, t.activity_id, t.entry_date, t.hours, t.note, t.is_deleted,
                       t.created_at, t.updated_at, a.short_description, a.long_description
                FROM dbo.time_entries t
                JOIN dbo.activities a ON a.id = t.activity_id
                WHERE t.customer_id = ? AND t.is_deleted = 0
                  AND t.entry_date = ?
                ORDER BY a.short_description
                """;
        List<TimeEntryRecord> entries = new ArrayList<>();
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
            }
        }
        return entries;
    }

    public Optional<TimeEntryRecord> findById(long id) throws SQLException {
        String sql = """
                SELECT t.id, t.customer_id, t.activity_id, t.entry_date, t.hours, t.note, t.is_deleted,
                       t.created_at, t.updated_at, a.short_description, a.long_description
                FROM dbo.time_entries t
                JOIN dbo.activities a ON a.id = t.activity_id
                WHERE t.id = ? AND t.is_deleted = 0
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

    public long insert(long customerId, TimeEntryRecord entry) throws SQLException {
        String sql = """
                INSERT INTO dbo.time_entries (customer_id, activity_id, entry_date, hours, note)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setLong(2, entry.activityId());
            statement.setDate(3, Date.valueOf(entry.entryDate()));
            statement.setBigDecimal(4, entry.hours());
            statement.setString(5, entry.note());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create time entry");
    }

    public void update(long id, TimeEntryRecord entry) throws SQLException {
        String sql = """
                UPDATE dbo.time_entries
                SET activity_id = ?, entry_date = ?, hours = ?, note = ?, updated_at = SYSDATETIME()
                WHERE id = ? AND is_deleted = 0
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entry.activityId());
            statement.setDate(2, Date.valueOf(entry.entryDate()));
            statement.setBigDecimal(3, entry.hours());
            statement.setString(4, entry.note());
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dbo.time_entries
                     SET is_deleted = 1, updated_at = SYSDATETIME()
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private TimeEntryRecord map(ResultSet resultSet) throws SQLException {
        return new TimeEntryRecord(
                resultSet.getLong("id"),
                resultSet.getLong("customer_id"),
                resultSet.getLong("activity_id"),
                resultSet.getDate("entry_date").toLocalDate(),
                resultSet.getBigDecimal("hours"),
                resultSet.getString("note"),
                resultSet.getBoolean("is_deleted"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime(),
                resultSet.getString("short_description"),
                resultSet.getString("long_description")
        );
    }
}
