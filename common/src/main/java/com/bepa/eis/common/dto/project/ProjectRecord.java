package com.bepa.eis.common.dto.project;

import com.bepa.eis.common.enums.project.ProjectStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

public class ProjectRecord {

    private Integer projectPK;
    private Integer projectId;
    private Integer nextProjectId;
    private final Integer entityId = 1;
    private Integer version;
    private Boolean latest;

    private String projectName;
    private Integer customerId;
    private Integer ownerId;
    private Integer categoryId;
    private Integer priorityId;
    private ProjectStatus projectStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer budgetInDays;
    private BigDecimal budgetInValue;
    private Integer departmentId;

    private Integer changedByUserId;
    private Timestamp changedDateTime;

    public ProjectRecord() {
        projectPK = null;
        projectId = null;
        version = 1;
        latest = true;

        projectName = "";
        customerId = null;
        ownerId = null;
        categoryId = null;
        priorityId = null;
        projectStatus = ProjectStatus.CREATED;
        startDate = null;
        endDate = null;
        budgetInDays = null;
        budgetInValue = null;
        departmentId = null;

        changedByUserId = null;
        changedDateTime = null;
    }

    public Integer getProjectPK() {
        return projectPK;
    }

    public void setProjectPK(Integer projectPK) {
        this.projectPK = projectPK;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getNextProjectId() {
        return nextProjectId;
    }

    public void setNextProjectId(Integer nextProjectId) {
        this.nextProjectId = nextProjectId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getVersion() {
        return version == null ? 1 : version;
    }

    public void setVersion(Integer version) {
        this.version = version == null || version < 1 ? 1 : version;
    }

    public Boolean getLatest() {
        return latest != null && latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest != null && latest;
    }

    public boolean isLatest() {
        return latest != null && latest;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = safeText(projectName);
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(Integer priorityId) {
        this.priorityId = priorityId;
    }

    public ProjectStatus getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(ProjectStatus projectStatus) {
        this.projectStatus = projectStatus == null ? ProjectStatus.CREATED : projectStatus;
    }

    public Integer getProjectStatusId() {
        return projectStatus == null ? ProjectStatus.CREATED.getId() : projectStatus.getId();
    }

    public void setProjectStatusId(Integer projectStatusId) {
        this.projectStatus = ProjectStatus.fromIdOrDefault(
                projectStatusId,
                ProjectStatus.CREATED
        );
    }

    public String getProjectStatusCode() {
        return projectStatus == null ? ProjectStatus.CREATED.getCode() : projectStatus.getCode();
    }

    public String getProjectStatusLabel() {
        return projectStatus == null ? ProjectStatus.CREATED.getLabel() : projectStatus.getLabel();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public Timestamp getStartDateAsTimestamp() {
        return startDate == null
                ? null
                : Timestamp.valueOf(startDate.atStartOfDay());
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Timestamp getEndDateAsTimestamp() {
        return endDate == null
                ? null
                : Timestamp.valueOf(endDate.atStartOfDay());
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getBudgetInDays() {
        return budgetInDays;
    }

    public void setBudgetInDays(Integer budgetInDays) {
        this.budgetInDays = budgetInDays;
    }

    public BigDecimal getBudgetInValue() {
        return budgetInValue;
    }

    public void setBudgetInValue(BigDecimal budgetInValue) {
        this.budgetInValue = budgetInValue;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Integer changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public Timestamp getChangedDateTime() {
        return changedDateTime;
    }

    public void setChangedDateTime(Timestamp changedDateTime) {
        this.changedDateTime = changedDateTime;
    }

    public boolean isNew() {
        return projectPK == null;
    }

    public boolean hasValidProjectId() {
        return projectId != null && projectId > 0;
    }

    public boolean hasValidVersion() {
        return version != null && version > 0;
    }

    public boolean hasProjectName() {
        return projectName != null && !projectName.trim().isEmpty();
    }

    public boolean isActiveStatus() {
        return projectStatus != null && projectStatus.isActiveStatus();
    }

    public boolean isTerminalStatus() {
        return projectStatus != null && projectStatus.isTerminalStatus();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "ProjectRecord [projectPK=" + projectPK
                + ", projectId=" + projectId
                + ", version=" + version
                + ", latest=" + latest
                + ", projectName=" + projectName
                + ", customerId=" + customerId
                + ", ownerId=" + ownerId
                + ", categoryId=" + categoryId
                + ", priorityId=" + priorityId
                + ", projectStatus=" + getProjectStatusCode()
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", budgetInDays=" + budgetInDays
                + ", budgetInValue=" + budgetInValue
                + ", departmentId=" + departmentId
                + ", changedByUserId=" + changedByUserId
                + ", changedDateTime=" + changedDateTime
                + "]";
    }
}