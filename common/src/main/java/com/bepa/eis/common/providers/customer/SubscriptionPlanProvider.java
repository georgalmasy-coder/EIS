package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
                    "ValidFrom, " +
                    "ValidTo, " +
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
                    "ValidFrom, " +
                    "ValidTo, " +
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
                    "  AND ValidFrom <= CONVERT(date, SYSUTCDATETIME()) " +
                    "  AND (ValidTo IS NULL OR ValidTo >= CONVERT(date, SYSUTCDATETIME())) " +
                    "ORDER BY ValidFrom DESC, SubscriptionPlanId DESC ";

    private static final String SELECT_ACTIVE_PLANS_SQL =
            "SELECT " +
                    "SubscriptionPlanId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "ValidFrom, " +
                    "ValidTo, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "WHERE Active = 1 " +
                    "  AND ValidFrom <= CONVERT(date, SYSUTCDATETIME()) " +
                    "  AND (ValidTo IS NULL OR ValidTo >= CONVERT(date, SYSUTCDATETIME())) " +
                    "ORDER BY ModuleName ASC, PlanName ASC, ValidFrom DESC ";

    private static final String SELECT_ALL_PLANS_SQL =
            "SELECT " +
                    "SubscriptionPlanId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "ValidFrom, " +
                    "ValidTo, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active, " +
                    "CreatedAt, " +
                    "UpdatedAt " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "ORDER BY ModuleName ASC, PlanName ASC, ValidFrom DESC, SubscriptionPlanId DESC ";

    private static final String INSERT_PLAN_SQL =
            "INSERT INTO [dbo].[SUBSCRIPTION_PLAN] ( " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "PlanName, " +
                    "Description, " +
                    "ValidFrom, " +
                    "ValidTo, " +
                    "PriceAmount, " +
                    "Currency, " +
                    "BillingPeriodMonths, " +
                    "TrialDays, " +
                    "Active " +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    private static final String UPDATE_PLAN_SQL =
            "UPDATE [dbo].[SUBSCRIPTION_PLAN] " +
                    "SET ModuleCode = ?, " +
                    "ModuleName = ?, " +
                    "PlanName = ?, " +
                    "Description = ?, " +
                    "ValidFrom = ?, " +
                    "ValidTo = ?, " +
                    "PriceAmount = ?, " +
                    "Currency = ?, " +
                    "BillingPeriodMonths = ?, " +
                    "TrialDays = ?, " +
                    "Active = ?, " +
                    "UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE SubscriptionPlanId = ? ";

    private static final String SELECT_PLAN_VALIDITY_SQL =
            "SELECT ValidFrom, ValidTo " +
                    "FROM [dbo].[SUBSCRIPTION_PLAN] " +
                    "WHERE ModuleCode = ? " +
                    "  AND SubscriptionPlanId <> ISNULL(?, -1) ";

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

    public List<SubscriptionPlan> getAllPlans() {
        List<SubscriptionPlan> plans = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_PLANS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                plans.add(mapPlan(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error loading all subscription plans.", e);
            return Collections.emptyList();
        }

        return plans;
    }

    public Integer createPlan(SubscriptionPlan plan) {
        if (plan == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_PLAN_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, safeText(plan.getModuleCode(), ""));
            statement.setString(2, safeText(plan.getModuleName(), ""));
            statement.setString(3, safeText(plan.getPlanName(), ""));
            statement.setString(4, safeText(plan.getDescription(), ""));
            setLocalDate(statement, plan.getValidFrom(), 5);
            setLocalDate(statement, plan.getValidTo(), 6);
            statement.setBigDecimal(7, plan.getPriceAmount());
            statement.setString(8, safeText(plan.getCurrency(), "EUR"));
            setNullableInt(statement, plan.getBillingPeriodMonths(), 9);
            setNullableInt(statement, plan.getTrialDays(), 10);
            statement.setBoolean(11, plan.getActive());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    plan.setSubscriptionPlanId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Error creating subscription plan.", e);
        }

        return null;
    }

    public boolean updatePlan(SubscriptionPlan plan) {
        if (plan == null || plan.getSubscriptionPlanId() == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PLAN_SQL)) {

            statement.setString(1, safeText(plan.getModuleCode(), ""));
            statement.setString(2, safeText(plan.getModuleName(), ""));
            statement.setString(3, safeText(plan.getPlanName(), ""));
            statement.setString(4, safeText(plan.getDescription(), ""));
            setLocalDate(statement, plan.getValidFrom(), 5);
            setLocalDate(statement, plan.getValidTo(), 6);
            statement.setBigDecimal(7, plan.getPriceAmount());
            statement.setString(8, safeText(plan.getCurrency(), "EUR"));
            setNullableInt(statement, plan.getBillingPeriodMonths(), 9);
            setNullableInt(statement, plan.getTrialDays(), 10);
            statement.setBoolean(11, plan.getActive());
            statement.setInt(12, plan.getSubscriptionPlanId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating subscription plan. subscriptionPlanId={}", plan.getSubscriptionPlanId(), e);
            return false;
        }
    }

    public boolean hasOverlappingPlan(
            String moduleCode,
            Integer subscriptionPlanId,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (moduleCode == null || moduleCode.trim().isEmpty() || validFrom == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAN_VALIDITY_SQL)) {

            statement.setString(1, moduleCode.trim().toUpperCase());

            if (subscriptionPlanId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, subscriptionPlanId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    java.sql.Date existingValidFrom = resultSet.getDate("ValidFrom");
                    java.sql.Date existingValidTo = resultSet.getDate("ValidTo");

                    if (existingValidFrom == null) {
                        continue;
                    }

                    LocalDate existingFrom = existingValidFrom.toLocalDate();
                    LocalDate existingTo = existingValidTo == null ? null : existingValidTo.toLocalDate();

                    if (overlaps(existingFrom, existingTo, validFrom, validTo)) {
                        return true;
                    }
                }

                return false;
            }
        } catch (SQLException e) {
            log.error("Error checking overlapping subscription plan. moduleCode={}, subscriptionPlanId={}", moduleCode, subscriptionPlanId, e);
            return true;
        }
    }

    private boolean overlaps(
            LocalDate existingFrom,
            LocalDate existingTo,
            LocalDate candidateFrom,
            LocalDate candidateTo
    ) {
        if (existingFrom == null || candidateFrom == null) {
            return false;
        }

        boolean startsBeforeCandidateEnds = candidateTo == null || !existingFrom.isAfter(candidateTo);
        boolean endsAfterCandidateStarts = existingTo == null || !existingTo.isBefore(candidateFrom);

        return startsBeforeCandidateEnds && endsAfterCandidateStarts;
    }

    private SubscriptionPlan mapPlan(ResultSet resultSet) throws SQLException {
        SubscriptionPlan plan = new SubscriptionPlan();

        int subscriptionPlanId = resultSet.getInt("SubscriptionPlanId");
        plan.setSubscriptionPlanId(resultSet.wasNull() ? null : subscriptionPlanId);

        plan.setModuleCode(resultSet.getString("ModuleCode"));
        plan.setModuleName(resultSet.getString("ModuleName"));
        plan.setPlanName(resultSet.getString("PlanName"));
        plan.setDescription(resultSet.getString("Description"));

        java.sql.Date validFrom = resultSet.getDate("ValidFrom");
        java.sql.Date validTo = resultSet.getDate("ValidTo");
        plan.setValidFrom(validFrom == null ? null : validFrom.toLocalDate());
        plan.setValidTo(validTo == null ? null : validTo.toLocalDate());

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

    private void setLocalDate(PreparedStatement statement, LocalDate value, int index) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(value));
        }
    }

    private void setNullableInt(PreparedStatement statement, Integer value, int index) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }
}
