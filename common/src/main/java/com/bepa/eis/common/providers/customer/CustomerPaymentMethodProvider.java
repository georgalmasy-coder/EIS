package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerPaymentMethod;
import com.bepa.eis.common.enums.customer.CustomerPaymentMethodStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class CustomerPaymentMethodProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerPaymentMethodProvider.class);

    private static final String INSERT_PAYMENT_METHOD_SQL =
            "INSERT INTO [dbo].[CUSTOMER_PAYMENT_METHOD] ( " +
                    "CustomerId, " +
                    "PaymentProvider, " +
                    "ProviderPaymentMethodReference, " +
                    "CardholderName, " +
                    "CardBrand, " +
                    "MaskedCardNumber, " +
                    "ExpiryMonth, " +
                    "ExpiryYear, " +
                    "BillingZipCode, " +
                    "PaymentMethodStatus, " +
                    "Latest " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_LATEST_PAYMENT_METHOD_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (1) " +
                    "CustomerPaymentMethodId, " +
                    "CustomerId, " +
                    "PaymentProvider, " +
                    "ProviderPaymentMethodReference, " +
                    "CardholderName, " +
                    "CardBrand, " +
                    "MaskedCardNumber, " +
                    "ExpiryMonth, " +
                    "ExpiryYear, " +
                    "BillingZipCode, " +
                    "PaymentMethodStatus, " +
                    "CreatedAt, " +
                    "UpdatedAt, " +
                    "Latest " +
                    "FROM [dbo].[CUSTOMER_PAYMENT_METHOD] " +
                    "WHERE CustomerId = ? " +
                    "  AND Latest = 1 " +
                    "ORDER BY CustomerPaymentMethodId DESC ";

    private static final String SELECT_PAYMENT_METHOD_BY_ID_SQL =
            "SELECT " +
                    "CustomerPaymentMethodId, " +
                    "CustomerId, " +
                    "PaymentProvider, " +
                    "ProviderPaymentMethodReference, " +
                    "CardholderName, " +
                    "CardBrand, " +
                    "MaskedCardNumber, " +
                    "ExpiryMonth, " +
                    "ExpiryYear, " +
                    "BillingZipCode, " +
                    "PaymentMethodStatus, " +
                    "CreatedAt, " +
                    "UpdatedAt, " +
                    "Latest " +
                    "FROM [dbo].[CUSTOMER_PAYMENT_METHOD] " +
                    "WHERE CustomerPaymentMethodId = ? ";

    private static final String UPDATE_LATEST_OFF_SQL =
            "UPDATE [dbo].[CUSTOMER_PAYMENT_METHOD] " +
                    "SET Latest = 0, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerId = ? " +
                    "  AND Latest = 1 ";

    private static final String UPDATE_PAYMENT_METHOD_SQL =
            "UPDATE [dbo].[CUSTOMER_PAYMENT_METHOD] " +
                    "SET PaymentProvider = ?, " +
                    "    ProviderPaymentMethodReference = ?, " +
                    "    CardholderName = ?, " +
                    "    CardBrand = ?, " +
                    "    MaskedCardNumber = ?, " +
                    "    ExpiryMonth = ?, " +
                    "    ExpiryYear = ?, " +
                    "    BillingZipCode = ?, " +
                    "    PaymentMethodStatus = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerPaymentMethodId = ? ";

    private static final String UPDATE_PAYMENT_METHOD_STATUS_SQL =
            "UPDATE [dbo].[CUSTOMER_PAYMENT_METHOD] " +
                    "SET PaymentMethodStatus = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerPaymentMethodId = ? ";

    public CustomerPaymentMethodProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createPaymentMethod(CustomerPaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.getCustomerId() == null) {
            return null;
        }

        if (!paymentMethod.hasValidPaymentProvider()) {
            log.warn("Payment method could not be created because payment provider is missing. customerId={}", paymentMethod.getCustomerId());
            return null;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                updateLatestOff(
                        connection,
                        paymentMethod.getCustomerId()
                );

                paymentMethod.setLatest(true);

                Integer paymentMethodId = insertPaymentMethod(
                        connection,
                        paymentMethod
                );

                connection.commit();

                paymentMethod.setCustomerPaymentMethodId(paymentMethodId);

                log.info(
                        "Customer payment method created. customerId={}, customerPaymentMethodId={}, provider={}, status={}",
                        paymentMethod.getCustomerId(),
                        paymentMethodId,
                        paymentMethod.getPaymentProvider(),
                        paymentMethod.getPaymentMethodStatusCode()
                );

                return paymentMethodId;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                restoreAutoCommitQuietly(connection);
            }
        } catch (SQLException e) {
            log.error("Error creating customer payment method. customerId={}", paymentMethod.getCustomerId(), e);
            return null;
        }
    }

    public CustomerPaymentMethod getLatestPaymentMethodByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_PAYMENT_METHOD_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPaymentMethod(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest payment method. customerId={}", customerId, e);
        }

        return null;
    }

    public CustomerPaymentMethod getPaymentMethodById(Integer customerPaymentMethodId) {
        if (customerPaymentMethodId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PAYMENT_METHOD_BY_ID_SQL)) {

            statement.setInt(1, customerPaymentMethodId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPaymentMethod(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading payment method. customerPaymentMethodId={}", customerPaymentMethodId, e);
        }

        return null;
    }

    public boolean updatePaymentMethod(CustomerPaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.getCustomerPaymentMethodId() == null) {
            return false;
        }

        if (!paymentMethod.hasValidPaymentProvider()) {
            log.warn(
                    "Payment method could not be updated because payment provider is missing. customerPaymentMethodId={}",
                    paymentMethod.getCustomerPaymentMethodId()
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PAYMENT_METHOD_SQL)) {

            statement.setString(1, paymentMethod.getPaymentProvider());
            statement.setString(2, nullIfBlank(paymentMethod.getProviderPaymentMethodReference()));
            statement.setString(3, nullIfBlank(paymentMethod.getCardholderName()));
            statement.setString(4, nullIfBlank(paymentMethod.getCardBrand()));
            statement.setString(5, nullIfBlank(paymentMethod.getMaskedCardNumber()));
            setNullableInt(statement, 6, paymentMethod.getExpiryMonth());
            setNullableInt(statement, 7, paymentMethod.getExpiryYear());
            statement.setString(8, nullIfBlank(paymentMethod.getBillingZipCode()));
            statement.setInt(9, paymentMethod.getPaymentMethodStatusId());
            statement.setInt(10, paymentMethod.getCustomerPaymentMethodId());

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                log.info(
                        "Customer payment method updated. customerPaymentMethodId={}, provider={}, status={}",
                        paymentMethod.getCustomerPaymentMethodId(),
                        paymentMethod.getPaymentProvider(),
                        paymentMethod.getPaymentMethodStatusCode()
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error(
                    "Error updating customer payment method. customerPaymentMethodId={}",
                    paymentMethod.getCustomerPaymentMethodId(),
                    e
            );
            return false;
        }
    }

    public boolean updatePaymentMethodStatus(
            Integer customerPaymentMethodId,
            CustomerPaymentMethodStatus paymentMethodStatus
    ) {
        if (customerPaymentMethodId == null) {
            return false;
        }

        CustomerPaymentMethodStatus safeStatus = paymentMethodStatus == null
                ? CustomerPaymentMethodStatus.ACTIVE
                : paymentMethodStatus;

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PAYMENT_METHOD_STATUS_SQL)) {

            statement.setInt(1, safeStatus.getId());
            statement.setInt(2, customerPaymentMethodId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(
                    "Error updating payment method status. customerPaymentMethodId={}, status={}",
                    customerPaymentMethodId,
                    safeStatus.getCode(),
                    e
            );
            return false;
        }
    }

    public boolean updatePaymentMethodStatus(
            Integer customerPaymentMethodId,
            Integer paymentMethodStatusId
    ) {
        CustomerPaymentMethodStatus status = CustomerPaymentMethodStatus.fromIdOrDefault(
                paymentMethodStatusId,
                CustomerPaymentMethodStatus.ACTIVE
        );

        return updatePaymentMethodStatus(
                customerPaymentMethodId,
                status
        );
    }

    private Integer insertPaymentMethod(
            Connection connection,
            CustomerPaymentMethod paymentMethod
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_PAYMENT_METHOD_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setInt(1, paymentMethod.getCustomerId());
            statement.setString(2, paymentMethod.getPaymentProvider());
            statement.setString(3, nullIfBlank(paymentMethod.getProviderPaymentMethodReference()));
            statement.setString(4, nullIfBlank(paymentMethod.getCardholderName()));
            statement.setString(5, nullIfBlank(paymentMethod.getCardBrand()));
            statement.setString(6, nullIfBlank(paymentMethod.getMaskedCardNumber()));
            setNullableInt(statement, 7, paymentMethod.getExpiryMonth());
            setNullableInt(statement, 8, paymentMethod.getExpiryYear());
            statement.setString(9, nullIfBlank(paymentMethod.getBillingZipCode()));
            statement.setInt(10, paymentMethod.getPaymentMethodStatusId());
            statement.setBoolean(11, paymentMethod.isLatest());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return null;
        }
    }

    private void updateLatestOff(
            Connection connection,
            Integer customerId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LATEST_OFF_SQL)) {
            statement.setInt(1, customerId);
            statement.executeUpdate();
        }
    }

    private CustomerPaymentMethod mapPaymentMethod(ResultSet resultSet) throws SQLException {
        CustomerPaymentMethod paymentMethod = new CustomerPaymentMethod();

        int customerPaymentMethodId = resultSet.getInt("CustomerPaymentMethodId");
        paymentMethod.setCustomerPaymentMethodId(resultSet.wasNull() ? null : customerPaymentMethodId);

        int customerId = resultSet.getInt("CustomerId");
        paymentMethod.setCustomerId(resultSet.wasNull() ? null : customerId);

        paymentMethod.setPaymentProvider(resultSet.getString("PaymentProvider"));
        paymentMethod.setProviderPaymentMethodReference(resultSet.getString("ProviderPaymentMethodReference"));
        paymentMethod.setCardholderName(resultSet.getString("CardholderName"));
        paymentMethod.setCardBrand(resultSet.getString("CardBrand"));
        paymentMethod.setMaskedCardNumber(resultSet.getString("MaskedCardNumber"));

        int expiryMonth = resultSet.getInt("ExpiryMonth");
        paymentMethod.setExpiryMonth(resultSet.wasNull() ? null : expiryMonth);

        int expiryYear = resultSet.getInt("ExpiryYear");
        paymentMethod.setExpiryYear(resultSet.wasNull() ? null : expiryYear);

        paymentMethod.setBillingZipCode(resultSet.getString("BillingZipCode"));

        int paymentMethodStatus = resultSet.getInt("PaymentMethodStatus");
        paymentMethod.setPaymentMethodStatusId(resultSet.wasNull() ? null : paymentMethodStatus);

        paymentMethod.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        paymentMethod.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        boolean latest = resultSet.getBoolean("Latest");
        paymentMethod.setLatest(resultSet.wasNull() ? null : latest);

        return paymentMethod;
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

    private String nullIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Ignore rollback error.
        }
    }

    private void restoreAutoCommitQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Ignore auto commit restore error.
        }
    }
}