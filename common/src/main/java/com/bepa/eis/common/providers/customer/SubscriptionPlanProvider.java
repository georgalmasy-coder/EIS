package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubscriptionPlanProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanProvider.class);

    private static final String SELECT_PLAN_BY_ID_SQL =
            "SELECT " +
                    "SubscriptionPlanId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "WHERE SubscriptionPlanId = ? ";

    private static final String SELECT_ACTIVE_PLAN_BY_MODULE_CODE_SQL =
            "SELECT TOP (1) " +
                    "SubscriptionPlanId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "WHERE ModuleCode = ? " +
                    "  AND Active = 1 " +
                    "ORDER BY SubscriptionPlanId ASC ";

    private static final String SELECT_ACTIVE_PLANS_SQL =
            "SELECT " +
                    "SubscriptionPlanId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "WHERE Active = 1 " +
                    "ORDER BY ModuleName ASC, PlanName ASC ";

    public SubscriptionPlanProvider(WebSession webSession) {
        super(webSession);
    }

    public SubscriptionPlan getPlanById(Integer subscriptionPlanId) {
        if (subscriptionPlanId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAN_BY_ID_SQL)) {

            statement.setInt(1, subscriptionPlanId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPlan(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading subscription plan. subscriptionPlanId={}", subscriptionPlanId, e);
        }

        return null;
    }

    public SubscriptionPlan getActivePlanByModuleCode(String moduleCode) {
        if (moduleCode == null || moduleCode.trim().isEmpty()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_PLAN_BY_MODULE_CODE_SQL)) {

            statement.setString(1, moduleCode.trim().toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPlan(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading active subscription plan. moduleCode={}", moduleCode, e);
        }

        return null;
    }

    public List<SubscriptionPlan> getActivePlans() {
        List<SubscriptionPlan> plans = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_PLANS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                plans.add(mapPlan(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error loading active subscription plans.", e);
            return Collections.emptyList();
        }

        return plans;
    }

    private SubscriptionPlan mapPlan(ResultSet resultSet) throws SQLException {
        SubscriptionPlan plan = new SubscriptionPlan();

        int subscriptionPlanId = resultSet.getInt("SubscriptionPlanId");
        plan.setSubscriptionPlanId(resultSet.wasNull() ? null : subscriptionPlanId);

        plan.setModuleCode(resultSet.getString("ModuleCode"));
        plan.setModuleName(resultSet.getString("ModuleName"));
        plan.setPlanName(resultSet.getString("PlanName"));
        plan.setDescription(resultSet.getString("Description"));

        BigDecimal priceAmount = resultSet.getBigDecimal("PriceAmount");
        plan.setPriceAmount(priceAmount);

        plan.setCurrency(resultSet.getString("Currency"));

        int billingPeriodMonths = resultSet.getInt("BillingPeriodMonths");
        plan.setBillingPeriodMonths(resultSet.wasNull() ? null : billingPeriodMonths);

        int trialDays = resultSet.getInt("TrialDays");
        plan.setTrialDays(resultSet.wasNull() ? null : trialDays);

        boolean active = resultSet.getBoolean("Active");
        plan.setActive(resultSet.wasNull() ? null : active);

        plan.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        plan.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        return plan;
    }
}