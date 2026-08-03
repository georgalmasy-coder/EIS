package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.SubscriptionPlanBillingPeriod;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubscriptionPlanBillingPeriodProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanBillingPeriodProvider.class);

    private static final String SELECT_BY_PLAN_ID_SQL =
            "SELECT " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "SubscriptionPlanId, " +
                    "BillingPeriodCode, " +
                    "BillingPeriodName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] " +
                    "WHERE SubscriptionPlanId = ? " +
                    "ORDER BY BillingPeriodCode ASC, SubscriptionPlanBillingPeriodId ASC ";

    private static final String SELECT_BY_ID_SQL =
            "SELECT " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "SubscriptionPlanId, " +
                    "BillingPeriodCode, " +
                    "BillingPeriodName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] " +
                    "WHERE SubscriptionPlanBillingPeriodId = ? ";

    private static final String INSERT_SQL =
            "INSERT INTO [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] ( " +
                    "SubscriptionPlanId, " +
                    "BillingPeriodCode, " +
                    "BillingPeriodName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "Active " +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?) ";

    private static final String UPDATE_SQL =
            "UPDATE [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] " +
                    "SET BillingPeriodCode = ?, " +
                    "BillingPeriodName = ?, " +
                    "Description = ?, " +
                    "PriceAmount = ?, " +
                    "Currency = ?, " +
                    "Active = ?, " +
                    "UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE SubscriptionPlanBillingPeriodId = ? AND SubscriptionPlanId = ? ";

    private static final String DELETE_BY_PLAN_ID_SQL =
            "DELETE FROM [dbo].[SUBSCRIPTION_PLAN_BILLING_PERIOD] WHERE SubscriptionPlanId = ? ";

    public SubscriptionPlanBillingPeriodProvider(WebSession webSession) {
        super(webSession);
    }

    public List<SubscriptionPlanBillingPeriod> getBillingPeriodsByPlanId(Integer subscriptionPlanId) {
        if (subscriptionPlanId == null) {
            return Collections.emptyList();
        }

        List<SubscriptionPlanBillingPeriod> rows = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_PLAN_ID_SQL)) {

            statement.setInt(1, subscriptionPlanId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapBillingPeriod(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading billing periods. subscriptionPlanId={}", subscriptionPlanId, e);
            return Collections.emptyList();
        }

        return rows;
    }

    public SubscriptionPlanBillingPeriod getBillingPeriodById(Integer subscriptionPlanBillingPeriodId) {
        if (subscriptionPlanBillingPeriodId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, subscriptionPlanBillingPeriodId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBillingPeriod(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading billing period. subscriptionPlanBillingPeriodId={}", subscriptionPlanBillingPeriodId, e);
        }

        return null;
    }

    public Integer createBillingPeriod(SubscriptionPlanBillingPeriod billingPeriod) {
        if (billingPeriod == null || billingPeriod.getSubscriptionPlanId() == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, billingPeriod.getSubscriptionPlanId());
            statement.setString(2, safeText(billingPeriod.getBillingPeriodCode(), ""));
            statement.setString(3, safeText(billingPeriod.getBillingPeriodName(), ""));
            statement.setString(4, safeText(billingPeriod.getDescription(), ""));
            statement.setBigDecimal(5, billingPeriod.getPriceAmount());
            statement.setString(6, safeText(billingPeriod.getCurrency(), "EUR"));
            statement.setBoolean(7, billingPeriod.getActive());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    billingPeriod.setSubscriptionPlanBillingPeriodId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Error creating billing period. planId={}", billingPeriod.getSubscriptionPlanId(), e);
        }

        return null;
    }

    public boolean updateBillingPeriod(SubscriptionPlanBillingPeriod billingPeriod) {
        if (billingPeriod == null
                || billingPeriod.getSubscriptionPlanBillingPeriodId() == null
                || billingPeriod.getSubscriptionPlanId() == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, safeText(billingPeriod.getBillingPeriodCode(), ""));
            statement.setString(2, safeText(billingPeriod.getBillingPeriodName(), ""));
            statement.setString(3, safeText(billingPeriod.getDescription(), ""));
            statement.setBigDecimal(4, billingPeriod.getPriceAmount());
            statement.setString(5, safeText(billingPeriod.getCurrency(), "EUR"));
            statement.setBoolean(6, billingPeriod.getActive());
            statement.setInt(7, billingPeriod.getSubscriptionPlanBillingPeriodId());
            statement.setInt(8, billingPeriod.getSubscriptionPlanId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating billing period. id={}", billingPeriod.getSubscriptionPlanBillingPeriodId(), e);
            return false;
        }
    }

    public boolean replaceBillingPeriods(
            Integer subscriptionPlanId,
            List<SubscriptionPlanBillingPeriod> billingPeriods
    ) {
        if (subscriptionPlanId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_BY_PLAN_ID_SQL)) {
                    deleteStatement.setInt(1, subscriptionPlanId);
                    deleteStatement.executeUpdate();
                }

                if (billingPeriods != null) {
                    for (SubscriptionPlanBillingPeriod billingPeriod : billingPeriods) {
                        if (billingPeriod == null) {
                            continue;
                        }

                        billingPeriod.setSubscriptionPlanId(subscriptionPlanId);
                        insertBillingPeriod(connection, billingPeriod);
                    }
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error replacing billing periods. subscriptionPlanId={}", subscriptionPlanId, e);
            return false;
        }
    }

    private Integer insertBillingPeriod(
            Connection connection,
            SubscriptionPlanBillingPeriod billingPeriod
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, billingPeriod.getSubscriptionPlanId());
            statement.setString(2, safeText(billingPeriod.getBillingPeriodCode(), ""));
            statement.setString(3, safeText(billingPeriod.getBillingPeriodName(), ""));
            statement.setString(4, safeText(billingPeriod.getDescription(), ""));
            statement.setBigDecimal(5, billingPeriod.getPriceAmount());
            statement.setString(6, safeText(billingPeriod.getCurrency(), "EUR"));
            statement.setBoolean(7, billingPeriod.getActive());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    billingPeriod.setSubscriptionPlanBillingPeriodId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Could not read generated billing period id.");
    }

    private SubscriptionPlanBillingPeriod mapBillingPeriod(ResultSet resultSet) throws SQLException {
        SubscriptionPlanBillingPeriod billingPeriod = new SubscriptionPlanBillingPeriod();

        int id = resultSet.getInt("SubscriptionPlanBillingPeriodId");
        billingPeriod.setSubscriptionPlanBillingPeriodId(resultSet.wasNull() ? null : id);

        int planId = resultSet.getInt("SubscriptionPlanId");
        billingPeriod.setSubscriptionPlanId(resultSet.wasNull() ? null : planId);

        billingPeriod.setBillingPeriodCode(resultSet.getString("BillingPeriodCode"));
        billingPeriod.setBillingPeriodName(resultSet.getString("BillingPeriodName"));
        billingPeriod.setDescription(resultSet.getString("Description"));

        BigDecimal priceAmount = resultSet.getBigDecimal("PriceAmount");
        billingPeriod.setPriceAmount(priceAmount);

        billingPeriod.setCurrency(resultSet.getString("Currency"));
        billingPeriod.setActive(resultSet.getBoolean("Active"));
        billingPeriod.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        billingPeriod.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return billingPeriod;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }
}
