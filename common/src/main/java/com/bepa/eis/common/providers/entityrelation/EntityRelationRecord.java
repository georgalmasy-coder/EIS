package com.bepa.eis.common.providers.entityrelation;

import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class EntityRelationRecord {

    private final Integer customerId;
    private final Integer projectId;

    private Integer entityRelationPK;

    private String entityCodeId;
    private Integer entityId;
    private EntityType entityType;

    private String relatedEntityCodeId;
    private Integer relatedEntityId;
    private EntityType relatedEntityType;

    private Integer createdByUserId;
    private LocalDateTime createdDate = LocalDateTime.now();

    private RelationType relationType;
    private Integer version;
    private Boolean latest;


    public EntityRelationRecord(Integer customerId, Integer projectId) {
        this.customerId = customerId;
        this.projectId = projectId;
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

    public Integer getRelatedEntityId() {
        return relatedEntityId;
    }
    public EntityType getRelatedEntityType() {
        return relatedEntityType;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }
    public Integer getCreatedById() {
        return createdByUserId;
    }
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = (createdDate == null) ? null : createdDate.toLocalDateTime();
    }

    public Integer getEntityRelationPK() {
        return entityRelationPK;
    }
    public void setEntityRelationPK(Integer entityRelationPK) {
        this.entityRelationPK = entityRelationPK;
    }
    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public void setRelatedEntityCodeId(String relatedEntityCodeId) {
        this.relatedEntityCodeId = relatedEntityCodeId;
    }

    public String getRelatedEntityCodeId() {
        return relatedEntityCodeId;
    }

    public void setRelatedEntityId(Integer relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public void setRelatedEntityType(EntityType relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getNextVersion() {
        return version != null ? version + 1 : 1;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest;
    }

    public Boolean getLatest() {
        return latest;
    }

    public void setRelationType(RelationType entityRelationType) {
        this.relationType = entityRelationType;
    }

    public void setRelationTypeId(Integer entityRelationTypeId) {
        this.relationType = RelationType.valueOf(entityRelationTypeId);
    }

    public String toString() {
        return "EntityRelationRecord [customerId=" + customerId + ", projectId=" + projectId + ", entityId=" + entityId
                + ", entityType=" + entityType + ", relatedEntityId=" + relatedEntityId + ", relatedEntityType="
                + relatedEntityType + ", createdByUserId=" + createdByUserId + ", createdDate=" + createdDate
                + ", relationType=" + relationType + ", version=" + version + ", latest=" + latest + "]";
    }
}
