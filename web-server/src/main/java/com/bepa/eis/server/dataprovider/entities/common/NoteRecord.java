package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.time.LocalDateTime;

public class NoteRecord {
    private Integer customerId;
    private Integer projectId;
    private Integer entityId;
    private Integer version;
    private EntityType entityType;
    private String noteText;
    private Integer changedByUserId;
    private LocalDateTime changedDate = LocalDateTime.now();

    public NoteRecord(AbstractEntity entity, String noteText, Integer changedByUserId, LocalDateTime changedDate) {
        this.customerId = entity.getCustomerId();
        this.projectId = entity.getProjectId();
        this.entityId = entity.getEntityId();
        this.version = entity.getVersion();
        this.entityType = entity.getEntityType();
        this.noteText = noteText;
        this.changedByUserId = changedByUserId != null ? changedByUserId : entity.getChangedByUserId();
        this.changedDate = changedDate != null ? changedDate : LocalDateTime.now();
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
    public Integer getVersion() {
        return version;
    }
    public EntityType getEntityType() {
        return entityType;
    }
    public String getNoteText() {
        return noteText;
    }
    public Integer getChangedByUserId() {
        return changedByUserId;
    }
    public LocalDateTime getChangedDate() {
        return changedDate;
    }
}
