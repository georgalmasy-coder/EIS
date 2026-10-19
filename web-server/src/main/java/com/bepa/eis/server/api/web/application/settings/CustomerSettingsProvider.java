package com.bepa.eis.server.api.web.application.settings;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import java.sql.*;
import java.util.Map;

/** Web-only persistence, with one XML document per customer. */
public class CustomerSettingsProvider extends GenericProvider {
    public CustomerSettingsProvider(WebSession session) { super(session); }

    public CustomerSettings load(int customerId) throws SQLException {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT CONVERT(nvarchar(max), Settings) FROM dbo.CUSTOMER_SETTINGS WHERE CustomerId = ?")) {
            statement.setInt(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                return new CustomerSettings(result.next() ? result.getString(1) : null);
            }
        }
    }

    public void save(Map<String, String> values) throws SQLException {
        WebSession session = getWebSession();
        if (session == null || session.getCustomerId() == null) throw new IllegalArgumentException("Customer is required");
        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Lock the row (or key range for a new customer) before reading. Concurrent
                // saves for other projects then merge into the latest XML, not a cached copy.
                String xml = null;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT CONVERT(nvarchar(max), Settings) FROM dbo.CUSTOMER_SETTINGS WITH (UPDLOCK, HOLDLOCK)
                        WHERE CustomerId = ?
                        """)) {
                    statement.setInt(1, session.getCustomerId());
                    try (ResultSet result = statement.executeQuery()) { if (result.next()) xml = result.getString(1); }
                }
                String updated = new CustomerSettings(xml).update(session.getCustomerId(), session.getProjectId(), values).toXml();
                try (PreparedStatement statement = connection.prepareStatement("""
                        MERGE dbo.CUSTOMER_SETTINGS AS target
                        USING (SELECT ? AS CustomerId, CONVERT(xml, ?) AS Settings, ? AS ChangedByUserId) AS source
                        ON target.CustomerId = source.CustomerId
                        WHEN MATCHED THEN UPDATE SET Settings = source.Settings,
                            ChangedAt = SYSUTCDATETIME(), ChangedByUserId = source.ChangedByUserId
                        WHEN NOT MATCHED THEN INSERT (CustomerId, Settings, ChangedAt, ChangedByUserId)
                            VALUES (source.CustomerId, source.Settings, SYSUTCDATETIME(), source.ChangedByUserId);
                        """)) {
                    statement.setInt(1, session.getCustomerId());
                    statement.setString(2, updated);
                    setInt(statement, session.getUserId(), 3);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
