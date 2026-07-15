package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.common.enums.entity.EntityType;

public class EntityKey {
    private final Integer customerId;
    private final Integer projectId;
    private final Integer entityId;
    private final EntityType entityType;
    private final Integer version;

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
