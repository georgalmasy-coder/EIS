package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.api.web.application.cache.CustomerBasisInfo;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.ProjectBasisInfo;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.UserId;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
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

    public TopPanel getTopPanelBySession(PageType pageType) throws SQLException {
        findCustomerInfo();
        findProjectInfo();
        findUserInfo();
        if (pageType != null) {

            if (pageType.isHelpEnabled()) {
                HelpFileName pageName = new HelpFileName();
                pageName.setFieldNotVisible();
                pageName.setValue(pageType.getPageName());
                getTopPanelElement().addElement(pageName);
            }

            TopPanelTitle topPanelTitle = new TopPanelTitle();
            topPanelTitle.setFieldNotVisible();
            if (projectName != null && projectName.getValue() != null && !projectName.getValue().isBlank()) {
                topPanelTitle.setValue(projectName.getValue() + " - " + pageType.getTitle().toLowerCase());
            } else {
                topPanelTitle.setValue(pageType.getTitle().toLowerCase());
            }
            getTopPanelElement().addElement(topPanelTitle);

            if (pageType.getWorkspaceEyebrow() != null && !pageType.getWorkspaceEyebrow().isBlank()) {
                WorkspaceEyebrow workspaceEyebrow = new WorkspaceEyebrow();
                workspaceEyebrow.setFieldNotVisible();
                workspaceEyebrow.setValue(pageType.getWorkspaceEyebrow());
                getTopPanelElement().addElement(workspaceEyebrow);
            }

            if (pageType.getWorkspaceHeading() != null && !pageType.getWorkspaceHeading().isBlank()) {
                WorkspaceHeading workspaceHeading = new WorkspaceHeading();
                workspaceHeading.setFieldNotVisible();
                workspaceHeading.setValue(pageType.getWorkspaceHeading());
                getTopPanelElement().addElement(workspaceHeading);
            }

            if (pageType.getWorkspaceHelpText() != null && !pageType.getWorkspaceHelpText().isBlank()) {
                WorkspaceHelpText workspaceHelpText = new WorkspaceHelpText();
                workspaceHelpText.setFieldNotVisible();
                workspaceHelpText.setValue(pageType.getWorkspaceHelpText());
                getTopPanelElement().addElement(workspaceHelpText);
            }

        }
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
        Name name = new Name();
        name.setValue(getUserName());
        getTopPanelElement().addElement(name);
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

    public String getUserName() {
        if (userName == null && getWebSession().getUserId() != null) {
            loadUserInfo();
        }
        return userName != null ? userName.getValue() : "";
    }

    private void loadCustomerInfo() {
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

    private void loadProjectInfo(){
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

    private void loadUserInfo() {

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

    private static class HelpFileName extends AbstractString {

        @Override
        public String getFieldName() {
            return "HelpFileName";
        }

        @Override
        public String getFieldLabelName() {
            return "";
        }

        @Override
        public String getFieldHeaderName() {
            return "";
        }
    }

    private static class TopPanelTitle extends AbstractString {

        @Override
        public String getFieldName() {
            return "TopPanelTitle";
        }

        @Override
        public String getFieldLabelName() {
            return "";
        }

        @Override
        public String getFieldHeaderName() {
            return "";
        }

    }

    private static class WorkspaceEyebrow extends AbstractString {

        @Override
        public String getFieldName() {
            return "WorkspaceEyebrow";
        }

        @Override
        public String getFieldLabelName() {
            return "";
        }

        @Override
        public String getFieldHeaderName() {
            return "";
        }

    }

    private static class WorkspaceHeading extends AbstractString {

        @Override
        public String getFieldName() {
            return "WorkspaceHeading";
        }

        @Override
        public String getFieldLabelName() {
            return "";
        }

        @Override
        public String getFieldHeaderName() {
            return "";
        }
    }

    private static class WorkspaceHelpText extends AbstractString {

        @Override
        public String getFieldName() {
            return "WorkspaceHelpText";
        }

        @Override
        public String getFieldLabelName() {
            return "";
        }

        @Override
        public String getFieldHeaderName() {
            return "";
        }
    }

    private static class Name extends AbstractString {

        @Override
        public String getFieldName() {
            return "Name";
        }

        @Override
        public String getFieldLabelName() {
            return "User Name";
        }

        @Override
        public String getFieldHeaderName() {
            return "User Name";
        }

        @Override
        public Integer getFieldMinLength() {
            return 5;
        }

        @Override
        public Integer getFieldMaxLength() {
            return 100;
        }

        @Override
        public Integer getFieldDisplayLength() {
            return 25;
        }
    }
}
