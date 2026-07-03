package com.bepa.eis.server.api.web.application.views.basis.baseline;

import java.sql.Timestamp;

public class Baseline {

    private Integer baselineId;
    private Integer customerId;
    private Integer projectId;
    private String tagName;
    private String description;
    private Integer changedByUserId;
    private String changedBy;
    private Timestamp changedDateTime;
    private Timestamp previousBaselineDateTime;

    public Integer getBaselineId() {
        return baselineId;
    }

    public void setBaselineId(Integer baselinePK) {
        this.baselineId = baselinePK;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Integer changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Timestamp getChangedDateTime() {
        return changedDateTime;
    }

    public void setChangedDateTime(Timestamp changedDateTime) {
        this.changedDateTime = changedDateTime;
    }

    public Timestamp getPreviousBaselineDateTime() {
        return previousBaselineDateTime;
    }

    public void setPreviousBaselineDateTime(Timestamp previousBaselineDateTime) {
        this.previousBaselineDateTime = previousBaselineDateTime;
    }

}