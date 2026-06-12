package com.bepa.eis.server.dataprovider.misc;

import com.bepa.eis.server.api.DTO.CustomerProject;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerProjectProvider extends GenericProvider {
    private static final Logger log = LoggerFactory.getLogger(CustomerProjectProvider.class);

    private static final String CUSTOMER_PROJECT_SQL =
            "SELECT P.ProjectId, P.ProjectName, C.CustomerId, C.CustomerName  FROM PROJECT P, CUSTOMER C " +
                    "WHERE P.ProjectId IN (SELECT ProjectId FROM USER_PROJECT WHERE UserId IN (SELECT UserId FROM USERS WHERE Email = ?)) " +
                    "AND P.Active = 1 " +
                    "AND P.CustomerId = C.CustomerId " +
                    "AND C.Active = 1";

    private static final String CUSTOMER_ID_BY_PROJECT_ID_SQL =
            "SELECT C.CustomerId FROM PROJECT P, CUSTOMER C " +
                    "WHERE P.ProjectId = ? " +
                    "AND P.Active = 1 " +
                    "AND P.CustomerId = C.CustomerId " +
                    "AND C.Active = 1";


    public CustomerProjectProvider(WebSession webSession) {
        super(webSession);
    }


    /**
     * Retrieves the customer project associated with the given session ID.
     * @param sessionId The session ID to retrieve the customer project for.
     * @return The customer project associated with the session ID.
     * @throws SQLException If an error occurs while retrieving the customer project.
     */
    public CustomerProject getCustomerProject(String sessionId)  {

        CustomerProject customerProject = new CustomerProject();

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(CUSTOMER_PROJECT_SQL)) {

                setString(ps, sessionId, 1);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        int customerId = rs.getInt("CustomerId");
                        String customerName = rs.getString("CustomerName");
                        int projectId = rs.getInt("ProjectId");
                        String projectName = rs.getString("ProjectName");

                        customerProject.addCustomerAndProject(customerId, customerName, projectId, projectName);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error getting customer and projects: {}", e.getMessage(), e);
        }

        return customerProject;
    }

    public Integer getCustomerIdByProjectId(Integer projectId)  {

        Integer customerId = null;

        try {
            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(CUSTOMER_ID_BY_PROJECT_ID_SQL)) {

                setInt(ps, projectId, 1);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        customerId = rs.getInt("CustomerId");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error getting customer Id by project Id : {}", e.getMessage(), e);
        }

        return customerId;
    }
}
