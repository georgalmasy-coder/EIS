package dk.eis.tech.timesheet.data;

import dk.eis.tech.timesheet.model.CustomerRecord;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {

    public List<CustomerRecord> findAll() throws SQLException {
        String sql = """
                SELECT id, company_name, contact_name, contact_email, phone_number, address_line,
                       postal_code, city, hourly_rate, vat_rate, is_inactive, created_at, updated_at
                FROM dbo.customers
                ORDER BY is_inactive, company_name
                """;
        List<CustomerRecord> customers = new ArrayList<>();
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                customers.add(map(resultSet));
            }
        }
        return customers;
    }

    public Optional<CustomerRecord> findById(long id) throws SQLException {
        String sql = """
                SELECT id, company_name, contact_name, contact_email, phone_number, address_line,
                       postal_code, city, hourly_rate, vat_rate, is_inactive, created_at, updated_at
                FROM dbo.customers
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

    public long insert(CustomerRecord customer) throws SQLException {
        String sql = """
                INSERT INTO dbo.customers (
                    company_name, contact_name, contact_email, phone_number,
                    address_line, postal_code, city, hourly_rate, vat_rate, is_inactive
                )
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDetails(statement, customer);
            statement.setBoolean(10, customer.inactive());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }
        throw new SQLException("Unable to create customer");
    }

    public void update(long id, CustomerRecord customer) throws SQLException {
        String sql = """
                UPDATE dbo.customers
                SET company_name = ?, contact_name = ?, contact_email = ?, phone_number = ?,
                    address_line = ?, postal_code = ?, city = ?, hourly_rate = ?, vat_rate = ?,
                    is_inactive = ?,
                    updated_at = SYSDATETIME()
                WHERE id = ?
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDetails(statement, customer);
            statement.setBoolean(10, customer.inactive());
            statement.setLong(11, id);
            statement.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.activities SET is_inactive = 1, updated_at = SYSDATETIME()
                        WHERE customer_id = ? AND is_inactive = 0
                        """)) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.customers SET is_inactive = 1, updated_at = SYSDATETIME()
                        WHERE id = ?
                        """)) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void bindDetails(PreparedStatement statement, CustomerRecord customer) throws SQLException {
        statement.setString(1, customer.companyName());
        statement.setString(2, customer.contactName());
        statement.setString(3, customer.contactEmail());
        statement.setString(4, customer.phoneNumber());
        statement.setString(5, customer.addressLine());
        statement.setString(6, customer.postalCode());
        statement.setString(7, customer.city());
        statement.setBigDecimal(8, customer.hourlyRate());
        statement.setBigDecimal(9, customer.vatRate());
    }

    private CustomerRecord map(ResultSet resultSet) throws SQLException {
        return new CustomerRecord(
                resultSet.getLong("id"),
                resultSet.getString("company_name"),
                resultSet.getString("contact_name"),
                resultSet.getString("contact_email"),
                resultSet.getString("phone_number"),
                resultSet.getString("address_line"),
                resultSet.getString("postal_code"),
                resultSet.getString("city"),
                resultSet.getBigDecimal("hourly_rate"),
                resultSet.getBigDecimal("vat_rate"),
                resultSet.getBoolean("is_inactive"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
