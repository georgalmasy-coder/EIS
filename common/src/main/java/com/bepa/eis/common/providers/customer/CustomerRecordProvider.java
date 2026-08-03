package com.bepa.eis.common.providers.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CustomerRecordProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerRecordProvider.class);

    private static final String SELECT_NEXT_CUSTOMER_ID_SQL =
            "SELECT ISNULL(MAX(CustomerId), 0) + 1 AS NextCustomerId " +
                    "FROM [dbo].[CUSTOMER] WITH (UPDLOCK, HOLDLOCK) " +
                    "WHERE Latest = 1 ";

    private static final String SELECT_ALL_LATEST_CUSTOMERS_SQL =
            "SELECT " +
                    "C.CustomerPK, " +
                    "C.CustomerId, " +
                    "C.Version, " +
                    "C.CustomerName, " +
                    "C.CvrNumber, " +
                    "C.VatNumber, " +
                    "C.Phone, " +
                    "C.Address, " +
                    "C.ZipCode, " +
                    "C.City, " +
                    "C.Country, " +
                    "C.ContactName, " +
                    "C.ContactEmail, " +
                    "C.CustomerStatus, " +
                    "C.CustomerMfaPolicy, " +
                    "C.ChangedByUserId, " +
                    "C.ChangedDateTime, " +
                    "CreatedCustomer.CreatedDateTime, " +
                    "C.Latest " +
                    "FROM [dbo].[CUSTOMER] C " +
                    "OUTER APPLY ( " +
                    "    SELECT TOP (1) C1.ChangedDateTime AS CreatedDateTime " +
                    "    FROM [dbo].[CUSTOMER] C1 " +
                    "    WHERE C1.CustomerId = C.CustomerId " +
                    "    ORDER BY C1.Version ASC, C1.CustomerPK ASC " +
                    ") CreatedCustomer " +
                    "WHERE C.Latest = 1 " +
                    "ORDER BY C.CustomerName ASC, C.CustomerId ASC ";

    private static final String SELECT_LATEST_CUSTOMER_BY_CUSTOMER_ID_SQL =
            "SELECT TOP (1) " +
                    "C.CustomerPK, " +
                    "C.CustomerId, " +
                    "C.Version, " +
                    "C.CustomerName, " +
                    "C.CvrNumber, " +
                    "C.VatNumber, " +
                    "C.Phone, " +
                    "C.Address, " +
                    "C.ZipCode, " +
                    "C.City, " +
                    "C.Country, " +
                    "C.ContactName, " +
                    "C.ContactEmail, " +
                    "C.CustomerStatus, " +
                    "C.CustomerMfaPolicy, " +
                    "C.ChangedByUserId, " +
                    "C.ChangedDateTime, " +
                    "CreatedCustomer.CreatedDateTime, " +
                    "C.Latest " +
                    "FROM [dbo].[CUSTOMER] C " +
                    "OUTER APPLY ( " +
                    "    SELECT TOP (1) C1.ChangedDateTime AS CreatedDateTime " +
                    "    FROM [dbo].[CUSTOMER] C1 " +
                    "    WHERE C1.CustomerId = C.CustomerId " +
                    "    ORDER BY C1.Version ASC, C1.CustomerPK ASC " +
                    ") CreatedCustomer " +
                    "WHERE C.CustomerId = ? " +
                    "  AND C.Latest = 1 " +
                    "ORDER BY C.Version DESC ";

    private static final String SELECT_CUSTOMER_BY_CUSTOMER_PK_SQL =
            "SELECT " +
                    "C.CustomerPK, " +
                    "C.CustomerId, " +
                    "C.Version, " +
                    "C.CustomerName, " +
                    "C.CvrNumber, " +
                    "C.VatNumber, " +
                    "C.Phone, " +
                    "C.Address, " +
                    "C.ZipCode, " +
                    "C.City, " +
                    "C.Country, " +
                    "C.ContactName, " +
                    "C.ContactEmail, " +
                    "C.CustomerStatus, " +
                    "C.CustomerMfaPolicy, " +
                    "C.ChangedByUserId, " +
                    "C.ChangedDateTime, " +
                    "CreatedCustomer.CreatedDateTime, " +
                    "C.Latest " +
                    "FROM [dbo].[CUSTOMER] C " +
                    "OUTER APPLY ( " +
                    "    SELECT TOP (1) C1.ChangedDateTime AS CreatedDateTime " +
                    "    FROM [dbo].[CUSTOMER] C1 " +
                    "    WHERE C1.CustomerId = C.CustomerId " +
                    "    ORDER BY C1.Version ASC, C1.CustomerPK ASC " +
                    ") CreatedCustomer " +
                    "WHERE C.CustomerPK = ? ";

    private static final String SELECT_MAX_VERSION_BY_CUSTOMER_ID_SQL =
            "SELECT ISNULL(MAX(Version), 0) AS MaxVersion " +
                    "FROM [dbo].[CUSTOMER] WITH (UPDLOCK, HOLDLOCK) " +
                    "WHERE CustomerId = ? ";

    private static final String UPDATE_LATEST_OFF_SQL =
            "UPDATE [dbo].[CUSTOMER] " +
                    "SET Latest = 0 " +
                    "WHERE CustomerId = ? " +
                    "  AND Latest = 1 ";

    private static final String INSERT_CUSTOMER_SQL =
            "INSERT INTO [dbo].[CUSTOMER] ( " +
                    "CustomerId, " +
                    "Version, " +
                    "CustomerName, " +
                    "CvrNumber, " +
                    "VatNumber, " +
                    "Phone, " +
                    "Address, " +
                    "ZipCode, " +
                    "City, " +
                    "Country, " +
                    "ContactName, " +
                    "ContactEmail, " +
                    "CustomerStatus, " +
                    "CustomerMfaPolicy, " +
                    "ChangedByUserId, " +
                    "ChangedDateTime, " +
                    "Latest " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME(), ?) ";

    public CustomerRecordProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer createCustomer(CustomerRecord customer) {
        if (customer == null) {
            return null;
        }

        if (!customer.hasCustomerName()) {
            log.warn("Customer could not be created because CustomerName is missing.");
            return null;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                Integer customerId = nextCustomerId(connection);

                customer.setCustomerId(customerId);
                customer.setVersion(1);
                customer.setLatest(true);

                if (customer.getCustomerStatus() == null) {
                    customer.setCustomerStatus(CustomerStatus.CREATED);
                }

                customer.setCustomerMfaPolicy(customer.getCustomerMfaPolicy());

                Integer customerPK = insertCustomer(
                        connection,
                        customer
                );

                connection.commit();

                customer.setCustomerPK(customerPK);

                log.info(
                        "Customer created. customerPK={}, customerId={}, version={}, cvrNumber={}, vatNumber={}, customerMfaPolicy={}",
                        customerPK,
                        customerId,
                        customer.getVersion(),
                        customer.getCvrNumber(),
                        customer.getVatNumber(),
                        customer.getCustomerMfaPolicy()
                );

                return customerId;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                restoreAutoCommitQuietly(connection);
            }
        } catch (SQLException e) {
            log.error("Error creating customer.", e);
            return null;
        }
    }

    public Integer updateCustomer(CustomerRecord updatedCustomer) {
        if (updatedCustomer == null || updatedCustomer.getCustomerId() == null) {
            return null;
        }

        if (!updatedCustomer.hasCustomerName()) {
            log.warn(
                    "Customer could not be updated because CustomerName is missing. customerId={}",
                    updatedCustomer.getCustomerId()
            );
            return null;
        }

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                CustomerRecord currentCustomer = getLatestCustomerByCustomerId(
                        connection,
                        updatedCustomer.getCustomerId()
                );

                if (currentCustomer == null) {
                    log.warn(
                            "Customer could not be updated because latest customer was not found. customerId={}",
                            updatedCustomer.getCustomerId()
                    );
                    rollbackQuietly(connection);
                    return null;
                }

                Integer nextVersion = nextVersion(
                        connection,
                        updatedCustomer.getCustomerId()
                );

                updateLatestOff(
                        connection,
                        updatedCustomer.getCustomerId()
                );

                updatedCustomer.setVersion(nextVersion);
                updatedCustomer.setLatest(true);
                updatedCustomer.setCustomerMfaPolicy(updatedCustomer.getCustomerMfaPolicy());

                Integer customerPK = insertCustomer(
                        connection,
                        updatedCustomer
                );

                connection.commit();

                updatedCustomer.setCustomerPK(customerPK);

                log.info(
                        "Customer updated. customerPK={}, customerId={}, version={}, cvrNumber={}, vatNumber={}, customerMfaPolicy={}",
                        customerPK,
                        updatedCustomer.getCustomerId(),
                        updatedCustomer.getVersion(),
                        updatedCustomer.getCvrNumber(),
                        updatedCustomer.getVatNumber(),
                        updatedCustomer.getCustomerMfaPolicy()
                );

                return customerPK;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                restoreAutoCommitQuietly(connection);
            }
        } catch (SQLException e) {
            log.error(
                    "Error updating customer. customerId={}",
                    updatedCustomer.getCustomerId(),
                    e
            );
            return null;
        }
    }

    public boolean updateCustomerStatus(
            Integer customerId,
            CustomerStatus customerStatus,
            Integer changedByUserId
    ) {
        if (customerId == null || customerStatus == null) {
            return false;
        }

        CustomerRecord current = getLatestCustomerByCustomerId(customerId);

        if (current == null) {
            return false;
        }

        current.setCustomerStatus(customerStatus);
        current.setChangedByUserId(changedByUserId);

        Integer customerPK = updateCustomer(current);

        return customerPK != null;
    }

    public List<CustomerRecord> getAllLatestCustomers() {
        List<CustomerRecord> customers = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_LATEST_CUSTOMERS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                customers.add(mapCustomer(resultSet));
            }
        } catch (SQLException e) {
            log.error("Error loading all latest customers.", e);
        }

        return customers;
    }

    public CustomerRecord getLatestCustomerByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection()) {
            return getLatestCustomerByCustomerId(
                    connection,
                    customerId
            );
        } catch (SQLException e) {
            log.error("Error loading latest customer. customerId={}", customerId, e);
            return null;
        }
    }

    public CustomerRecord getCustomerByCustomerPK(Integer customerPK) {
        if (customerPK == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_CUSTOMER_BY_CUSTOMER_PK_SQL)) {

            statement.setInt(1, customerPK);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading customer by CustomerPK. customerPK={}", customerPK, e);
        }

        return null;
    }

    private CustomerRecord getLatestCustomerByCustomerId(
            Connection connection,
            Integer customerId
    ) throws SQLException {
        if (customerId == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_CUSTOMER_BY_CUSTOMER_ID_SQL)) {
            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        }

        return null;
    }

    private Integer nextCustomerId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_NEXT_CUSTOMER_ID_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("NextCustomerId");
            }

            return 1;
        }
    }

    private Integer nextVersion(
            Connection connection,
            Integer customerId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_MAX_VERSION_BY_CUSTOMER_ID_SQL)) {
            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("MaxVersion") + 1;
                }

                return 1;
            }
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

    private Integer insertCustomer(
            Connection connection,
            CustomerRecord customer
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_CUSTOMER_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setInt(1, customer.getCustomerId());
            statement.setInt(2, customer.getVersion());
            statement.setString(3, customer.getCustomerName());
            statement.setString(4, nullIfBlank(customer.getCvrNumber()));
            statement.setString(5, nullIfBlank(customer.getVatNumber()));
            statement.setString(6, nullIfBlank(customer.getPhone()));
            statement.setString(7, nullIfBlank(customer.getAddress()));
            statement.setString(8, nullIfBlank(customer.getZipCode()));
            statement.setString(9, nullIfBlank(customer.getCity()));
            statement.setString(10, nullIfBlank(customer.getCountry()));
            statement.setString(11, nullIfBlank(customer.getContactName()));
            statement.setString(12, nullIfBlank(customer.getContactEmail()));
            statement.setInt(13, customer.getCustomerStatusId());
            statement.setString(14, customer.getCustomerMfaPolicy());

            if (customer.getChangedByUserId() == null) {
                statement.setNull(15, Types.INTEGER);
            } else {
                statement.setInt(15, customer.getChangedByUserId());
            }

            statement.setBoolean(16, customer.isLatest());

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

    private CustomerRecord mapCustomer(ResultSet resultSet) throws SQLException {
        CustomerRecord customer = new CustomerRecord();

        int customerPK = resultSet.getInt("CustomerPK");
        customer.setCustomerPK(resultSet.wasNull() ? null : customerPK);

        int customerId = resultSet.getInt("CustomerId");
        customer.setCustomerId(resultSet.wasNull() ? null : customerId);

        int version = resultSet.getInt("Version");
        customer.setVersion(resultSet.wasNull() ? null : version);

        customer.setCustomerName(resultSet.getString("CustomerName"));
        customer.setCvrNumber(resultSet.getString("CvrNumber"));
        customer.setVatNumber(resultSet.getString("VatNumber"));
        customer.setPhone(resultSet.getString("Phone"));

        customer.setAddress(resultSet.getString("Address"));
        customer.setZipCode(resultSet.getString("ZipCode"));
        customer.setCity(resultSet.getString("City"));
        customer.setCountry(resultSet.getString("Country"));

        customer.setContactName(resultSet.getString("ContactName"));
        customer.setContactEmail(resultSet.getString("ContactEmail"));

        int customerStatus = resultSet.getInt("CustomerStatus");
        customer.setCustomerStatusId(resultSet.wasNull() ? null : customerStatus);

        customer.setCustomerMfaPolicy(resultSet.getString("CustomerMfaPolicy"));

        int changedByUserId = resultSet.getInt("ChangedByUserId");
        customer.setChangedByUserId(resultSet.wasNull() ? null : changedByUserId);

        customer.setChangedDateTime(resultSet.getTimestamp("ChangedDateTime"));
        customer.setCreatedDateTime(resultSet.getTimestamp("CreatedDateTime"));

        boolean latest = resultSet.getBoolean("Latest");
        customer.setLatest(resultSet.wasNull() ? null : latest);

        return customer;
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
            // Ignore auto commit restore.
        }
    }
}
