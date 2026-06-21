package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerRegistrationUserProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationUserProvider.class);

    private static final String SELECT_ACTIVE_USER_BY_EMAIL_SQL =
            "SELECT TOP (1) " +
                    "UserId " +
                    "FROM [dbo].[USERS] " +
                    "WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?))) " +
                    "  AND Active = 1 ";

    private static final String INSERT_USER_SQL =
            "INSERT INTO [dbo].[USERS] ( " +
                    "Initials, " +
                    "Name, " +
                    "Email, " +
                    "Phone, " +
                    "DepartmentId, " +
                    "Active, " +
                    "Password " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ";

    private static final String INSERT_USER_CUSTOMER_SQL =
            "INSERT INTO [dbo].[USER_CUSTOMER] ( " +
                    "UserId, " +
                    "CustomerId " +
                    ") " +
                    "VALUES (?, ?) ";

    public CustomerRegistrationUserProvider(WebSession webSession) {
        super(webSession);
    }

    public boolean activeUserExistsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_USER_BY_EMAIL_SQL)) {

            statement.setString(1, email.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            log.error("Error checking active user by email. email={}", email, e);
            return false;
        }
    }

    public Integer createActiveUser(
            String name,
            String email,
            String phone
    ) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_USER_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, initialsFromName(name));
            statement.setString(2, name.trim());
            statement.setString(3, email.trim().toLowerCase());
            statement.setString(4, nullIfBlank(phone));
            statement.setNull(5, java.sql.Types.INTEGER);
            statement.setBoolean(6, true);

            /*
             * User is created without a usable password.
             * The onboarding / confirmation workflow must handle password setup.
             */
            statement.setString(7, "");

            int rows = statement.executeUpdate();

            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return null;
        } catch (SQLException e) {
            log.error("Error creating active user. email={}", email, e);
            return null;
        }
    }

    public boolean linkUserToCustomer(
            Integer userId,
            Integer customerId
    ) {
        if (userId == null || customerId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER_CUSTOMER_SQL)) {

            statement.setInt(1, userId);
            statement.setInt(2, customerId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(
                    "Error linking user to customer. userId={}, customerId={}",
                    userId,
                    customerId,
                    e
            );
            return false;
        }
    }

    private String initialsFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "??";
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(part.substring(0, 1).toUpperCase());
            }

            if (initials.length() >= 3) {
                break;
            }
        }

        if (initials.isEmpty()) {
            return "??";
        }

        return initials.toString();
    }

    private String nullIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}