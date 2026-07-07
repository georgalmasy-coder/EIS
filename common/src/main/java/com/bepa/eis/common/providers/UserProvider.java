package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.mail.MailTemplateType;
import com.bepa.eis.common.enums.project.ProjectStatus;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.providers.customer.CustomerTokenProvider;
import com.bepa.eis.common.providers.security.MfaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private static final String SELECT_ADMIN_USERS_SQL = """
            SELECT
                U.UserId,
                U.Initials,
                U.Name,
                U.Email,
                U.Phone,
                U.DepartmentId,
                U.Active,
                U.UserRole,
                U.LockedUntil,
                U.MfaEnabled,
                U.MfaVerified,
                U.MfaSecretEncrypted,
                U.UserMfaPolicy,
                U.MfaResetRequired,
                U.MfaResetAt,
                U.MfaResetByUserId,
                U.Password,
                LastLogin.LastLoginAt,
                D.DepartmentName,
                D.DepartmentDescription,
                ISNULL(Cust.CustomerNames, '') AS CustomerNames
            FROM [dbo].[USERS] U
            LEFT JOIN [dbo].[DEPARTMENT] D
                ON D.DepartmentId = U.DepartmentId
            OUTER APPLY (
                SELECT TOP (1)
                    L.LoginTime AS LastLoginAt
                FROM [dbo].[USER_LOGIN_ACTIVITY] L
                WHERE L.UserId = U.UserId
                  AND L.Success = 1
                ORDER BY L.LoginTime DESC
            ) LastLogin
            OUTER APPLY (
                SELECT STRING_AGG(X.CustomerName, ', ') AS CustomerNames
                FROM (
                    SELECT DISTINCT C.CustomerName
                    FROM [dbo].[USER_CUSTOMER] UC
                    INNER JOIN [dbo].[CUSTOMER] C
                        ON C.CustomerId = UC.CustomerId
                       AND C.Latest = 1
                    WHERE UC.UserId = U.UserId
                ) X
            ) Cust
            ORDER BY U.Name, U.Email, U.UserId
            """;

    private static final String SELECT_USER_MAIN_ROWS_SQL = """
            SELECT
                U.UserId,
                U.Initials,
                U.Name,
                U.Email,
                U.Phone,
                U.DepartmentId,
                U.Active,
                U.UserRole,
                U.LockedUntil,
                U.MfaEnabled,
                U.MfaVerified,
                U.MfaSecretEncrypted,
                U.UserMfaPolicy,
                U.MfaResetRequired,
                U.MfaResetAt,
                U.MfaResetByUserId,
                U.Password,
                LastLogin.LastLoginAt,
                D.DepartmentName,
                D.DepartmentDescription,
                '' AS CustomerNames
            FROM [dbo].[USERS] U
            LEFT JOIN [dbo].[DEPARTMENT] D
                ON D.DepartmentId = U.DepartmentId
            OUTER APPLY (
                SELECT TOP (1)
                    L.LoginTime AS LastLoginAt
                FROM [dbo].[USER_LOGIN_ACTIVITY] L
                WHERE L.UserId = U.UserId
                  AND L.Success = 1
                ORDER BY L.LoginTime DESC
            ) LastLogin
            WHERE EXISTS (
                SELECT 1
                FROM [dbo].[USER_CUSTOMER] UC
                WHERE UC.UserId = U.UserId
                  AND UC.CustomerId = ?
            )
            ORDER BY U.Name, U.Email, U.UserId
            """;

    private static final String SELECT_ADMIN_USER_DETAIL_SQL = """
            SELECT
                U.UserId,
                U.Initials,
                U.Name,
                U.Email,
                U.Phone,
                U.DepartmentId,
                U.Active,
                U.UserRole,
                U.LockedUntil,
                U.MfaEnabled,
                U.MfaVerified,
                U.MfaSecretEncrypted,
                U.UserMfaPolicy,
                U.MfaResetRequired,
                U.MfaResetAt,
                U.MfaResetByUserId,
                U.Password,
                LastLogin.LastLoginAt,
                D.DepartmentName,
                D.DepartmentDescription,
                ISNULL(Cust.CustomerNames, '') AS CustomerNames
            FROM [dbo].[USERS] U
            LEFT JOIN [dbo].[DEPARTMENT] D
                ON D.DepartmentId = U.DepartmentId
            OUTER APPLY (
                SELECT TOP (1)
                    L.LoginTime AS LastLoginAt
                FROM [dbo].[USER_LOGIN_ACTIVITY] L
                WHERE L.UserId = U.UserId
                  AND L.Success = 1
                ORDER BY L.LoginTime DESC
            ) LastLogin
            OUTER APPLY (
                SELECT STRING_AGG(X.CustomerName, ', ') AS CustomerNames
                FROM (
                    SELECT DISTINCT C.CustomerName
                    FROM [dbo].[USER_CUSTOMER] UC
                    INNER JOIN [dbo].[CUSTOMER] C
                        ON C.CustomerId = UC.CustomerId
                       AND C.Latest = 1
                    WHERE UC.UserId = U.UserId
                ) X
            ) Cust
            WHERE U.UserId = ?
            """;

    private static final String SELECT_ADMIN_USER_CUSTOMERS_SQL = """
            SELECT
                C.CustomerId,
                C.CustomerName,
                C.Country,
                C.CustomerStatus,
                C.ContactEmail
            FROM [dbo].[USER_CUSTOMER] UC
            INNER JOIN [dbo].[CUSTOMER] C
                ON C.CustomerId = UC.CustomerId
               AND C.Latest = 1
            WHERE UC.UserId = ?
            ORDER BY C.CustomerName, C.CustomerId
            """;

    private static final String SELECT_ALL_DEPARTMENTS_SQL = """
            SELECT
                D.DepartmentId,
                D.CustomerId,
                D.DepartmentName,
                D.DepartmentDescription,
                D.Active,
                ISNULL(C.CustomerName, '') AS CustomerName
            FROM [dbo].[DEPARTMENT] D
            LEFT JOIN [dbo].[CUSTOMER] C
                ON C.CustomerId = D.CustomerId
               AND C.Latest = 1
            ORDER BY C.CustomerName, D.DepartmentName, D.DepartmentId
            """;

    private static final String INSERT_USER_SQL = """
            INSERT INTO [dbo].[USERS] (
                Initials,
                Name,
                Email,
                Phone,
                DepartmentId,
                Active,
                UserRole,
                FailedLoginCount,
                MfaEnabled,
                MfaVerified,
                MfaResetRequired,
                UserMfaPolicy,
                Password,
                LockedUntil
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_USER_SQL = """
            UPDATE [dbo].[USERS]
            SET
                Initials = ?,
                Name = ?,
                Email = ?,
                Phone = ?,
                DepartmentId = ?,
                Active = ?,
                UserRole = ?,
                UserMfaPolicy = ?,
                LockedUntil = ?
            WHERE UserId = ?
            """;

    private static final String SELECT_EMAIL_BY_USER_ID_FOR_UPDATE_SQL = """
            SELECT Email
            FROM [dbo].[USERS]
            WHERE UserId = ?
            """;

    private static final String SELECT_ACTIVE_USER_BY_EMAIL_SQL = """
            SELECT TOP (1)
                UserId
            FROM [dbo].[USERS]
            WHERE Active = 1
              AND LOWER(Email) = ?
              AND UserId <> ?
            """;

    private static final String UPDATE_USER_PASSWORD_SQL = """
            UPDATE [dbo].[USERS]
            SET
                Password = ?,
                LockedUntil = NULL
            WHERE UserId = ?
            """;

    private static final String DELETE_USER_CUSTOMERS_SQL = """
            DELETE FROM [dbo].[USER_CUSTOMER]
            WHERE UserId = ?
            """;

    private static final String INSERT_USER_CUSTOMER_SQL = """
            INSERT INTO [dbo].[USER_CUSTOMER] (
                UserId,
                CustomerId
            )
            VALUES (?, ?)
            """;

    private static final String UPDATE_USER_MFA_POLICY_SQL = """
            UPDATE [dbo].[USERS]
            SET UserMfaPolicy = ?
            WHERE UserId = ?
            """;

    private static final String SELECT_PASSWORD_RESET_TOKEN_BY_HASH_SQL = """
            SELECT
                TokenId,
                UserId,
                TokenHash,
                ExpiresAt,
                UsedAt,
                CreatedAt,
                CreatedByUserId
            FROM [dbo].[USER_PASSWORD_RESET_TOKEN]
            WHERE TokenHash = ?
            """;

    private static final String INSERT_PASSWORD_RESET_TOKEN_SQL = """
            INSERT INTO [dbo].[USER_PASSWORD_RESET_TOKEN] (
                UserId,
                TokenHash,
                ExpiresAt,
                CreatedByUserId
            )
            VALUES (?, ?, ?, ?)
            """;

    private static final String MARK_PASSWORD_RESET_TOKEN_USED_SQL = """
            UPDATE [dbo].[USER_PASSWORD_RESET_TOKEN]
            SET UsedAt = SYSUTCDATETIME()
            WHERE TokenId = ?
              AND UsedAt IS NULL
              AND ExpiresAt > SYSUTCDATETIME()
            """;

    private static final String EXPIRE_ACTIVE_PASSWORD_RESET_TOKENS_SQL = """
            UPDATE [dbo].[USER_PASSWORD_RESET_TOKEN]
            SET UsedAt = SYSUTCDATETIME()
            WHERE UserId = ?
              AND UsedAt IS NULL
            """;

    private static final String UPDATE_PASSWORD_BY_RESET_TOKEN_SQL = """
            UPDATE U
            SET
                U.Password = ?,
                U.LockedUntil = NULL
            FROM [dbo].[USERS] U
            INNER JOIN [dbo].[USER_PASSWORD_RESET_TOKEN] T
                ON T.UserId = U.UserId
            WHERE T.TokenHash = ?
              AND T.UsedAt IS NULL
              AND T.ExpiresAt > SYSUTCDATETIME()
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

    private static final String CUSTOMER_BY_USER_ID_SQL =
            "SELECT C.CustomerId, C.CustomerName, C.CustomerStatus, C.CustomerMfaPolicy " +
                    "FROM CUSTOMER C " +
                    "WHERE C.Latest = 1 " +
                    "AND C.CustomerStatus IN (" + CustomerStatus.getActiveStatusIds() + ") " +
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

    public List<UserAdministrationRow> getUserAdministrationRows() {
        List<UserAdministrationRow> rows = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ADMIN_USERS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapUserAdministrationRow(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error loading user administration rows.", e);
        }

        return rows;
    }

    public List<UserAdministrationRow> getUserMainRows(Integer customerId) {
        List<UserAdministrationRow> rows = new ArrayList<>();

        if (customerId == null) {
            return rows;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_MAIN_ROWS_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapUserAdministrationRow(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading user main rows. customerId={}", customerId, e);
        }

        return rows;
    }

    public UserAdministrationRow getUserAdministrationRow(Integer userId) {
        if (userId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ADMIN_USER_DETAIL_SQL)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUserAdministrationRow(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading user administration row. userId={}", userId, e);
        }

        return null;
    }

    public List<UserCustomerLink> getLinkedCustomers(Integer userId) {
        List<UserCustomerLink> links = new ArrayList<>();

        if (userId == null) {
            return links;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ADMIN_USER_CUSTOMERS_SQL)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    links.add(new UserCustomerLink(
                            resultSet.getInt("CustomerId"),
                            safeText(resultSet.getString("CustomerName"), ""),
                            safeText(resultSet.getString("Country"), ""),
                            safeText(resultSet.getString("CustomerStatus"), ""),
                            safeText(resultSet.getString("ContactEmail"), "")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading linked customers. userId={}", userId, e);
        }

        return links;
    }

    public List<DepartmentOption> getDepartmentOptions() {
        List<DepartmentOption> departments = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_DEPARTMENTS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                departments.add(new DepartmentOption(
                        resultSet.getInt("DepartmentId"),
                        resultSet.getInt("CustomerId"),
                        safeText(resultSet.getString("CustomerName"), ""),
                        safeText(resultSet.getString("DepartmentName"), ""),
                        safeText(resultSet.getString("DepartmentDescription"), ""),
                        resultSet.getBoolean("Active")
                ));
            }
        } catch (SQLException e) {
            log.error("Error loading department options.", e);
        }

        return departments;
    }

    public boolean saveUserAdministration(
            UserAdministrationRow user,
            List<Integer> customerIds
    ) {
        if (user == null) {
            return false;
        }

        if (safeText(user.name(), "").isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        if (normalizeEmail(user.email()).isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateEmailChange(connection, user);

                List<Integer> resolvedCustomerIds = resolveCustomerIdsForSave(connection, user.userId(), customerIds);

                Integer persistedUserId;

                if (user.userId() == null) {
                    persistedUserId = createUserAdministration(connection, user);

                    if (persistedUserId == null) {
                        connection.rollback();
                        return false;
                    }
                } else {
                    if (!updateUserAdministration(connection, user)) {
                        connection.rollback();
                        return false;
                    }

                    persistedUserId = user.userId();
                }

                replaceUserCustomers(connection, persistedUserId, resolvedCustomerIds);

                connection.commit();

                logUserAdministrationEvent(
                        user.userId() == null ? "USER_ADMIN_CREATED" : "USER_ADMIN_UPDATED",
                        user.email(),
                        "User administration data saved",
                        "OK"
                );

                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error saving user administration data. userId={}", user.userId(), e);
            logUserAdministrationEvent(
                    "USER_ADMIN_SAVE_FAILED",
                    user.email(),
                    "User administration data could not be saved",
                    "Warning"
            );
            return false;
        }
    }

    private void validateEmailChange(
            Connection connection,
            UserAdministrationRow user
    ) throws SQLException {
        if (connection == null || user == null || user.userId() == null) {
            String newEmail = normalizeEmail(user == null ? null : user.email());

            if (!newEmail.isBlank() && hasActiveUserWithEmail(connection, newEmail, -1)) {
                throw new IllegalStateException("Another active user already uses this email.");
            }

            return;
        }

        String currentEmail = getEmailByUserId(connection, user.userId());
        String newEmail = normalizeEmail(user.email());

        if (newEmail.isBlank()) {
            return;
        }

        if (!normalizeEmail(currentEmail).equals(newEmail)) {
            if (hasActiveUserWithEmail(connection, newEmail, user.userId())) {
                throw new IllegalStateException("Another active user already uses this email.");
            }
        }
    }

    private boolean hasActiveUserWithEmail(
            Connection connection,
            String normalizedEmail,
            Integer excludedUserId
    ) throws SQLException {
        if (connection == null || normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_USER_BY_EMAIL_SQL)) {
            statement.setString(1, normalizedEmail);
            statement.setInt(2, excludedUserId == null ? -1 : excludedUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Integer createUserAdministration(
            Connection connection,
            UserAdministrationRow user
    ) throws SQLException {
        if (connection == null || user == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_USER_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setString(1, initialsForUser(user));
            statement.setString(2, safeText(user.name(), "").trim());
            statement.setString(3, normalizeEmail(user.email()));
            statement.setString(4, blankToNull(user.phone()));

            if (user.departmentId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, user.departmentId());
            }

            statement.setBoolean(6, user.active());
            statement.setInt(7, user.userRole() == null ? UserRoles.CUSTOMER_ADMINISTRATOR.getId() : user.userRole().getId());
            statement.setInt(8, 0);
            statement.setBoolean(9, false);
            statement.setBoolean(10, false);
            statement.setBoolean(11, false);
            statement.setString(12, normalizeUserMfaPolicy(user.userMfaPolicy()));
            statement.setString(13, "");

            if (user.lockedUntil() == null) {
                statement.setNull(14, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(14, user.lockedUntil());
            }

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return null;
    }

    private List<Integer> resolveCustomerIdsForSave(
            Connection connection,
            Integer userId,
            List<Integer> customerIds
    ) {
        java.util.LinkedHashSet<Integer> uniqueIds = new java.util.LinkedHashSet<>();

        if (customerIds != null) {
            for (Integer customerId : customerIds) {
                if (customerId != null) {
                    uniqueIds.add(customerId);
                }
            }
        }

        if (uniqueIds.isEmpty() && userId != null) {
            for (UserCustomerLink link : getLinkedCustomers(userId)) {
                if (link != null && link.customerId() != null) {
                    uniqueIds.add(link.customerId());
                }
            }
        }

        if (uniqueIds.isEmpty() && getWebSession() != null && getWebSession().getCustomerId() != null) {
            uniqueIds.add(getWebSession().getCustomerId());
        }

        return new ArrayList<>(uniqueIds);
    }

    private String initialsForUser(UserAdministrationRow user) {
        String initials = safeText(user == null ? null : user.initials(), "").trim();

        if (!initials.isBlank()) {
            return initials;
        }

        String name = safeText(user == null ? null : user.name(), "").trim();

        if (name.isBlank()) {
            return "";
        }

        String[] parts = name.split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (builder.length() >= 3) {
                break;
            }
        }

        return builder.toString();
    }

    private String normalizeUserMfaPolicy(String value) {
        if (value == null || value.isBlank()) {
            return MfaConfig.UserMfaPolicy.DEFAULT.name();
        }

        try {
            return MfaConfig.UserMfaPolicy.valueOf(value.trim().toUpperCase(Locale.ENGLISH)).name();
        } catch (IllegalArgumentException e) {
            return MfaConfig.UserMfaPolicy.DEFAULT.name();
        }
    }

    private String getEmailByUserId(
            Connection connection,
            Integer userId
    ) throws SQLException {
        if (connection == null || userId == null) {
            return "";
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_EMAIL_BY_USER_ID_FOR_UPDATE_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return safeText(resultSet.getString("Email"), "");
                }
            }
        }

        return "";
    }

    public boolean setUserPassword(
            Integer userId,
            String newPassword
    ) {
        if (userId == null || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_USER_PASSWORD_SQL)) {

            statement.setString(1, newPassword);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating password. userId={}", userId, e);
            return false;
        }
    }

    public PasswordResetTokenResult createPasswordResetToken(
            Integer userId,
            Integer createdByUserId,
            String baseUrl
    ) {
        if (userId == null) {
            return null;
        }

        expireActivePasswordResetTokens(userId);

        String rawToken = CustomerTokenProvider.generateRawToken();
        String tokenHash = CustomerTokenProvider.hashToken(rawToken);
        Timestamp expiresAt = Timestamp.from(java.time.Instant.now().plus(java.time.Duration.ofHours(24)));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_PASSWORD_RESET_TOKEN_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, expiresAt);

            if (createdByUserId == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, createdByUserId);
            }

            int updated = statement.executeUpdate();

            if (updated == 0) {
                return null;
            }

            Integer tokenId = null;

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tokenId = generatedKeys.getInt(1);
                }
            }

            String resetBaseUrl = safeText(baseUrl, "").replaceAll("/+$", "");
            String resetLink = resetBaseUrl + "/forgot-password.html?token=" + urlEncode(rawToken);

            return new PasswordResetTokenResult(
                    tokenId,
                    userId,
                    rawToken,
                    tokenHash,
                    expiresAt,
                    null,
                    null,
                    createdByUserId,
                    resetLink
            );
        } catch (SQLException e) {
            log.error("Error creating password reset token. userId={}", userId, e);
            return null;
        }
    }

    public boolean sendPasswordResetLink(
            Integer userId,
            Integer createdByUserId,
            String baseUrl
    ) {
        UserAdministrationRow user = getUserAdministrationRow(userId);

        if (user == null || user.email() == null || user.email().isBlank()) {
            return false;
        }

        PasswordResetTokenResult tokenResult = createPasswordResetToken(userId, createdByUserId, baseUrl);

        if (tokenResult == null) {
            return false;
        }

        MailProvider mailProvider = new MailProvider(getWebSession());
        boolean queued = mailProvider.createMail(
                MailRecipient.of(user.name(), user.email()),
                MailTemplateType.PASSWORD_RESET,
                java.util.Map.of(
                        "userName", safeText(user.name(), user.email()),
                        "email", safeText(user.email(), ""),
                        "resetLink", tokenResult.resetLink()
                )
        ) != null;

        logUserAdministrationEvent(
                queued ? "PASSWORD_RESET_LINK_QUEUED" : "PASSWORD_RESET_LINK_FAILED",
                user.email(),
                queued ? "Password reset link queued" : "Password reset link could not be queued",
                queued ? "OK" : "Warning"
        );

        return queued;
    }

    public boolean validatePasswordResetToken(String rawToken) {
        return getPasswordResetToken(rawToken) != null;
    }

    public boolean completePasswordReset(
            String rawToken,
            String newPassword
    ) {
        if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        PasswordResetTokenResult token = getPasswordResetToken(rawToken);

        if (token == null || token.isUsed() || token.isExpired()) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!updatePasswordByResetToken(connection, token.tokenHash(), newPassword)) {
                    connection.rollback();
                    return false;
                }

                markPasswordResetTokenUsed(connection, token.tokenId());
                connection.commit();

                logUserAdministrationEvent(
                        "PASSWORD_RESET_COMPLETED",
                        getEmailByUserId(token.userId()),
                        "Password reset completed",
                        "OK"
                );
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error completing password reset. tokenId={}", token.tokenId(), e);
            return false;
        }
    }

    private UserAdministrationRow mapUserAdministrationRow(ResultSet resultSet) throws SQLException {
        Integer userId = resultSet.getInt("UserId");
        Integer departmentId = resultSet.getObject("DepartmentId") == null ? null : resultSet.getInt("DepartmentId");
        Integer userRoleId = resultSet.getObject("UserRole") == null ? null : resultSet.getInt("UserRole");
        Integer mfaResetByUserId = resultSet.getObject("MfaResetByUserId") == null ? null : resultSet.getInt("MfaResetByUserId");

        return new UserAdministrationRow(
                userId,
                safeText(resultSet.getString("Initials"), ""),
                safeText(resultSet.getString("Name"), ""),
                safeText(resultSet.getString("Email"), ""),
                safeText(resultSet.getString("Phone"), ""),
                departmentId,
                resultSet.getBoolean("Active"),
                UserRoles.fromIdOrDefault(userRoleId, UserRoles.BEPA_SYSTEM_ADMINISTRATOR),
                resultSet.getTimestamp("LockedUntil"),
                resultSet.getBoolean("MfaEnabled"),
                resultSet.getBoolean("MfaVerified"),
                safeText(resultSet.getString("MfaSecretEncrypted"), ""),
                safeText(resultSet.getString("UserMfaPolicy"), MfaConfig.UserMfaPolicy.DEFAULT.name()),
                resultSet.getBoolean("MfaResetRequired"),
                resultSet.getTimestamp("MfaResetAt"),
                mfaResetByUserId,
                safeText(resultSet.getString("Password"), ""),
                resultSet.getTimestamp("LastLoginAt"),
                safeText(resultSet.getString("DepartmentName"), ""),
                safeText(resultSet.getString("DepartmentDescription"), ""),
                safeText(resultSet.getString("CustomerNames"), "")
        );
    }

    private boolean updateUserAdministration(
            Connection connection,
            UserAdministrationRow user
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_SQL)) {
            statement.setString(1, safeText(user.initials(), ""));
            statement.setString(2, safeText(user.name(), ""));
            statement.setString(3, safeText(user.email(), "").toLowerCase(Locale.ENGLISH));
            statement.setString(4, blankToNull(user.phone()));

            if (user.departmentId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, user.departmentId());
            }

            statement.setBoolean(6, user.active());
            statement.setInt(7, user.userRole() == null ? UserRoles.BEPA_SYSTEM_ADMINISTRATOR.getId() : user.userRole().getId());
            statement.setString(8, user.userMfaPolicy() == null || user.userMfaPolicy().isBlank()
                    ? MfaConfig.UserMfaPolicy.DEFAULT.name()
                    : user.userMfaPolicy());

            if (user.lockedUntil() == null) {
                statement.setNull(9, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(9, user.lockedUntil());
            }

            statement.setInt(10, user.userId());

            return statement.executeUpdate() > 0;
        }
    }

    private void replaceUserCustomers(
            Connection connection,
            Integer userId,
            List<Integer> customerIds
    ) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_USER_CUSTOMERS_SQL)) {
            deleteStatement.setInt(1, userId);
            deleteStatement.executeUpdate();
        }

        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_USER_CUSTOMER_SQL)) {
            for (Integer customerId : customerIds) {
                if (customerId == null) {
                    continue;
                }

                insertStatement.setInt(1, userId);
                insertStatement.setInt(2, customerId);
                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        }
    }

    private void expireActivePasswordResetTokens(Integer userId) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(EXPIRE_ACTIVE_PASSWORD_RESET_TOKENS_SQL)) {

            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.warn("Could not expire active password reset tokens. userId={}", userId, e);
        }
    }

    private PasswordResetTokenResult getPasswordResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String tokenHash = CustomerTokenProvider.hashToken(rawToken);

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PASSWORD_RESET_TOKEN_BY_HASH_SQL)) {

            statement.setString(1, tokenHash);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Integer tokenId = resultSet.getInt("TokenId");
                    Integer userId = resultSet.getInt("UserId");

                    return new PasswordResetTokenResult(
                            resultSet.wasNull() ? null : tokenId,
                            userId,
                            rawToken,
                            tokenHash,
                            resultSet.getTimestamp("ExpiresAt"),
                            resultSet.getTimestamp("UsedAt"),
                            resultSet.getTimestamp("CreatedAt"),
                            resultSet.getObject("CreatedByUserId") == null ? null : resultSet.getInt("CreatedByUserId"),
                            ""
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Error loading password reset token.", e);
        }

        return null;
    }

    private boolean markPasswordResetTokenUsed(
            Connection connection,
            Integer tokenId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(MARK_PASSWORD_RESET_TOKEN_USED_SQL)) {
            statement.setInt(1, tokenId);
            return statement.executeUpdate() > 0;
        }
    }

    private boolean updatePasswordByResetToken(
            Connection connection,
            String tokenHash,
            String newPassword
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD_BY_RESET_TOKEN_SQL)) {
            statement.setString(1, newPassword);
            statement.setString(2, tokenHash);
            return statement.executeUpdate() > 0;
        }
    }

    private void logUserAdministrationEvent(
            String eventType,
            String targetUserEmail,
            String description,
            String status
    ) {
        AuditEventProvider auditEventProvider = new AuditEventProvider(getWebSession());
        auditEventProvider.logUserEvent(
                getActorEmail(),
                eventType,
                safeText(targetUserEmail, "unknown"),
                safeText(description, eventType),
                safeText(status, "OK")
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ENGLISH);
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(safeText(value, ""), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return safeText(value, "");
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

    public record UserAdministrationRow(
            Integer userId,
            String initials,
            String name,
            String email,
            String phone,
            Integer departmentId,
            boolean active,
            UserRoles userRole,
            Timestamp lockedUntil,
            boolean mfaEnabled,
            boolean mfaVerified,
            String mfaSecretEncrypted,
            String userMfaPolicy,
            boolean mfaResetRequired,
            Timestamp mfaResetAt,
            Integer mfaResetByUserId,
            String password,
            Timestamp lastLoginAt,
            String departmentName,
            String departmentDescription,
            String customerNames
    ) {
        public boolean hasMfaSecret() {
            return mfaSecretEncrypted != null && !mfaSecretEncrypted.isBlank();
        }

        public boolean hasPassword() {
            return password != null && !password.isBlank();
        }
    }

    public record UserCustomerLink(
            Integer customerId,
            String customerName,
            String country,
            String customerStatus,
            String contactEmail
    ) {
    }

    public record DepartmentOption(
            Integer departmentId,
            Integer customerId,
            String customerName,
            String departmentName,
            String departmentDescription,
            boolean active
    ) {
        public String getDisplayName() {
            String customerPart = customerName == null || customerName.isBlank() ? "" : customerName.trim();
            String departmentPart = departmentName == null ? "" : departmentName.trim();
            String descriptionPart = departmentDescription == null || departmentDescription.isBlank()
                    ? ""
                    : departmentDescription.trim();

            StringBuilder builder = new StringBuilder();

            if (!customerPart.isBlank()) {
                builder.append(customerPart);
            }

            if (!departmentPart.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" - ");
                }
                builder.append(departmentPart);
            }

            if (!descriptionPart.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" - ");
                }
                builder.append(descriptionPart);
            }

            return builder.toString();
        }
    }

    public record PasswordResetTokenResult(
            Integer tokenId,
            Integer userId,
            String rawToken,
            String tokenHash,
            Timestamp expiresAt,
            Timestamp usedAt,
            Timestamp createdAt,
            Integer createdByUserId,
            String resetLink
    ) {
        public boolean isUsed() {
            return usedAt != null;
        }

        public boolean isExpired() {
            return expiresAt == null || !expiresAt.after(new Timestamp(System.currentTimeMillis()));
        }
    }
}
