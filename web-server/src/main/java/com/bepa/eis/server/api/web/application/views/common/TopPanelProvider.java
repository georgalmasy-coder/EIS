package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.UserId;
import com.bepa.eis.server.dataprovider.fields.strings.CustomerName;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.strings.UserName;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TopPanelProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(TopPanelProvider.class);

    private final TopPanel topPanel = new TopPanel(getWebSession());

    private static final String GET_CUSTOMER_BY_CUSTOMER_ID_SQL =
            "SELECT * FROM CUSTOMER WHERE CustomerId = ?";

    private static final String GET_PROJECT_BY_PROJECT_ID_SQL =
            "SELECT * FROM PROJECT WHERE ProjectId = ?";

    private static final String GET_USER_BY_USER_ID_SQL =
            "SELECT * FROM USERS WHERE UserId = ?";

    public TopPanelProvider(WebSession webSession) {
        super(webSession);
    }

    private CustomerId customerId = null;
    private CustomerName customerName = null;
    private ProjectId projectId = null;
    private ProjectName projectName = null;
    private UserId userId = null;
    private UserName userName = null;

    /**
     * Retrieves a {@code Customer} object using the customer ID from the provided {@code WebSession}.
     *
     * @return the {@code Customer} object corresponding to the customer ID provided in the {@code WebSession}.
     * @throws SQLException if no customer is found for the given customer ID or if a database access error occurs.
     */
    public TopPanel getTopPanelBySession() throws SQLException {
        findCustomerInfo();
        findProjectInfo();
        findUserInfo();
        return topPanel;
    }

    private void findCustomerInfo() throws SQLException {
        getTopPanelElement().addElement(new CustomerId(getCustomerId()));
        getTopPanelElement().addElement(new CustomerName(getCustomerName()));
    }

    private void findProjectInfo() throws SQLException {
        getTopPanelElement().addElement(new ProjectId(getProjectId()));
        getTopPanelElement().addElement(new ProjectName(getProjectName()));
    }

    private void findUserInfo() throws SQLException {
        getTopPanelElement().addElement(new UserId(getUserId()));
        getTopPanelElement().addElement(new UserName(getUserName()));
    }

    private ListOfElements getTopPanelElement() {
        return topPanel.getTopPanelElements();
    }

    public Integer getCustomerId() throws SQLException {
        if (customerId == null && getWebSession().getCustomerId() != null) {
            loadCustomerInfo();
        }
        return customerId != null ? customerId.getValue() : 0;
    }

    public String getCustomerName() throws SQLException {
        if (customerName == null && getWebSession().getCustomerId() != null) {
            loadCustomerInfo();
        }
        return customerName != null ? customerName.getValue() : "";
    }

    public Integer getProjectId() throws SQLException {
        if (projectId == null && getWebSession().getProjectId() != null) {
            loadProjectInfo();
        }
        return projectId != null ? projectId.getValue() : 0;
    }

    public String getProjectName() throws SQLException {
        if (projectName == null && getWebSession().getProjectId() != null) {
            loadProjectInfo();
        }
        return projectName != null ? projectName.getValue() : "";
    }

    public Integer getUserId() throws SQLException {
        if (userId == null && getWebSession().getUserId() != null) {
            loadUserInfo();
        }
        return userId != null ? userId.getValue() : 0;
    }

    public String getUserName() throws SQLException {
        if (userId == null && getWebSession().getUserId() != null) {
            loadProjectInfo();
        }
        return userName != null ? userName.getValue() : "";
    }

    private void loadCustomerInfo() throws SQLException {
        /* Get customer info */
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_CUSTOMER_BY_CUSTOMER_ID_SQL)) {

            setInt(ps, getWebSession().getCustomerId(), 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.error("No customer found for customerId: {}", getWebSession().getCustomerId());
                    throw new SQLException("No customer found for customerId: " + getWebSession().getCustomerId());
                }

                customerId = new CustomerId(rs.getInt(CustomerId.FIELD_NAME));
                customerName = new CustomerName(rs.getString(CustomerName.FIELD_NAME));
            }
        }
    }

    private void loadProjectInfo() throws SQLException {
        /* Get project info */
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_PROJECT_BY_PROJECT_ID_SQL)) {

            setInt(ps, getWebSession().getProjectId(), 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.error("No project found for projectId: {}", getWebSession().getProjectId());
                    throw new SQLException("No project found for projectId: " + getWebSession().getProjectId());
                }

                projectId = new ProjectId(rs.getInt(ProjectId.FIELD_NAME));
                projectName = new ProjectName(rs.getString(ProjectName.FIELD_NAME));
            }
        }

    }

    private void loadUserInfo() throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_USER_BY_USER_ID_SQL)) {

            setInt(ps, getWebSession().getUserId(), 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.error("No user found for userId: {}", getWebSession().getUserId());
                    throw new SQLException("No user found for userId: " + getWebSession().getUserId());
                }

                userId = new UserId(rs.getInt(UserId.FIELD_NAME));
                userName = new UserName(rs.getString(UserName.FIELD_NAME));
            }
        }

    }
}
