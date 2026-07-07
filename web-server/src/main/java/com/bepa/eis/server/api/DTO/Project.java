package com.bepa.eis.server.api.DTO;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class Project {

    private ListOfElements projectElements = null;

    private final WebSession webSession;

    private ProjectId projectId;
    private Version version;
    private ProjectName projectName;
    private CustomerId customerId;
    private ProjectOwner ownerId;
    private ProjectCategory categoryId;
    private ProjectPriority priorityId;
    private ProjectStatus projectStatus;
    private StartDate startDate;
    private EndDate endDate;
    private BudgetInDays budgetInDays;
    private BudgetInValue budgetInValue;
    private CustomerDepartment departmentId;
    private ChangedDateTime changedDateTime;

    public Project(WebSession webSession) {
        this.webSession = webSession;
    }

    private WebSession getWebSession() {
        return webSession;
    }

    public ListOfElements getProjectElements() {
        if (projectElements == null) {
            projectElements = new ListOfElements(
                    getWebSession(),
                    this.getClass().getSimpleName()
            );
        }

        return projectElements;
    }

    public void setProjectId(ProjectId projectId) {
        this.projectId = projectId;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    public void setProjectName(ProjectName projectName) {
        this.projectName = projectName;
    }

    public ProjectName getProjectName() {
        return projectName;
    }

    public void setProjectCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    public CustomerId getProjectCustomerId() {
        return customerId;
    }

    public void setProjectOwnerId(ProjectOwner ownerId) {
        this.ownerId = ownerId;
    }

    public ProjectOwner getProjectOwnerId() {
        return ownerId;
    }

    public void setProjectCategoryId(ProjectCategory categoryId) {
        this.categoryId = categoryId;
    }

    public ProjectCategory getProjectCategoryId() {
        return categoryId;
    }

    public void setProjectPriorityId(ProjectPriority priorityId) {
        this.priorityId = priorityId;
    }

    public ProjectPriority getProjectPriorityId() {
        return priorityId;
    }

    public void setProjectStatus(ProjectStatus projectStatus) {
        this.projectStatus = projectStatus;
    }

    public ProjectStatus getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStartDate(StartDate startDate) {
        this.startDate = startDate;
    }

    public StartDate getProjectStartDate() {
        return startDate;
    }

    public void setProjectEndDate(EndDate endDate) {
        this.endDate = endDate;
    }

    public EndDate getProjectEndDate() {
        return endDate;
    }

    public void setBudgetInDays(BudgetInDays budgetInDays) {
        this.budgetInDays = budgetInDays;
    }

    public BudgetInDays getBudgetInDays() {
        return budgetInDays;
    }

    public void setBudgetInValue(BudgetInValue budgetInValue) {
        this.budgetInValue = budgetInValue;
    }

    public BudgetInValue getBudgetInValue() {
        return budgetInValue;
    }

    public void setDepartment(CustomerDepartment departmentId) {
        this.departmentId = departmentId;
    }

    public CustomerDepartment getDepartment() {
        return departmentId;
    }

    public void setChangedDateTime(ChangedDateTime changedDateTime) {
        this.changedDateTime = changedDateTime;
    }

    public ChangedDateTime getChangedDateTime() {
        return changedDateTime;
    }
}