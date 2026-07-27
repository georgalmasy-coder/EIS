package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.api.web.application.cache.CustomerBasisInfo;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.ProjectBasisInfo;
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

import java.sql.SQLException;

public class TopPanelProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(TopPanelProvider.class);

    private final TopPanel topPanel = new TopPanel(getWebSession());

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
        if (userName == null && getWebSession().getUserId() != null) {
            loadUserInfo();
        }
        return userName != null ? userName.getValue() : "";
    }

    private void loadCustomerInfo() throws SQLException {
        /* Get customer info */
        if (getWebSession() != null && getWebSession().getCustomerId() != null) {

            CustomerBasisInfo customerInfo = CustomerLookupCache.getCustomerInfo(getWebSession());

            if (customerInfo != null) {
                customerId = new CustomerId(customerInfo.getCustomerId());
                customerName = new CustomerName(customerInfo.getCustomerName());
            } else {
                customerId = new CustomerId(0);
                customerName = new CustomerName("--");
                log.error("Customer not found for customerId: {}", getWebSession().getCustomerId());
            }
        }
    }

    private void loadProjectInfo() throws SQLException {
        /* Get project info */

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            ProjectBasisInfo projectBasisInfo = CustomerLookupCache.getProjectInfo(getWebSession());

            if (projectBasisInfo != null) {
                projectId = new ProjectId(projectBasisInfo.getProjectId());
                projectName = new ProjectName(projectBasisInfo.getProjectName());
            } else {
                projectId = new ProjectId(0);
                projectName = new ProjectName("--");
                log.error("Project not found for projectId: {}", getWebSession().getProjectId());
            }

        }
    }

    private void loadUserInfo() throws SQLException {

        User user = CustomerLookupCache.getUser(getWebSession(), getWebSession().getUserId());

        if (user != null) {
            userId = new UserId( user.getUserId());
            userName = new UserName(user.getName());
        } else {
            userId = new UserId( 0);
            userName = new UserName("--");
            log.error("User not found for userId: {}", getWebSession().getUserId());
        }
    }
}
