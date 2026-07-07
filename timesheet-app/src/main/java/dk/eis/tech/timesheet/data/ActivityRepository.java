package dk.eis.tech.timesheet.data;

import dk.eis.tech.timesheet.model.ActivityRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityRepository {

    public List<ActivityRecord> findByCustomerId(long customerId) throws SQLException {
        String sql = """
                SELECT id, customer_id, short_description, long_description, is_inactive, created_at, updated_at
                FROM dbo.activities
                WHERE customer_id = ?
                ORDER BY is_inactive, short_description
                """;
        List<ActivityRecord> activities = new ArrayList<>();
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    activities.add(map(resultSet));
                }
            }
        }
        return activities;
    }

    public Optional<ActivityRecord> findById(long id) throws SQLException {
        String sql = """
                SELECT id, customer_id, short_description, long_description, is_inactive, created_at, updated_at
                FROM dbo.activities
                WHERE id = ?
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

    public long insert(long customerId, ActivityRecord activity) throws SQLException {
        String sql = """
                INSERT INTO dbo.activities (customer_id, short_description, long_description, is_inactive)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setString(2, activity.shortDescription());
            statement.setString(3, activity.longDescription());
            statement.setBoolean(4, activity.inactive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create activity");
    }

    public void update(long id, ActivityRecord activity) throws SQLException {
        String sql = """
                UPDATE dbo.activities
                SET short_description = ?, long_description = ?, is_inactive = ?, updated_at = SYSDATETIME()
                WHERE id = ?
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, activity.shortDescription());
            statement.setString(2, activity.longDescription());
            statement.setBoolean(3, activity.inactive());
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dbo.activities
                     SET is_inactive = 1, updated_at = SYSDATETIME()
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private ActivityRecord map(ResultSet resultSet) throws SQLException {
        return new ActivityRecord(
                resultSet.getLong("id"),
                resultSet.getLong("customer_id"),
                resultSet.getString("short_description"),
                resultSet.getString("long_description"),
                resultSet.getBoolean("is_inactive"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
