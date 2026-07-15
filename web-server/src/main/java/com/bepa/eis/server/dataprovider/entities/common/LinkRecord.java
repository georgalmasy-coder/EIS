package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.entites.AbstractEntity;

import java.time.LocalDateTime;

public class LinkRecord {
    private final CustomerId customerId;
    private final ProjectId projectId;
    private final EntityId entityId;
    private final Version version;
    private final EntityType entityType;
    private final String description;
    private final String url;
    private ChangedBy changedBy;
    private final ChangedDateTime changedDateTime = new ChangedDateTime(LocalDateTime.now());

    public LinkRecord(AbstractEntity entity, String description, String url, Integer changedByUserId, LocalDateTime changedDate) {
        this.customerId = entity.getCustomerId();
        this.projectId = entity.getProjectId();
        this.entityId = entity.getEntityId();
        this.version = entity.getVersion();
        this.entityType = entity.getEntityType();
        this.description = description;
        this.url = url;
        this.changedBy = new ChangedBy(entity.getWebSession());
        this.changedBy.setValue(changedByUserId != null ? changedByUserId : entity.getChangedByUser().getValue());
        this.changedDateTime.setValue(changedDate != null ? changedDate : LocalDateTime.now());
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
    public ProjectId getProjectId() {
        return projectId;
    }
    public EntityId getEntityId() {
        return entityId;
    }
    public Version getVersion() {
        return version;
    }
    public EntityType getEntityType() {
        return entityType;
    }
    public String getDescription()  {
        return description;
    }
    public String getUrl()  {
        return url;
    }
    public ChangedBy getChangedByUserId() {
        return changedBy;
    }
    public ChangedDateTime getChangedDate() {
        return changedDateTime;
    }
}
