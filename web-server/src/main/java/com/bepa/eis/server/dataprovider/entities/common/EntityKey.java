package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.common.enums.entity.EntityType;

public class EntityKey {
    private Integer customerId;
    private Integer projectId;
    private Integer entityId;
    private EntityType entityType;
    private Integer version;

    public EntityKey(Integer customerId, Integer projectId, Integer entityId, EntityType entityType, Integer version) {
        this.customerId = customerId;
        this.projectId = projectId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.version = version;
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

    public EntityType getEntityType() {
        return entityType;
    }

    public Integer getEntityTypeAsInteger() {
        return entityType.getId();
    }
}
