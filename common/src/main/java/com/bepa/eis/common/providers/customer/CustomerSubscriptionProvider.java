package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.CustomerSubscription;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.CustomerSubscriptionStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class CustomerSubscriptionProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerSubscriptionProvider.class);

    private static final String INSERT_SUBSCRIPTION_SQL =
            "INSERT INTO [dbo].[CUSTOMER_SUBSCRIPTION] ( " +
                    "CustomerId, " +
                    "SubscriptionStatus, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "SubscriptionPlanName, " +
                    "TrialStartAt, " +
                    "TrialEndAt, " +
                    "TrialReminderSentAt, " +
                    "PeriodStartAt, " +
                    "PeriodEndAt, " +
                    "RenewalReminderSentAt, " +
                    "ContinuationConfirmedAt, " +
                    "RenewalConfirmedAt, " +
                    "GracePeriodEndsAt " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_SUBSCRIPTION_BY_ID_SQL =
            "SELECT " +
                    "SubscriptionId, " +
                    "CustomerId, " +
                    "SubscriptionStatus, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "SubscriptionPlanName, " +
                    "TrialStartAt, " +
                    "TrialEndAt, " +
                    "TrialReminderSentAt, " +
                    "PeriodStartAt, " +
                    "PeriodEndAt, " +
                    "RenewalReminderSentAt, " +
                    "ContinuationConfirmedAt, " +
                    "RenewalConfirmedAt, " +
                    "GracePeriodEndsAt, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_SUBSCRIPTION] " +
                    "WHERE SubscriptionId = ? ";

    private static final String SELECT_LATEST_SUBSCRIPTION_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (1) " +
                    "SubscriptionId, " +
                    "CustomerId, " +
                    "SubscriptionStatus, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "SubscriptionPlanName, " +
                    "TrialStartAt, " +
                    "TrialEndAt, " +
                    "TrialReminderSentAt, " +
                    "PeriodStartAt, " +
                    "PeriodEndAt, " +
                    "RenewalReminderSentAt, " +
                    "ContinuationConfirmedAt, " +
                    "RenewalConfirmedAt, " +
                    "GracePeriodEndsAt, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[CUSTOMER_SUBSCRIPTION] " +
                    "WHERE CustomerId = ? " +
                    "ORDER BY SubscriptionId DESC ";

    private static final String UPDATE_SUBSCRIPTION_SQL =
            "UPDATE [dbo].[CUSTOMER_SUBSCRIPTION] " +
                    "SET " +
                    "    SubscriptionStatus = ?, " +
                    "    SubscriptionPlanId = ?, " +
                    "    SubscriptionPlanBillingPeriodId = ?, " +
                    "    SubscriptionPlanName = ?, " +
                    "    TrialStartAt = ?, " +
                    "    TrialEndAt = ?, " +
                    "    TrialReminderSentAt = ?, " +
                    "    PeriodStartAt = ?, " +
                    "    PeriodEndAt = ?, " +
                    "    RenewalReminderSentAt = ?, " +
                    "    ContinuationConfirmedAt = ?, " +
                    "    RenewalConfirmedAt = ?, " +
                    "    GracePeriodEndsAt = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE SubscriptionId = ? ";

    private static final String UPDATE_SUBSCRIPTION_STATUS_SQL =
            "UPDATE [dbo].[CUSTOMER_SUBSCRIPTION] " +
                    "SET " +
                    "    SubscriptionStatus = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE SubscriptionId = ? ";

    public CustomerSubscriptionProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createSubscription(CustomerSubscription subscription) {
        if (subscription == null || subscription.getCustomerId() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SUBSCRIPTION_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, subscription.getCustomerId());
            statement.setString(2, subscription.getSubscriptionStatusCode());
            setNullableInt(statement, 3, subscription.getSubscriptionPlanId());
            setNullableInt(statement, 4, subscription.getSubscriptionPlanBillingPeriodId());
            statement.setString(5, safeText(subscription.getSubscriptionPlanName(), ""));
            statement.setTimestamp(6, subscription.getTrialStartAt());
            statement.setTimestamp(7, subscription.getTrialEndAt());
            statement.setTimestamp(8, subscription.getTrialReminderSentAt());
            statement.setTimestamp(9, subscription.getPeriodStartAt());
            statement.setTimestamp(10, subscription.getPeriodEndAt());
            statement.setTimestamp(11, subscription.getRenewalReminderSentAt());
            statement.setTimestamp(12, subscription.getContinuationConfirmedAt());
            statement.setTimestamp(13, subscription.getRenewalConfirmedAt());
            statement.setTimestamp(14, subscription.getGracePeriodEndsAt());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Integer subscriptionId = generatedKeys.getInt(1);
                    subscription.setSubscriptionId(subscriptionId);
                    return subscriptionId;
                }
            }
        } catch (SQLException e) {
            log.error("Error creating customer subscription. customerId={}", subscription.getCustomerId(), e);
        }

        return null;
    }

    public CustomerSubscription getSubscriptionById(Integer subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SUBSCRIPTION_BY_ID_SQL)) {

            statement.setInt(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSubscription(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer subscription. subscriptionId={}", subscriptionId, e);
        }

        return null;
    }

    public CustomerSubscription getLatestSubscriptionByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_SUBSCRIPTION_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSubscription(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest customer subscription. customerId={}", customerId, e);
        }

        return null;
    }

    public boolean updateSubscription(CustomerSubscription subscription) {
        if (subscription == null || subscription.getSubscriptionId() == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SUBSCRIPTION_SQL)) {

            statement.setString(1, subscription.getSubscriptionStatusCode());
            setNullableInt(statement, 2, subscription.getSubscriptionPlanId());
            setNullableInt(statement, 3, subscription.getSubscriptionPlanBillingPeriodId());
            statement.setString(4, safeText(subscription.getSubscriptionPlanName(), ""));
            statement.setTimestamp(5, subscription.getTrialStartAt());
            statement.setTimestamp(6, subscription.getTrialEndAt());
            statement.setTimestamp(7, subscription.getTrialReminderSentAt());
            statement.setTimestamp(8, subscription.getPeriodStartAt());
            statement.setTimestamp(9, subscription.getPeriodEndAt());
            statement.setTimestamp(10, subscription.getRenewalReminderSentAt());
            statement.setTimestamp(11, subscription.getContinuationConfirmedAt());
            statement.setTimestamp(12, subscription.getRenewalConfirmedAt());
            statement.setTimestamp(13, subscription.getGracePeriodEndsAt());
            statement.setInt(14, subscription.getSubscriptionId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating customer subscription. subscriptionId={}", subscription.getSubscriptionId(), e);
            return false;
        }
    }

    public boolean updateSubscriptionStatus(
            Integer subscriptionId,
            CustomerSubscriptionStatus subscriptionStatus
    ) {
        if (subscriptionId == null) {
            return false;
        }

        CustomerSubscriptionStatus safeStatus = subscriptionStatus == null
                ? CustomerSubscriptionStatus.NONE
                : subscriptionStatus;

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SUBSCRIPTION_STATUS_SQL)) {

            statement.setString(1, safeStatus.getCode());
            statement.setInt(2, subscriptionId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating customer subscription status. subscriptionId={}", subscriptionId, e);
            return false;
        }
    }

    private CustomerSubscription mapSubscription(ResultSet resultSet) throws SQLException {
        CustomerSubscription subscription = new CustomerSubscription();

        int subscriptionId = resultSet.getInt("SubscriptionId");
        subscription.setSubscriptionId(resultSet.wasNull() ? null : subscriptionId);

        int customerId = resultSet.getInt("CustomerId");
        subscription.setCustomerId(resultSet.wasNull() ? null : customerId);

        subscription.setSubscriptionStatusCode(resultSet.getString("SubscriptionStatus"));

        int subscriptionPlanId = resultSet.getInt("SubscriptionPlanId");
        subscription.setSubscriptionPlanId(resultSet.wasNull() ? null : subscriptionPlanId);

        int subscriptionPlanBillingPeriodId = resultSet.getInt("SubscriptionPlanBillingPeriodId");
        subscription.setSubscriptionPlanBillingPeriodId(resultSet.wasNull() ? null : subscriptionPlanBillingPeriodId);

        subscription.setSubscriptionPlanName(resultSet.getString("SubscriptionPlanName"));
        subscription.setTrialStartAt(resultSet.getTimestamp("TrialStartAt"));
        subscription.setTrialEndAt(resultSet.getTimestamp("TrialEndAt"));
        subscription.setTrialReminderSentAt(resultSet.getTimestamp("TrialReminderSentAt"));
        subscription.setPeriodStartAt(resultSet.getTimestamp("PeriodStartAt"));
        subscription.setPeriodEndAt(resultSet.getTimestamp("PeriodEndAt"));
        subscription.setRenewalReminderSentAt(resultSet.getTimestamp("RenewalReminderSentAt"));
        subscription.setContinuationConfirmedAt(resultSet.getTimestamp("ContinuationConfirmedAt"));
        subscription.setRenewalConfirmedAt(resultSet.getTimestamp("RenewalConfirmedAt"));
        subscription.setGracePeriodEndsAt(resultSet.getTimestamp("GracePeriodEndsAt"));
        subscription.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        subscription.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return subscription;
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
}
