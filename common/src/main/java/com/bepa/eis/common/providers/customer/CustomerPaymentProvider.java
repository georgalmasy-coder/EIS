package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerPayment;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerPaymentStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class CustomerPaymentProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerPaymentProvider.class);

    private static final String INSERT_PAYMENT_SQL =
            "INSERT INTO [dbo].[CUSTOMER_PAYMENT] ( " +
                    "CustomerId, " +
                    "SubscriptionId, " +
                    "PaymentStatus, " +
                    "PaymentProvider, " +
                    "PaymentProviderReference, " +
                    "Amount, " +
                    "Currency, " +
                    "PaymentDueAt, " +
                    "GracePeriodEndsAt, " +
                    "RequestedAt, " +
                    "AuthorizedAt, " +
                    "CapturedAt, " +
                    "SucceededAt, " +
                    "FailedAt, " +
                    "CancelledAt, " +
                    "FailureReason " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_PAYMENT_BY_ID_SQL =
            "SELECT " +
                    "PaymentId, " +
                    "CustomerId, " +
                    "SubscriptionId, " +
                    "PaymentStatus, " +
                    "PaymentProvider, " +
                    "PaymentProviderReference, " +
                    "Amount, " +
                    "Currency, " +
                    "PaymentDueAt, " +
                    "GracePeriodEndsAt, " +
                    "RequestedAt, " +
                    "AuthorizedAt, " +
                    "CapturedAt, " +
                    "SucceededAt, " +
                    "FailedAt, " +
                    "CancelledAt, " +
                    "FailureReason, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_PAYMENT] " +
                    "WHERE PaymentId = ? ";

    private static final String SELECT_LATEST_PAYMENT_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (1) " +
                    "PaymentId, " +
                    "CustomerId, " +
                    "SubscriptionId, " +
                    "PaymentStatus, " +
                    "PaymentProvider, " +
                    "PaymentProviderReference, " +
                    "Amount, " +
                    "Currency, " +
                    "PaymentDueAt, " +
                    "GracePeriodEndsAt, " +
                    "RequestedAt, " +
                    "AuthorizedAt, " +
                    "CapturedAt, " +
                    "SucceededAt, " +
                    "FailedAt, " +
                    "CancelledAt, " +
                    "FailureReason, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_PAYMENT] " +
                    "WHERE CustomerId = ? " +
                    "ORDER BY PaymentId DESC ";

    private static final String SELECT_LATEST_PAYMENT_BY_SUBSCRIPTION_ID_SQL =
            "SELECT TOP (1) " +
                    "PaymentId, " +
                    "CustomerId, " +
                    "SubscriptionId, " +
                    "PaymentStatus, " +
                    "PaymentProvider, " +
                    "PaymentProviderReference, " +
                    "Amount, " +
                    "Currency, " +
                    "PaymentDueAt, " +
                    "GracePeriodEndsAt, " +
                    "RequestedAt, " +
                    "AuthorizedAt, " +
                    "CapturedAt, " +
                    "SucceededAt, " +
                    "FailedAt, " +
                    "CancelledAt, " +
                    "FailureReason, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_PAYMENT] " +
                    "WHERE SubscriptionId = ? " +
                    "ORDER BY PaymentId DESC ";

    private static final String UPDATE_PAYMENT_SQL =
            "UPDATE [dbo].[CUSTOMER_PAYMENT] " +
                    "SET " +
                    "    SubscriptionId = ?, " +
                    "    PaymentStatus = ?, " +
                    "    PaymentProvider = ?, " +
                    "    PaymentProviderReference = ?, " +
                    "    Amount = ?, " +
                    "    Currency = ?, " +
                    "    PaymentDueAt = ?, " +
                    "    GracePeriodEndsAt = ?, " +
                    "    RequestedAt = ?, " +
                    "    AuthorizedAt = ?, " +
                    "    CapturedAt = ?, " +
                    "    SucceededAt = ?, " +
                    "    FailedAt = ?, " +
                    "    CancelledAt = ?, " +
                    "    FailureReason = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE PaymentId = ? ";

    private static final String UPDATE_PAYMENT_STATUS_SQL =
            "UPDATE [dbo].[CUSTOMER_PAYMENT] " +
                    "SET " +
                    "    PaymentStatus = ?, " +
                    "    FailureReason = ?, " +
                    "    RequestedAt = CASE WHEN ? = 'REQUESTED' THEN SYSUTCDATETIME() ELSE RequestedAt END, " +
                    "    AuthorizedAt = CASE WHEN ? = 'AUTHORIZED' THEN SYSUTCDATETIME() ELSE AuthorizedAt END, " +
                    "    CapturedAt = CASE WHEN ? = 'CAPTURED' THEN SYSUTCDATETIME() ELSE CapturedAt END, " +
                    "    SucceededAt = CASE WHEN ? = 'SUCCEEDED' THEN SYSUTCDATETIME() ELSE SucceededAt END, " +
                    "    FailedAt = CASE WHEN ? IN ('FAILED', 'REJECTED', 'EXPIRED', 'TIMED_OUT', 'OVERDUE') THEN SYSUTCDATETIME() ELSE FailedAt END, " +
                    "    CancelledAt = CASE WHEN ? = 'CANCELLED' THEN SYSUTCDATETIME() ELSE CancelledAt END, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE PaymentId = ? ";

    public CustomerPaymentProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createPayment(CustomerPayment payment) {
        if (payment == null || payment.getCustomerId() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_PAYMENT_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, payment.getCustomerId());
            setNullableInt(statement, 2, payment.getSubscriptionId());
            statement.setString(3, payment.getPaymentStatusCode());
            statement.setString(4, safeText(payment.getPaymentProvider(), ""));
            statement.setString(5, safeText(payment.getPaymentProviderReference(), ""));
            statement.setBigDecimal(6, safeAmount(payment.getAmount()));
            statement.setString(7, safeText(payment.getCurrency(), "EUR"));
            statement.setTimestamp(8, payment.getPaymentDueAt());
            statement.setTimestamp(9, payment.getGracePeriodEndsAt());
            statement.setTimestamp(10, payment.getRequestedAt());
            statement.setTimestamp(11, payment.getAuthorizedAt());
            statement.setTimestamp(12, payment.getCapturedAt());
            statement.setTimestamp(13, payment.getSucceededAt());
            statement.setTimestamp(14, payment.getFailedAt());
            statement.setTimestamp(15, payment.getCancelledAt());
            statement.setString(16, safeText(payment.getFailureReason(), ""));

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer paymentId = generatedKeys.getInt(1);
                    payment.setPaymentId(paymentId);
                    return paymentId;
                }
            }
        } catch (SQLException e) {
            log.error("Error creating customer payment. customerId={}", payment.getCustomerId(), e);
        }

        return null;
    }

    public CustomerPayment getPaymentById(Integer paymentId) {
        if (paymentId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PAYMENT_BY_ID_SQL)) {

            statement.setInt(1, paymentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPayment(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer payment. paymentId={}", paymentId, e);
        }

        return null;
    }

    public CustomerPayment getLatestPaymentByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_PAYMENT_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPayment(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest customer payment. customerId={}", customerId, e);
        }

        return null;
    }

    public CustomerPayment getLatestPaymentBySubscriptionId(Integer subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_PAYMENT_BY_SUBSCRIPTION_ID_SQL)) {

            statement.setInt(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPayment(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest customer payment. subscriptionId={}", subscriptionId, e);
        }

        return null;
    }

    public boolean updatePayment(CustomerPayment payment) {
        if (payment == null || payment.getPaymentId() == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PAYMENT_SQL)) {

            setNullableInt(statement, 1, payment.getSubscriptionId());
            statement.setString(2, payment.getPaymentStatusCode());
            statement.setString(3, safeText(payment.getPaymentProvider(), ""));
            statement.setString(4, safeText(payment.getPaymentProviderReference(), ""));
            statement.setBigDecimal(5, safeAmount(payment.getAmount()));
            statement.setString(6, safeText(payment.getCurrency(), "EUR"));
            statement.setTimestamp(7, payment.getPaymentDueAt());
            statement.setTimestamp(8, payment.getGracePeriodEndsAt());
            statement.setTimestamp(9, payment.getRequestedAt());
            statement.setTimestamp(10, payment.getAuthorizedAt());
            statement.setTimestamp(11, payment.getCapturedAt());
            statement.setTimestamp(12, payment.getSucceededAt());
            statement.setTimestamp(13, payment.getFailedAt());
            statement.setTimestamp(14, payment.getCancelledAt());
            statement.setString(15, safeText(payment.getFailureReason(), ""));
            statement.setInt(16, payment.getPaymentId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating customer payment. paymentId={}", payment.getPaymentId(), e);
            return false;
        }
    }

    public boolean updatePaymentStatus(
            Integer paymentId,
            CustomerPaymentStatus paymentStatus,
            String failureReason
    ) {
        if (paymentId == null) {
            return false;
        }

        CustomerPaymentStatus safeStatus = paymentStatus == null
                ? CustomerPaymentStatus.NONE
                : paymentStatus;

        String statusCode = safeStatus.getCode();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PAYMENT_STATUS_SQL)) {

            statement.setString(1, statusCode);
            statement.setString(2, safeText(failureReason, ""));
            statement.setString(3, statusCode);
            statement.setString(4, statusCode);
            statement.setString(5, statusCode);
            statement.setString(6, statusCode);
            statement.setString(7, statusCode);
            statement.setString(8, statusCode);
            statement.setInt(9, paymentId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating customer payment status. paymentId={}", paymentId, e);
            return false;
        }
    }

    private CustomerPayment mapPayment(ResultSet resultSet) throws SQLException {
        CustomerPayment payment = new CustomerPayment();

        int paymentId = resultSet.getInt("PaymentId");
        payment.setPaymentId(resultSet.wasNull() ? null : paymentId);

        int customerId = resultSet.getInt("CustomerId");
        payment.setCustomerId(resultSet.wasNull() ? null : customerId);

        int subscriptionId = resultSet.getInt("SubscriptionId");
        payment.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        payment.setPaymentStatusCode(resultSet.getString("PaymentStatus"));
        payment.setPaymentProvider(resultSet.getString("PaymentProvider"));
        payment.setPaymentProviderReference(resultSet.getString("PaymentProviderReference"));
        payment.setAmount(resultSet.getBigDecimal("Amount"));
        payment.setCurrency(resultSet.getString("Currency"));
        payment.setPaymentDueAt(resultSet.getTimestamp("PaymentDueAt"));
        payment.setGracePeriodEndsAt(resultSet.getTimestamp("GracePeriodEndsAt"));
        payment.setRequestedAt(resultSet.getTimestamp("RequestedAt"));
        payment.setAuthorizedAt(resultSet.getTimestamp("AuthorizedAt"));
        payment.setCapturedAt(resultSet.getTimestamp("CapturedAt"));
        payment.setSucceededAt(resultSet.getTimestamp("SucceededAt"));
        payment.setFailedAt(resultSet.getTimestamp("FailedAt"));
        payment.setCancelledAt(resultSet.getTimestamp("CancelledAt"));
        payment.setFailureReason(resultSet.getString("FailureReason"));
        payment.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        payment.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return payment;
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

    private BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return amount;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }
}