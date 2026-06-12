package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.integers.ids.*;
import com.bepa.eis.server.dataprovider.fields.lookups.common.CreatedBy;
import com.bepa.eis.server.dataprovider.fields.strings.RelatedEntityTypeName;
import com.bepa.eis.server.dataprovider.fields.timestamp.CreatedDateTime;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.common.enums.entity.EntityType;

import java.sql.Timestamp;

public class EntityRelation extends ListOfElements {

    private final ListOfElements entityRelationElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());

    private EntityRelationPK entityRelationPK;

    private EntityId entityId;
    private EntityTypeId entityTypeId;
    private EntityType entityType;

    private RelatedEntityId relatedEntityId;
    private RelatedEntityTypeId relatedEntityTypeId;
    private EntityType relatedEntityType;

    private CreatedBy createdBy;
    private CreatedDateTime createdDateTime;

    private RelatedEntityTypeName relatedEntityTypeName;

    public EntityRelation(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public void setEntityRelationPK(Integer entityRelationPK) {
        this.entityRelationPK = new EntityRelationPK(entityRelationPK);
        this.addElement(this.entityRelationPK);
    }

    public EntityRelationPK getEntityRelationPK() {
        return entityRelationPK;
    }

    public ListOfElements getEntityRelationsElements() {
        return entityRelationElements;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = new EntityId(entityId);
        this.addElement(this.entityId);
    }

    public EntityId getEntityId() {
        return entityId;
    }

    public void setEntityTypeId(Integer entityTypeId) {
        this.entityTypeId = new EntityTypeId(entityTypeId);
        this.entityType = EntityType.fromId(entityTypeId);
        this.addElement(this.entityTypeId);
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setRelatedEntityId(Integer relatedEntityId) {
        this.relatedEntityId = new RelatedEntityId(relatedEntityId);
        this.addElement(this.relatedEntityId);
    }

    public RelatedEntityId getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityTypeId(Integer relatedEntityTypeId) {
        this.relatedEntityTypeId = new RelatedEntityTypeId(relatedEntityTypeId);
        this.relatedEntityType = EntityType.fromId(relatedEntityTypeId);
        this.addElement(this.relatedEntityTypeId);

        relatedEntityTypeName = new RelatedEntityTypeName(relatedEntityType.getDescription());
        addElement(relatedEntityTypeName);
    }

    public EntityType getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setCreatedBy(WebSession webSession, Integer createdBy) {
        this.createdBy = new CreatedBy(webSession, createdBy);
        this.addElement(this.createdBy);
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setCreatedDateTime(Timestamp createdDateTime) {
        this.createdDateTime = new CreatedDateTime(createdDateTime);
        this.addElement(this.createdDateTime);
    }

    public CreatedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public String toString() {
        String fromEntity = "Relation from " + entityType.getDescription() + " - entity Id " +  entityId.getValue();
        String toEntity = " to " + relatedEntityType.getDescription() + " - entity Id " +  relatedEntityId.getValue();
        return fromEntity + toEntity;
    }
}
