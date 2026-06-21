package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.project.ProjectStatus;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.security.MfaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(UserProvider.class);

    private static final String VALIDATE_LOGIN_SQL = """
            SELECT
                UserId,
                [Password],
                Active,
                LockedUntil
            FROM [dbo].[USERS]
            WHERE Email = ?
            """;

    private static final String SELECT_EMAIL_BY_USER_ID_SQL = """
            SELECT Email
            FROM [dbo].[USERS]
            WHERE UserId = ?
            """;

    private static final String SELECT_USER_DISPLAY_NAME_BY_ID_SQL = """
            SELECT
                Name,
                Email
            FROM [dbo].[USERS]
            WHERE UserId = ?
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT UserId
            FROM [dbo].[USERS]
            WHERE Email = ?
            """;

    private static final String SELECT_USER_MFA_BY_USER_ID_SQL = """
            SELECT
                UserId,
                Email,
                MfaEnabled,
                MfaVerified,
                MfaSecretEncrypted,
                UserMfaPolicy,
                MfaResetRequired
            FROM [dbo].[USERS]
            WHERE UserId = ?
            """;

    private static final String SELECT_USER_MFA_BY_EMAIL_SQL = """
            SELECT
                UserId,
                Email,
                MfaEnabled,
                MfaVerified,
                MfaSecretEncrypted,
                UserMfaPolicy,
                MfaResetRequired
            FROM [dbo].[USERS]
            WHERE Email = ?
            """;

    private static final String SELECT_CUSTOMER_MFA_POLICY_BY_CUSTOMER_ID_SQL = """
            SELECT TOP (1)
                CustomerMfaPolicy
            FROM [dbo].[CUSTOMER]
            WHERE CustomerId = ?
              AND Latest = 1
            """;

    private static final String UPDATE_MFA_LAST_VERIFIED_SQL = """
            UPDATE [dbo].[USERS]
            SET MfaLastVerifiedAt = GETDATE()
            WHERE UserId = ?
            """;

    private static final String ENABLE_MFA_AFTER_SETUP_SQL = """
            UPDATE [dbo].[USERS]
            SET
                MfaEnabled = 1,
                MfaVerified = 1,
                MfaSecretEncrypted = ?,
                MfaEnabledAt = COALESCE(MfaEnabledAt, GETDATE()),
                MfaLastVerifiedAt = GETDATE(),
                MfaResetRequired = 0,
                MfaResetAt = NULL,
                MfaResetByUserId = NULL
            WHERE UserId = ?
            """;

    private static final String RESET_MFA_SQL = """
            UPDATE [dbo].[USERS]
            SET
                MfaEnabled = 0,
                MfaVerified = 0,
                MfaSecretEncrypted = NULL,
                MfaResetRequired = 1,
                MfaResetAt = GETDATE(),
                MfaResetByUserId = ?
            WHERE UserId = ?
            """;

    private static final String CLEAR_MFA_RESET_REQUIRED_SQL = """
            UPDATE [dbo].[USERS]
            SET
                MfaResetRequired = 0,
                MfaResetAt = NULL,
                MfaResetByUserId = NULL
            WHERE UserId = ?
            """;

    private static final String MARK_MFA_RESET_REQUIRED_SQL = """
            UPDATE [dbo].[USERS]
            SET
                MfaResetRequired = 1,
                MfaResetAt = GETDATE(),
                MfaResetByUserId = ?
            WHERE UserId = ?
            """;

    private static final String SET_USER_MFA_POLICY_SQL = """
            UPDATE [dbo].[USERS]
            SET UserMfaPolicy = ?
            WHERE UserId = ?
            """;

    private static final String DISABLE_MFA_SQL = """
            UPDATE [dbo].[USERS]
            SET
                MfaEnabled = 0,
                MfaVerified = 0,
                MfaSecretEncrypted = NULL,
                MfaResetRequired = 0,
                MfaResetAt = NULL,
                MfaResetByUserId = NULL
            WHERE UserId = ?
            """;

    private static final String ACTIVE_PROJECT_STATUS_IDS =
            ProjectStatus.CREATED.getId() + ", " +
                    ProjectStatus.PLANNED.getId() + ", " +
                    ProjectStatus.IN_PROGRESS.getId() + ", " +
                    ProjectStatus.ON_HOLD.getId() + ", " +
                    ProjectStatus.AT_RISK.getId();

    private static final String CUSTOMER_PROJECT_SQL =
            "SELECT P.ProjectId, P.ProjectName, C.CustomerId, C.CustomerName " +
                    "FROM PROJECT P, CUSTOMER C " +
                    "WHERE P.ProjectId IN ( " +
                    "    SELECT ProjectId " +
                    "    FROM USER_PROJECT " +
                    "    WHERE UserId IN ( " +
                    "        SELECT UserId " +
                    "        FROM USERS " +
                    "        WHERE Email = ? " +
                    "    ) " +
                    ") " +
                    "AND P.Latest = 1 " +
                    "AND P.ProjectStatus IN (" + ACTIVE_PROJECT_STATUS_IDS + ") " +
                    "AND P.CustomerId = C.CustomerId " +
                    "AND C.Latest = 1 ";

    private static final String ACTIVE_CUSTOMER_STATUS_IDS =
            CustomerStatus.PENDING_CONFIRMATION.getId() + ", " +
                    CustomerStatus.TRIAL_ACTIVE.getId() + ", " +
                    CustomerStatus.PENDING_SUBSCRIPTION_CONFIRMATION.getId() + ", " +
                    CustomerStatus.PAYMENT_PENDING.getId() + ", " +
                    CustomerStatus.SUBSCRIPTION_ACTIVE.getId() + ", " +
                    CustomerStatus.SUBSCRIPTION_EXPIRING.getId() + ", " +
                    CustomerStatus.PAYMENT_OVERDUE.getId();

    private static final String CUSTOMER_BY_USER_ID_SQL =
            "SELECT C.CustomerId, C.CustomerName, C.CustomerStatus, C.CustomerMfaPolicy " +
                    "FROM CUSTOMER C " +
                    "WHERE C.Latest = 1 " +
                    "AND C.CustomerStatus IN (" + ACTIVE_CUSTOMER_STATUS_IDS + ") " +
                    "AND C.CustomerId IN (SELECT CustomerId FROM USER_CUSTOMER WHERE UserId = ?) ";

    public UserProvider(WebSession webSession) {
        super(webSession);
    }

    public LoginValidationResult validateLogin(
            String email,
            String passwordInput
    ) {
        if (email == null || email.isBlank()) {
            return LoginValidationResult.failed(null, "Missing email");
        }

        if (passwordInput == null || passwordInput.isBlank()) {
            return LoginValidationResult.failed(null, "Missing password");
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(VALIDATE_LOGIN_SQL)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return LoginValidationResult.failed(null, "Unknown user");
                }

                int userId = resultSet.getInt("UserId");
                String passwordFromDb = resultSet.getString("Password");
                boolean active = resultSet.getBoolean("Active");

                if (!active) {
                    return LoginValidationResult.failed(userId, "User is inactive");
                }

                java.sql.Timestamp lockedUntil = resultSet.getTimestamp("LockedUntil");

                if (lockedUntil != null && lockedUntil.after(new java.util.Date())) {
                    return LoginValidationResult.failed(userId, "Account is locked");
                }

                if (passwordFromDb == null || !passwordFromDb.equals(passwordInput)) {
                    return LoginValidationResult.failed(userId, "Invalid password");
                }

                return LoginValidationResult.success(userId);
            }
        } catch (SQLException e) {
            log.error("Error validating login for email: {}", email, e);
            return LoginValidationResult.failed(null, "Login validation error");
        }
    }

    public List<CustomerRecord> getCustomersByUserId(Integer userId) {
        List<CustomerRecord> customers = new ArrayList<>();

        if (userId != null) {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(CUSTOMER_BY_USER_ID_SQL)) {

                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        CustomerRecord customer = new CustomerRecord();

                        customer.setCustomerId(resultSet.getInt("CustomerId"));
                        customer.setCustomerName(resultSet.getString("CustomerName"));

                        CustomerStatus customerStatus = CustomerStatus.fromId(resultSet.getInt("CustomerStatus"));
                        customer.setCustomerStatus(customerStatus);

                        customer.setCustomerMfaPolicy(resultSet.getString("CustomerMfaPolicy"));

                        customers.add(customer);
                    }
                }
            } catch (SQLException e) {
                log.error("Error loading customers for userId: {}", userId, e);
            }
        }

        return customers;
    }

    public UserMfaState getUserMfaStateByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_MFA_BY_USER_ID_SQL)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUserMfaState(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading MFA state for userId: {}", userId, e);
        }

        return null;
    }

    public UserMfaState getUserMfaStateByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_MFA_BY_EMAIL_SQL)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUserMfaState(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading MFA state for email: {}", email, e);
        }

        return null;
    }

    public MfaConfig.CustomerMfaPolicy getCustomerMfaPolicyByCustomerId(Integer customerId) {
        if (customerId == null) {
            return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CUSTOMER_MFA_POLICY_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return parseCustomerMfaPolicy(resultSet.getString("CustomerMfaPolicy"));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer MFA policy for customerId: {}", customerId, e);
        }

        return MfaConfig.CustomerMfaPolicy.OPTIONAL;
    }

    public boolean updateMfaLastVerified(Integer userId) {
        if (userId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_MFA_LAST_VERIFIED_SQL)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating MFA last verified timestamp for userId: {}", userId, e);
            return false;
        }
    }

    public boolean enableMfaAfterSetup(
            Integer userId,
            String encryptedSecret
    ) {
        String targetUserEmail = getEmailByUserId(userId);

        if (userId == null) {
            logMfaAuditEvent(
                    "system",
                    "MFA_ENABLE_FAILED",
                    targetUserEmail,
                    "MFA could not be enabled because userId was missing",
                    "Warning"
            );
            return false;
        }

        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            logMfaAuditEvent(
                    targetUserEmail,
                    "MFA_ENABLE_FAILED",
                    targetUserEmail,
                    "MFA could not be enabled because MFA secret was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(ENABLE_MFA_AFTER_SETUP_SQL)) {

            statement.setString(1, encryptedSecret);
            statement.setInt(2, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        targetUserEmail,
                        "MFA_ENABLED",
                        targetUserEmail,
                        "User enabled two-factor authentication",
                        "OK"
                );
            } else {
                logMfaAuditEvent(
                        targetUserEmail,
                        "MFA_ENABLE_FAILED",
                        targetUserEmail,
                        "MFA setup was verified, but no user row was updated",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error enabling MFA after setup for userId: {}", userId, e);

            logMfaAuditEvent(
                    targetUserEmail,
                    "MFA_ENABLE_FAILED",
                    targetUserEmail,
                    "MFA could not be enabled due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean resetMfa(Integer userId) {
        return resetMfa(userId, null);
    }

    public boolean resetMfaByEmail(
            String email,
            Integer resetByUserId
    ) {
        Integer userId = getUserIdByEmail(email);

        if (userId == null) {
            logMfaAuditEvent(
                    getEmailByUserId(resetByUserId),
                    "MFA_RESET_FAILED",
                    safeText(email, "unknown"),
                    "MFA reset failed because user was not found",
                    "Warning"
            );
            return false;
        }

        return resetMfa(userId, resetByUserId);
    }

    public boolean resetMfa(
            Integer userId,
            Integer resetByUserId
    ) {
        String targetUserEmail = getEmailByUserId(userId);
        String actorEmail = resetByUserId == null
                ? getActorEmail()
                : getEmailByUserId(resetByUserId);

        if (userId == null) {
            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_FAILED",
                    targetUserEmail,
                    "MFA reset failed because userId was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(RESET_MFA_SQL)) {

            if (resetByUserId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, resetByUserId);
            }

            statement.setInt(2, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_BY_ADMIN",
                        targetUserEmail,
                        "MFA was reset by administrator",
                        "Warning"
                );
            } else {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_FAILED",
                        targetUserEmail,
                        "MFA reset did not update any user row",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error resetting MFA for userId: {}", userId, e);

            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_FAILED",
                    targetUserEmail,
                    "MFA reset failed due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean disableMfaForUser(Integer userId) {
        String targetUserEmail = getEmailByUserId(userId);
        String actorEmail = getActorEmail();

        if (userId == null) {
            logMfaAuditEvent(
                    actorEmail,
                    "MFA_DISABLE_FAILED",
                    targetUserEmail,
                    "MFA disable failed because userId was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(DISABLE_MFA_SQL)) {

            statement.setInt(1, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_DISABLED",
                        targetUserEmail,
                        "MFA was disabled for user",
                        "Warning"
                );
            } else {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_DISABLE_FAILED",
                        targetUserEmail,
                        "MFA disable did not update any user row",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error disabling MFA for userId: {}", userId, e);

            logMfaAuditEvent(
                    actorEmail,
                    "MFA_DISABLE_FAILED",
                    targetUserEmail,
                    "MFA disable failed due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean enableMfaRequiredForUser(Integer userId) {
        return setUserMfaPolicy(userId, MfaConfig.UserMfaPolicy.REQUIRED);
    }

    public boolean setUserMfaPolicyToDefault(Integer userId) {
        return setUserMfaPolicy(userId, MfaConfig.UserMfaPolicy.DEFAULT);
    }

    public boolean setUserMfaPolicyByEmail(
            String email,
            MfaConfig.UserMfaPolicy userMfaPolicy
    ) {
        Integer userId = getUserIdByEmail(email);

        if (userId == null) {
            logMfaAuditEvent(
                    getActorEmail(),
                    "MFA_POLICY_CHANGE_FAILED",
                    safeText(email, "unknown"),
                    "User MFA policy could not be changed because user was not found",
                    "Warning"
            );
            return false;
        }

        return setUserMfaPolicy(userId, userMfaPolicy);
    }

    public boolean setUserMfaPolicy(
            Integer userId,
            MfaConfig.UserMfaPolicy userMfaPolicy
    ) {
        String targetUserEmail = getEmailByUserId(userId);
        String actorEmail = getActorEmail();

        if (userId == null || userMfaPolicy == null) {
            logMfaAuditEvent(
                    actorEmail,
                    "MFA_POLICY_CHANGE_FAILED",
                    targetUserEmail,
                    "User MFA policy could not be changed because input was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SET_USER_MFA_POLICY_SQL)) {

            statement.setString(1, userMfaPolicy.name());
            statement.setInt(2, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_POLICY_CHANGED",
                        targetUserEmail,
                        "User MFA policy changed to " + userMfaPolicy.name(),
                        "OK"
                );
            } else {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_POLICY_CHANGE_FAILED",
                        targetUserEmail,
                        "User MFA policy change did not update any user row",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error updating MFA policy for userId: {}", userId, e);

            logMfaAuditEvent(
                    actorEmail,
                    "MFA_POLICY_CHANGE_FAILED",
                    targetUserEmail,
                    "User MFA policy change failed due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean markMfaResetRequired(
            Integer userId,
            Integer resetByUserId
    ) {
        String targetUserEmail = getEmailByUserId(userId);
        String actorEmail = resetByUserId == null
                ? getActorEmail()
                : getEmailByUserId(resetByUserId);

        if (userId == null) {
            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_REQUIRED_FAILED",
                    targetUserEmail,
                    "MFA reset-required flag could not be set because userId was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_MFA_RESET_REQUIRED_SQL)) {

            if (resetByUserId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, resetByUserId);
            }

            statement.setInt(2, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_REQUIRED",
                        targetUserEmail,
                        "User was marked as requiring MFA reset",
                        "Warning"
                );
            } else {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_REQUIRED_FAILED",
                        targetUserEmail,
                        "MFA reset-required flag did not update any user row",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error marking MFA reset required for userId: {}", userId, e);

            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_REQUIRED_FAILED",
                    targetUserEmail,
                    "MFA reset-required flag failed due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean clearMfaResetRequired(Integer userId) {
        String targetUserEmail = getEmailByUserId(userId);
        String actorEmail = getActorEmail();

        if (userId == null) {
            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_REQUIRED_CLEAR_FAILED",
                    targetUserEmail,
                    "MFA reset-required flag could not be cleared because userId was missing",
                    "Warning"
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(CLEAR_MFA_RESET_REQUIRED_SQL)) {

            statement.setInt(1, userId);

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_REQUIRED_CLEARED",
                        targetUserEmail,
                        "MFA reset-required flag was cleared",
                        "OK"
                );
            } else {
                logMfaAuditEvent(
                        actorEmail,
                        "MFA_RESET_REQUIRED_CLEAR_FAILED",
                        targetUserEmail,
                        "MFA reset-required flag clear did not update any user row",
                        "Warning"
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error("Error clearing MFA reset required for userId: {}", userId, e);

            logMfaAuditEvent(
                    actorEmail,
                    "MFA_RESET_REQUIRED_CLEAR_FAILED",
                    targetUserEmail,
                    "MFA reset-required flag clear failed due to a database error",
                    "Warning"
            );

            return false;
        }
    }

    public boolean isMfaConfigured(Integer userId) {
        UserMfaState userMfaState = getUserMfaStateByUserId(userId);
        return userMfaState != null && userMfaState.isMfaConfigured();
    }

    public boolean isMfaRequiredForUser(Integer userId) {
        return isMfaRequiredForUser(userId, null);
    }

    public boolean isMfaRequiredForUser(
            Integer userId,
            Integer customerId
    ) {
        UserMfaState userMfaState = getUserMfaStateByUserId(userId);

        if (userMfaState == null) {
            return false;
        }

        MfaConfig.CustomerMfaPolicy customerMfaPolicy = getCustomerMfaPolicyByCustomerId(customerId);

        return MfaConfig.isMfaRequired(
                customerMfaPolicy,
                userMfaState.userMfaPolicy(),
                userMfaState.mfaEnabled()
        );
    }

    public Integer getUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_ID_BY_EMAIL_SQL)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("UserId");
                    return resultSet.wasNull() ? null : userId;
                }
            }
        } catch (SQLException e) {
            log.warn("Could not load userId for email: {}", email, e);
        }

        return null;
    }

    public String getEmailByUserId(Integer userId) {
        if (userId == null) {
            return "unknown";
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_EMAIL_BY_USER_ID_SQL)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String email = resultSet.getString("Email");

                    if (email != null && !email.isBlank()) {
                        return email;
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("Could not load email for userId: {}", userId, e);
        }

        return "unknown";
    }

    public String getUserDisplayNameById(Integer userId) {
        if (userId == null) {
            return "";
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_DISPLAY_NAME_BY_ID_SQL)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("Name");

                    if (name != null && !name.isBlank()) {
                        return name.trim();
                    }

                    String email = resultSet.getString("Email");

                    if (email != null && !email.isBlank()) {
                        return email.trim();
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("Could not load display name for userId: {}", userId, e);
        }

        return "";
    }

    private UserMfaState mapUserMfaState(ResultSet resultSet) throws SQLException {
        Integer userId = resultSet.getInt("UserId");
        String email = resultSet.getString("Email");
        boolean mfaEnabled = resultSet.getBoolean("MfaEnabled");
        boolean mfaVerified = resultSet.getBoolean("MfaVerified");
        String mfaSecretEncrypted = resultSet.getString("MfaSecretEncrypted");
        MfaConfig.UserMfaPolicy userMfaPolicy = parseUserMfaPolicy(resultSet.getString("UserMfaPolicy"));
        boolean mfaResetRequired = resultSet.getBoolean("MfaResetRequired");

        return new UserMfaState(
                userId,
                email,
                mfaEnabled,
                mfaVerified,
                mfaSecretEncrypted,
                userMfaPolicy,
                mfaResetRequired
        );
    }

    private MfaConfig.UserMfaPolicy parseUserMfaPolicy(String value) {
        if (value == null || value.isBlank()) {
            return MfaConfig.UserMfaPolicy.DEFAULT;
        }

        try {
            return MfaConfig.UserMfaPolicy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown UserMfaPolicy value: {}. Falling back to DEFAULT.", value);
            return MfaConfig.UserMfaPolicy.DEFAULT;
        }
    }

    private MfaConfig.CustomerMfaPolicy parseCustomerMfaPolicy(String value) {
        if (value == null || value.isBlank()) {
            return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        }

        try {
            return MfaConfig.CustomerMfaPolicy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown CustomerMfaPolicy value: {}. Falling back to OPTIONAL.", value);
            return MfaConfig.CustomerMfaPolicy.OPTIONAL;
        }
    }

    private String getActorEmail() {
        WebSession webSession = getWebSession();

        if (webSession == null || webSession.getUserId() == null) {
            return "system";
        }

        return getEmailByUserId(webSession.getUserId());
    }

    private void logMfaAuditEvent(
            String actorEmail,
            String eventType,
            String targetUserEmail,
            String description,
            String status
    ) {
        AuditEventProvider auditEventProvider = new AuditEventProvider(getWebSession());

        auditEventProvider.logMfaEvent(
                safeText(actorEmail, "system"),
                eventType,
                safeText(targetUserEmail, "unknown"),
                description,
                status
        );
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public record LoginValidationResult(
            boolean valid,
            Integer userId,
            String failureReason
    ) {
        public static LoginValidationResult success(Integer userId) {
            return new LoginValidationResult(
                    true,
                    userId,
                    null
            );
        }

        public static LoginValidationResult failed(Integer userId, String failureReason) {
            return new LoginValidationResult(
                    false,
                    userId,
                    failureReason
            );
        }
    }

    public record UserMfaState(
            Integer userId,
            String email,
            boolean mfaEnabled,
            boolean mfaVerified,
            String mfaSecretEncrypted,
            MfaConfig.UserMfaPolicy userMfaPolicy,
            boolean mfaResetRequired
    ) {
        public boolean hasMfaSecret() {
            return mfaSecretEncrypted != null && !mfaSecretEncrypted.isBlank();
        }

        public boolean isMfaConfigured() {
            return mfaEnabled
                    && mfaVerified
                    && hasMfaSecret()
                    && !mfaResetRequired;
        }

        public boolean requiresMfaSetup() {
            return !isMfaConfigured();
        }
    }
}