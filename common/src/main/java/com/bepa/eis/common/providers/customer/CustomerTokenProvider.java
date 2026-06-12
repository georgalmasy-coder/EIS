package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerToken;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerTokenType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Base64;

public class CustomerTokenProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerTokenProvider.class);

    private static final int DEFAULT_TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String INSERT_TOKEN_SQL =
            "INSERT INTO [dbo].[CUSTOMER_TOKEN] ( " +
                    "CustomerId, " +
                    "WorkflowId, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "TokenType, " +
                    "TokenHash, " +
                    "ExpiresAt, " +
                    "CreatedByUserId " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_TOKEN_BY_HASH_SQL =
            "SELECT " +
                    "TokenId, " +
                    "CustomerId, " +
                    "WorkflowId, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "TokenType, " +
                    "TokenHash, " +
                    "ExpiresAt, " +
                    "UsedAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId " +
                    "FROM [dbo].[CUSTOMER_TOKEN] " +
                    "WHERE TokenHash = ? ";

    private static final String SELECT_TOKEN_BY_ID_SQL =
            "SELECT " +
                    "TokenId, " +
                    "CustomerId, " +
                    "WorkflowId, " +
                    "SubscriptionId, " +
                    "PaymentId, " +
                    "TokenType, " +
                    "TokenHash, " +
                    "ExpiresAt, " +
                    "UsedAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId " +
                    "FROM [dbo].[CUSTOMER_TOKEN] " +
                    "WHERE TokenId = ? ";

    private static final String MARK_TOKEN_USED_SQL =
            "UPDATE [dbo].[CUSTOMER_TOKEN] " +
                    "SET UsedAt = SYSUTCDATETIME() " +
                    "WHERE TokenId = ? " +
                    "  AND UsedAt IS NULL " +
                    "  AND ExpiresAt > SYSUTCDATETIME() ";

    private static final String EXPIRE_ACTIVE_TOKENS_SQL =
            "UPDATE [dbo].[CUSTOMER_TOKEN] " +
                    "SET UsedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerId = ? " +
                    "  AND TokenType = ? " +
                    "  AND UsedAt IS NULL ";

    public CustomerTokenProvider(WebSession webSession) {
        super(webSession);
    }

    public CreatedCustomerToken createToken(
            Integer customerId,
            Integer workflowId,
            Integer subscriptionId,
            Integer paymentId,
            CustomerTokenType tokenType,
            Timestamp expiresAt,
            Integer createdByUserId
    ) {
        if (customerId == null || expiresAt == null) {
            return null;
        }

        CustomerTokenType safeTokenType = tokenType == null
                ? CustomerTokenType.EMAIL_CONFIRMATION
                : tokenType;

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        CustomerToken token = new CustomerToken();
        token.setCustomerId(customerId);
        token.setWorkflowId(workflowId);
        token.setSubscriptionId(subscriptionId);
        token.setPaymentId(paymentId);
        token.setTokenType(safeTokenType);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setCreatedByUserId(createdByUserId);

        Integer tokenId = insertToken(token);

        if (tokenId == null) {
            return null;
        }

        return new CreatedCustomerToken(tokenId, rawToken, tokenHash, safeTokenType, expiresAt);
    }

    public Integer insertToken(CustomerToken token) {
        if (token == null || token.getCustomerId() == null || token.getExpiresAt() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_TOKEN_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, token.getCustomerId());
            setNullableInt(statement, 2, token.getWorkflowId());
            setNullableInt(statement, 3, token.getSubscriptionId());
            setNullableInt(statement, 4, token.getPaymentId());
            statement.setString(5, token.getTokenTypeCode());
            statement.setString(6, safeText(token.getTokenHash(), ""));
            statement.setTimestamp(7, token.getExpiresAt());

            if (token.getCreatedByUserId() == null) {
                statement.setNull(8, Types.INTEGER);
            } else {
                statement.setInt(8, token.getCreatedByUserId());
            }

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer tokenId = generatedKeys.getInt(1);
                    token.setTokenId(tokenId);
                    return tokenId;
                }
            }
        } catch (SQLException e) {
            log.error(
                    "Error creating customer token. customerId={}, tokenType={}",
                    token.getCustomerId(),
                    token.getTokenTypeCode(),
                    e
            );
        }

        return null;
    }

    public CustomerToken getTokenById(Integer tokenId) {
        if (tokenId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_TOKEN_BY_ID_SQL)) {

            statement.setInt(1, tokenId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapToken(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer token. tokenId={}", tokenId, e);
        }

        return null;
    }

    public CustomerToken getTokenByRawToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        return getTokenByHash(tokenHash);
    }

    public CustomerToken getTokenByHash(String tokenHash) {
        if (tokenHash == null || tokenHash.trim().isEmpty()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_TOKEN_BY_HASH_SQL)) {

            statement.setString(1, tokenHash.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapToken(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer token by hash.", e);
        }

        return null;
    }

    public boolean markTokenUsed(Integer tokenId) {
        if (tokenId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_TOKEN_USED_SQL)) {

            statement.setInt(1, tokenId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking customer token as used. tokenId={}", tokenId, e);
            return false;
        }
    }

    public int expireActiveTokens(
            Integer customerId,
            CustomerTokenType tokenType
    ) {
        if (customerId == null || tokenType == null) {
            return 0;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(EXPIRE_ACTIVE_TOKENS_SQL)) {

            statement.setInt(1, customerId);
            statement.setString(2, tokenType.getCode());

            return statement.executeUpdate();
        } catch (SQLException e) {
            log.error(
                    "Error expiring active customer tokens. customerId={}, tokenType={}",
                    customerId,
                    tokenType,
                    e
            );
            return 0;
        }
    }

    public boolean isValidRawToken(
            String rawToken,
            CustomerTokenType expectedTokenType
    ) {
        CustomerToken token = getTokenByRawToken(rawToken);

        if (token == null) {
            return false;
        }

        if (expectedTokenType != null && token.getTokenType() != expectedTokenType) {
            return false;
        }

        return token.isValid(new Timestamp(System.currentTimeMillis()));
    }

    public static String generateRawToken() {
        byte[] bytes = new byte[DEFAULT_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash customer token.", e);
        }
    }

    private CustomerToken mapToken(ResultSet resultSet) throws SQLException {
        CustomerToken token = new CustomerToken();

        int tokenId = resultSet.getInt("TokenId");
        token.setTokenId(resultSet.wasNull() ? null : tokenId);

        int customerId = resultSet.getInt("CustomerId");
        token.setCustomerId(resultSet.wasNull() ? null : customerId);

        int workflowId = resultSet.getInt("WorkflowId");
        token.setWorkflowId(resultSet.wasNull() ? null : workflowId);

        int subscriptionId = resultSet.getInt("SubscriptionId");
        token.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        int paymentId = resultSet.getInt("PaymentId");
        token.setPaymentId(resultSet.wasNull() ? null : paymentId);

        token.setTokenTypeCode(resultSet.getString("TokenType"));
        token.setTokenHash(resultSet.getString("TokenHash"));
        token.setExpiresAt(resultSet.getTimestamp("ExpiresAt"));
        token.setUsedAt(resultSet.getTimestamp("UsedAt"));
        token.setCreatedAt(resultSet.getTimestamp("CreatedAt"));

        int createdByUserId = resultSet.getInt("CreatedByUserId");
        token.setCreatedByUserId(resultSet.wasNull() ? null : createdByUserId);

        return token;
    }

    private void setNullableInt(
            PreparedStatement statement,
            int parameterIndex,
            Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.INTEGER);
            return;
        }

        statement.setInt(parameterIndex, value);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    public static class CreatedCustomerToken {

        private final Integer tokenId;
        private final String rawToken;
        private final String tokenHash;
        private final CustomerTokenType tokenType;
        private final Timestamp expiresAt;

        public CreatedCustomerToken(
                Integer tokenId,
                String rawToken,
                String tokenHash,
                CustomerTokenType tokenType,
                Timestamp expiresAt
        ) {
            this.tokenId = tokenId;
            this.rawToken = rawToken == null ? "" : rawToken;
            this.tokenHash = tokenHash == null ? "" : tokenHash;
            this.tokenType = tokenType == null ? CustomerTokenType.EMAIL_CONFIRMATION : tokenType;
            this.expiresAt = expiresAt;
        }

        public Integer getTokenId() {
            return tokenId;
        }

        public String getRawToken() {
            return rawToken;
        }

        public String getTokenHash() {
            return tokenHash;
        }

        public CustomerTokenType getTokenType() {
            return tokenType;
        }

        public Timestamp getExpiresAt() {
            return expiresAt;
        }
    }
}