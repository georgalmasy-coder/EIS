package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.common.scheduling.CleanupJobSettings.Type;
import java.sql.*;

public class DataCleanupProvider extends GenericProvider {
    private final javax.sql.DataSource dataSource;
    public DataCleanupProvider() { this(null); }
    public DataCleanupProvider(javax.sql.DataSource dataSource) { super(null); this.dataSource = dataSource; }

    public long deleteExpired(Type type, int retentionDays) throws SQLException {
        if (retentionDays < 1 || retentionDays > 36500) throw new IllegalArgumentException("Invalid retention days");
        // Identifiers come only from the fixed enum, never from configuration.
        String sql = "DELETE TOP (1000) FROM [dbo].[" + type.table + "] WHERE [" + type.dateColumn + "] < ?";
        try (Connection connection = (dataSource == null ? getDataSource() : dataSource).getConnection()) {
            Timestamp cutoff;
            try (PreparedStatement statement = connection.prepareStatement("SELECT DATEADD(day, -?, GETDATE())")) {
                statement.setQueryTimeout(60);
                statement.setInt(1, retentionDays);
                try (ResultSet rows = statement.executeQuery()) { rows.next(); cutoff = rows.getTimestamp(1); }
            }
            long deleted = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, cutoff);
                statement.setQueryTimeout(60);
                int batch;
                do {
                    if (Thread.currentThread().isInterrupted()) throw new SQLException("Cleanup interrupted");
                    batch = statement.executeUpdate();
                    deleted += batch;
                } while (batch == 1000);
            }
            return deleted;
        }
    }
}
