package com.bepa.eis.server.api.web.application.views.basis.baseline;

import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.Timestamp;

public class BaselineChangeRow {

    private String activity;
    private String id;
    private String name;
    private Timestamp lastModified;
    private String lastModifiedBy;
    private Integer lastModifiedById;
    private EntityType entityType;
    private Integer entityId;
    private Integer version;
    private boolean isNew;
    private boolean isActive;

    public BaselineChangeRow(EntityType entityType, Integer entityId, Integer version) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.version = version;
        this.isNew = version != null && version == 1;
        this.isActive = true;
    }

    public BaselineChangeRow(
            String activity,
            String id,
            String name,
            Timestamp lastModified,
            String lastModifiedBy
    ) {
        this.activity = activity;
        this.id = id;
        this.name = name;
        this.lastModified = lastModified;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Integer getLastModifiedById() {
        return lastModifiedById;
    }

    public void setLastModifiedById(Integer lastModifiedById) {
        this.lastModifiedById = lastModifiedById;
    }

    public void setNew() {
        this.isNew = true;
        this.isActive = true;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        return isActive;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

}