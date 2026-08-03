package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerModule;
import com.bepa.eis.common.dto.customer.SubscriptionPlan;
import com.bepa.eis.common.enums.customer.CustomerModuleStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerModuleProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerModuleProvider.class);

    private static final String INSERT_CUSTOMER_MODULE_SQL =
            "INSERT INTO [dbo].[CUSTOMER_MODULE] ( " +
                    "CustomerId, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "CustomerModuleStatus, " +
                    "Latest " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ";

    private static final String SELECT_LATEST_MODULES_BY_CUSTOMER_ID_SQL =
            "SELECT " +
                    "CustomerModuleId, " +
                    "CustomerId, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "CustomerModuleStatus, " +
                    "CreatedAt, " +
                    "UpdatedAt, " +
                    "Latest " +
                    "FROM [dbo].[CUSTOMER_MODULE] " +
                    "WHERE CustomerId = ? " +
                    "  AND Latest = 1 " +
                    "ORDER BY ModuleCode ASC ";

    private static final String SELECT_LATEST_MODULE_BY_CUSTOMER_AND_MODULE_SQL =
            "SELECT TOP (1) " +
                    "CustomerModuleId, " +
                    "CustomerId, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "CustomerModuleStatus, " +
                    "CreatedAt, " +
                    "UpdatedAt, " +
                    "Latest " +
                    "FROM [dbo].[CUSTOMER_MODULE] " +
                    "WHERE CustomerId = ? " +
                    "  AND ModuleCode = ? " +
                    "  AND Latest = 1 " +
                    "ORDER BY CustomerModuleId DESC ";

    private static final String SELECT_CUSTOMER_MODULE_BY_ID_SQL =
            "SELECT " +
                    "CustomerModuleId, " +
                    "CustomerId, " +
                    "SubscriptionPlanId, " +
                    "SubscriptionPlanBillingPeriodId, " +
                    "ModuleCode, " +
                    "ModuleName, " +
                    "CustomerModuleStatus, " +
                    "CreatedAt, " +
                    "UpdatedAt, " +
                    "Latest " +
                    "FROM [dbo].[CUSTOMER_MODULE] " +
                    "WHERE CustomerModuleId = ? ";

    private static final String UPDATE_LATEST_OFF_BY_CUSTOMER_AND_MODULE_SQL =
            "UPDATE [dbo].[CUSTOMER_MODULE] " +
                    "SET Latest = 0, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerId = ? " +
                    "  AND ModuleCode = ? " +
                    "  AND Latest = 1 ";

    private static final String UPDATE_CUSTOMER_MODULE_SQL =
            "UPDATE [dbo].[CUSTOMER_MODULE] " +
                    "SET SubscriptionPlanId = ?, " +
                    "    SubscriptionPlanBillingPeriodId = ?, " +
                    "    ModuleCode = ?, " +
                    "    ModuleName = ?, " +
                    "    CustomerModuleStatus = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerModuleId = ? ";

    private static final String UPDATE_CUSTOMER_MODULE_STATUS_SQL =
            "UPDATE [dbo].[CUSTOMER_MODULE] " +
                    "SET CustomerModuleStatus = ?, " +
                    "    UpdatedAt = SYSUTCDATETIME() " +
                    "WHERE CustomerModuleId = ? ";

    public CustomerModuleProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createCustomerModule(CustomerModule customerModule) {
        if (customerModule == null || customerModule.getCustomerId() == null) {
            return null;
        }

        if (!customerModule.hasSubscriptionPlanId()) {
            log.warn("Customer module could not be created because SubscriptionPlanId is missing. customerId={}", customerModule.getCustomerId());
            return null;
        }

        if (!customerModule.hasModuleCode()) {
            log.warn("Customer module could not be created because ModuleCode is missing. customerId={}", customerModule.getCustomerId());
            return null;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                updateLatestOff(
                        connection,
                        customerModule.getCustomerId(),
                        customerModule.getModuleCode()
                );

                customerModule.setLatest(true);

                Integer customerModuleId = insertCustomerModule(
                        connection,
                        customerModule
                );

                connection.commit();

                customerModule.setCustomerModuleId(customerModuleId);

                log.info(
                        "Customer module created. customerId={}, customerModuleId={}, moduleCode={}, status={}",
                        customerModule.getCustomerId(),
                        customerModuleId,
                        customerModule.getModuleCode(),
                        customerModule.getCustomerModuleStatusCode()
                );

                return customerModuleId;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                restoreAutoCommitQuietly(connection);
            }
        } catch (SQLException e) {
            log.error(
                    "Error creating customer module. customerId={}, moduleCode={}",
                    customerModule.getCustomerId(),
                    customerModule.getModuleCode(),
                    e
            );
            return null;
        }
    }

    public Integer createCustomerModuleFromPlan(
            Integer customerId,
            SubscriptionPlan subscriptionPlan,
            CustomerModuleStatus customerModuleStatus
    ) {
        if (customerId == null || subscriptionPlan == null || subscriptionPlan.getSubscriptionPlanId() == null) {
            return null;
        }

        CustomerModule customerModule = new CustomerModule();

        customerModule.setCustomerId(customerId);
        customerModule.setSubscriptionPlanId(subscriptionPlan.getSubscriptionPlanId());
        customerModule.setModuleCode(subscriptionPlan.getModuleCode());
        customerModule.setModuleName(subscriptionPlan.getModuleName());
        customerModule.setCustomerModuleStatus(customerModuleStatus == null ? CustomerModuleStatus.TRIAL : customerModuleStatus);
        customerModule.setLatest(true);

        return createCustomerModule(customerModule);
    }

    public Integer createCustomerModuleFromPlan(
            Integer customerId,
            SubscriptionPlan subscriptionPlan,
            Integer customerModuleStatusId
    ) {
        CustomerModuleStatus status = CustomerModuleStatus.fromIdOrDefault(
                customerModuleStatusId,
                CustomerModuleStatus.TRIAL
        );

        return createCustomerModuleFromPlan(
                customerId,
                subscriptionPlan,
                status
        );
    }

    public CustomerModule getCustomerModuleById(Integer customerModuleId) {
        if (customerModuleId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CUSTOMER_MODULE_BY_ID_SQL)) {

            statement.setInt(1, customerModuleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomerModule(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer module. customerModuleId={}", customerModuleId, e);
        }

        return null;
    }

    public CustomerModule getLatestCustomerModule(
            Integer customerId,
            String moduleCode
    ) {
        if (customerId == null || moduleCode == null || moduleCode.trim().isEmpty()) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_MODULE_BY_CUSTOMER_AND_MODULE_SQL)) {

            statement.setInt(1, customerId);
            statement.setString(2, moduleCode.trim().toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomerModule(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error(
                    "Error loading latest customer module. customerId={}, moduleCode={}",
                    customerId,
                    moduleCode,
                    e
            );
        }

        return null;
    }

    public List<CustomerModule> getLatestCustomerModules(Integer customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }

        List<CustomerModule> modules = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_MODULES_BY_CUSTOMER_ID_SQL)) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    modules.add(mapCustomerModule(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest customer modules. customerId={}", customerId, e);
            return Collections.emptyList();
        }

        return modules;
    }

    public boolean updateCustomerModule(CustomerModule customerModule) {
        if (customerModule == null || customerModule.getCustomerModuleId() == null) {
            return false;
        }

        if (!customerModule.hasSubscriptionPlanId()) {
            log.warn(
                    "Customer module could not be updated because SubscriptionPlanId is missing. customerModuleId={}",
                    customerModule.getCustomerModuleId()
            );
            return false;
        }

        if (!customerModule.hasModuleCode()) {
            log.warn(
                    "Customer module could not be updated because ModuleCode is missing. customerModuleId={}",
                    customerModule.getCustomerModuleId()
            );
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_CUSTOMER_MODULE_SQL)) {

            statement.setInt(1, customerModule.getSubscriptionPlanId());
            setNullableInt(statement, 2, customerModule.getSubscriptionPlanBillingPeriodId());
            statement.setString(3, customerModule.getModuleCode());
            statement.setString(4, nullIfBlank(customerModule.getModuleName()));
            statement.setInt(5, customerModule.getCustomerModuleStatusId());
            statement.setInt(6, customerModule.getCustomerModuleId());

            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                log.info(
                        "Customer module updated. customerModuleId={}, moduleCode={}, status={}",
                        customerModule.getCustomerModuleId(),
                        customerModule.getModuleCode(),
                        customerModule.getCustomerModuleStatusCode()
                );
            }

            return updated;
        } catch (SQLException e) {
            log.error(
                    "Error updating customer module. customerModuleId={}",
                    customerModule.getCustomerModuleId(),
                    e
            );
            return false;
        }
    }

    public boolean updateCustomerModuleStatus(
            Integer customerModuleId,
            CustomerModuleStatus customerModuleStatus
    ) {
        if (customerModuleId == null) {
            return false;
        }

        CustomerModuleStatus safeStatus = customerModuleStatus == null
                ? CustomerModuleStatus.ACTIVE
                : customerModuleStatus;

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_CUSTOMER_MODULE_STATUS_SQL)) {

            statement.setInt(1, safeStatus.getId());
            statement.setInt(2, customerModuleId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(
                    "Error updating customer module status. customerModuleId={}, status={}",
                    customerModuleId,
                    safeStatus.getCode(),
                    e
            );
            return false;
        }
    }

    public boolean updateCustomerModuleStatus(
            Integer customerModuleId,
            Integer customerModuleStatusId
    ) {
        CustomerModuleStatus status = CustomerModuleStatus.fromIdOrDefault(
                customerModuleStatusId,
                CustomerModuleStatus.ACTIVE
        );

        return updateCustomerModuleStatus(
                customerModuleId,
                status
        );
    }

    private Integer insertCustomerModule(
            Connection connection,
            CustomerModule customerModule
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_CUSTOMER_MODULE_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setInt(1, customerModule.getCustomerId());
            statement.setInt(2, customerModule.getSubscriptionPlanId());
            setNullableInt(statement, 3, customerModule.getSubscriptionPlanBillingPeriodId());
            statement.setString(4, customerModule.getModuleCode());
            statement.setString(5, nullIfBlank(customerModule.getModuleName()));
            statement.setInt(6, customerModule.getCustomerModuleStatusId());
            statement.setBoolean(7, customerModule.isLatest());

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
            Integer customerId,
            String moduleCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LATEST_OFF_BY_CUSTOMER_AND_MODULE_SQL)) {
            statement.setInt(1, customerId);
            statement.setString(2, moduleCode.trim().toUpperCase());
            statement.executeUpdate();
        }
    }

    private CustomerModule mapCustomerModule(ResultSet resultSet) throws SQLException {
        CustomerModule customerModule = new CustomerModule();

        int customerModuleId = resultSet.getInt("CustomerModuleId");
        customerModule.setCustomerModuleId(resultSet.wasNull() ? null : customerModuleId);

        int customerId = resultSet.getInt("CustomerId");
        customerModule.setCustomerId(resultSet.wasNull() ? null : customerId);

        int subscriptionPlanId = resultSet.getInt("SubscriptionPlanId");
        customerModule.setSubscriptionPlanId(resultSet.wasNull() ? null : subscriptionPlanId);

        int subscriptionPlanBillingPeriodId = resultSet.getInt("SubscriptionPlanBillingPeriodId");
        customerModule.setSubscriptionPlanBillingPeriodId(resultSet.wasNull() ? null : subscriptionPlanBillingPeriodId);

        customerModule.setModuleCode(resultSet.getString("ModuleCode"));
        customerModule.setModuleName(resultSet.getString("ModuleName"));

        int customerModuleStatus = resultSet.getInt("CustomerModuleStatus");
        customerModule.setCustomerModuleStatusId(resultSet.wasNull() ? null : customerModuleStatus);

        customerModule.setCreatedAt(resultSet.getTimestamp("CreatedAt"));
        customerModule.setUpdatedAt(resultSet.getTimestamp("UpdatedAt"));

        boolean latest = resultSet.getBoolean("Latest");
        customerModule.setLatest(resultSet.wasNull() ? null : latest);

        return customerModule;
    }

    private String nullIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private void setNullableInt(
            PreparedStatement statement,
            int parameterIndex,
            Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.INTEGER);
            return;
        }

        statement.setInt(parameterIndex, value);
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
