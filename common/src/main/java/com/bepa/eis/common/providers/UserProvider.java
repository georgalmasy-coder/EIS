package com.bepa.eis.common.providers;

/**
 *
 Below is a method-by-method description intended for a future **User Management / MFA administration dialog**.

 ---

 ## `resetMfa(Integer userId)`

 Resets MFA for the selected user using the current/session actor or system as the actor.
 Use this when an administrator wants to force the user to set up MFA again at next login.

 Typically used alone. Do **not** call `disableMfaForUser(...)` immediately after, because reset already clears the existing MFA setup and marks setup as required.

 ---

 ## `resetMfa(Integer userId, Integer resetByUserId)`

 Resets MFA for a selected user and explicitly records which administrator performed the reset.
 Use this from an admin dialog when the acting admin user ID is known.

 Typically used alone. The next user login should trigger MFA setup again if MFA is required globally or by user policy.

 ---

 ## `resetMfaByEmail(String email, Integer resetByUserId)`

 Same as `resetMfa(...)`, but looks up the user by email instead of user ID.
 Useful for admin tools where the selected user is represented by email rather than internal ID.

 This is an alternative to `resetMfa(userId, resetByUserId)`, not something that should be called after it.

 ---

 ## `enableMfaAfterSetup(Integer userId, String encryptedSecret)`

 Marks MFA as enabled and verified after the user has successfully completed authenticator setup.
 This should normally be called by the login/MFA setup flow, not manually from an admin dialog.

 This depends on a valid generated TOTP secret. It should be called only after the submitted authenticator code has been verified.

 ---

 ## `updateMfaLastVerified(Integer userId)`

 Updates the timestamp for the latest successful MFA verification.
 Use this after a user successfully enters a valid MFA code during login.

 This should be called after successful TOTP verification. It does not enable or reset MFA.

 ---

 ## `disableMfaForUser(Integer userId)`

 Completely disables MFA for the user and clears the stored MFA secret.
 Use this only if an administrator explicitly wants the user to no longer use MFA.

 Do not call this directly after `resetMfa(...)`. Reset means “set it up again”; disable means “do not use MFA”.

 ---

 ## `enableMfaRequiredForUser(Integer userId)`

 Sets the user-level MFA policy to `REQUIRED`.
 Use this when an administrator wants this specific user to always use MFA, regardless of optional global settings.

 This may be followed by `resetMfa(...)` only if the administrator also wants to force a fresh authenticator setup.

 ---

 ## `setUserMfaPolicyToDefault(Integer userId)`

 Sets the user MFA policy back to `DEFAULT`, meaning the user follows the global MFA configuration.
 Use this when removing a user-specific MFA override.

 Usually used alone. Whether MFA is required afterwards depends on the global MFA mode and whether the user already has MFA enabled.

 ---

 ## `setUserMfaPolicyByEmail(String email, MfaConfig.UserMfaPolicy policy)`

 Changes the user MFA policy by looking up the user via email.
 Useful for admin tools or batch operations where email is the primary identifier.

 This is an alternative to `setUserMfaPolicy(userId, policy)`, not something that should be called after it.

 ---

 ## `setUserMfaPolicy(Integer userId, MfaConfig.UserMfaPolicy policy)`

 Sets the user’s MFA policy to `DEFAULT`, `REQUIRED`, or `DISABLED`.
 Use this from the admin dialog when changing how MFA should apply to a specific user.

 If setting policy to `REQUIRED`, optionally call `resetMfa(...)` afterwards if the user must set up a new authenticator.

 ---

 ## `markMfaResetRequired(Integer userId, Integer resetByUserId)`

 Marks the user as requiring MFA reset, without necessarily clearing all MFA fields.
 Use this if you want to flag that the current MFA setup is no longer trusted and must be renewed.

 In most admin cases, `resetMfa(...)` is simpler and clearer. Use this only if you need a softer reset workflow.

 ---

 ## `clearMfaResetRequired(Integer userId)`

 Clears the MFA reset-required flag for the user.
 Use this if an administrator wants to cancel a previously requested MFA reset.

 This should not normally be called after `resetMfa(...)`, unless an admin explicitly decides that the reset requirement should be withdrawn.

 ---

 ## `isMfaConfigured(Integer userId)`

 Returns whether the user currently has a complete and verified MFA setup.
 Use this to decide what status to show in the user-management UI, such as “Configured” or “Not configured”.

 This is read-only and can be called whenever the dialog loads or refreshes.

 ---

 ## `isMfaRequiredForUser(Integer userId)`

 Returns whether MFA is effectively required for the user based on global configuration, user policy, and user MFA state.
 Use this to show whether the user will be asked for MFA at next login.

 This is read-only and useful together with `isMfaConfigured(...)` to distinguish “Required but not configured” from “Required and ready”.

 ---

 ## `getUserIdByEmail(String email)`

 Looks up the internal user ID for a given email address.
 Use this when the admin UI works with email but backend operations require `userId`.

 This is normally called before user-ID based methods, unless the selected user already has `userId` available.

 ---

 ## `getEmailByUserId(Integer userId)`

 Looks up the email address for a given internal user ID.
 Use this for display, audit logging, or confirmation messages.

 This is read-only and can be used before audit-sensitive operations to show exactly which user will be affected.

 ---

 ## `getUserDisplayNameById(Integer userId)`

 Looks up a display-friendly user name for a given internal user ID.
 Returns the user's name when available, otherwise email, otherwise an empty string.

 This is read-only and is useful in administration screens where technical IDs should not be shown directly.

 ---

 ## Suggested admin dialog actions

 Typical administrator actions could map like this:

 | Admin action | Method |
 |---|---|
 | Require MFA for user | `enableMfaRequiredForUser(userId)` |
 | Reset user MFA | `resetMfa(userId, adminUserId)` |
 | Disable MFA for user | `disableMfaForUser(userId)` |
 | Follow global MFA policy | `setUserMfaPolicyToDefault(userId)` |
 | Change MFA policy manually | `setUserMfaPolicy(userId, policy)` |
 | Show MFA status | `isMfaConfigured(userId)` + `isMfaRequiredForUser(userId)` |

 */

import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.common.providers.security.MfaConfig;
import com.bepa.eis.common.dto.WebSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        UserMfaState userMfaState = getUserMfaStateByUserId(userId);

        if (userMfaState == null) {
            return false;
        }

        return MfaConfig.isMfaRequired(
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