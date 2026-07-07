package com.bepa.eis.server.dataprovider.customer;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.customer.CustomerStatus;
import com.bepa.eis.common.enums.project.ProjectStatus;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.entities.ProjectEntityProvider;
import com.bepa.eis.server.dataprovider.project.InstallDefaultConfiguration;
import com.bepa.eis.server.entites.project.ProjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomerProvider.class);

    private static final String CUSTOMER_BY_USER_ID_SQL =
            "SELECT C.* " +
                    "FROM CUSTOMER C " +
                    "WHERE C.Latest = 1 " +
                    "AND C.CustomerStatus IN (" + CustomerStatus.getActiveStatusIds() + ") " +
                    "AND C.CustomerId = ? ";

    public CustomerProvider(WebSession webSession) {
        super(webSession);
    }

    public CustomerRecord getCustomersByCustomerId(Integer customerId) {

        CustomerRecord customer = null;

        if (customerId != null) {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(CUSTOMER_BY_USER_ID_SQL)) {

                statement.setInt(1, customerId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        customer = new CustomerRecord();

                        customer.setCustomerId(resultSet.getInt("CustomerId"));
                        customer.setCustomerName(resultSet.getString("CustomerName"));
                        customer.setCvrNumber(resultSet.getString("CvrNumber"));
                        customer.setPhone(resultSet.getString("Phone"));
                        customer.setContactEmail(resultSet.getString("ContactEmail"));

                        CustomerStatus customerStatus = CustomerStatus.fromId(resultSet.getInt("CustomerStatus"));
                        customer.setCustomerStatus(customerStatus);

                        customer.setChangedByUserId(resultSet.getInt("ChangedByUserId"));
                        customer.setChangedDateTime(resultSet.getTimestamp("ChangedDateTime"));


                        customer.setCustomerMfaPolicy(resultSet.getString("CustomerMfaPolicy"));
                    }
                }
            } catch (SQLException e) {
                log.error("Error loading customer for customerId: {}", customerId, e);
            }
        }

        return customer;
    }


}