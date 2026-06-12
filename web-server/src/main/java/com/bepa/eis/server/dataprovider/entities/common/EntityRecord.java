package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.common.dto.WebSession;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EntityRecord {

    private WebSession webSession;
    private Integer customerId;
    private Integer projectId;
    private Integer entityId;
    private Integer entityType;
    private Integer version;
    private Integer changedByUserId;
    private Timestamp changedDateTime;
    private Boolean latest;
    private Boolean active;
    private List<EntityElementRecord> entityElementRecords = new ArrayList<>();

    public EntityRecord(WebSession webSession,
                        Integer customerId,
                        Integer projectId,
                        Integer entityId,
                        Integer entityType,
                        Integer version,
                        Integer changedByUserId,
                        Timestamp changedDateTime,
                        Boolean latest,
                        Boolean active) {
        this.webSession = webSession;
        this.customerId = customerId;
        this.projectId = projectId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.version = version;
        this.changedByUserId = changedByUserId;
        this.changedDateTime = changedDateTime;
        this.latest = latest;
        this.active = active;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public String getKey() {
        return customerId + "_" + projectId + "_" + entityId + "_" + entityType + "_" + version;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getEntityType() {
        return entityType;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getChangedByUserId() {
        return changedByUserId;
    }

    public Timestamp getChangedDateTime() {
        return changedDateTime;
    }

    public Boolean isLatest() {
        return latest;
    }

    public Boolean isActive() {
        return active;
    }
    public List<EntityElementRecord> getEntityElementRecords() {
        return entityElementRecords;
    }

    public void addEntityElementRecord(EntityElementRecord entityElementRecord) {
        entityElementRecords.add(entityElementRecord);
    }
}

