package com.bepa.eis.server.entites;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.common.AttachmentRecord;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.entities.common.NoteRecord;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.entites.configuration.EntityConfiguration;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.AbstractDataElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

abstract public class AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(AbstractEntity.class);

    private WebSession webSession;
    private Integer customerId;
    private Integer projectId;
    private Integer entityId;
    private Integer version;
    private final List<AbstractDataElement> listOfDataElements = new ArrayList<>();
    private final List<NoteRecord> listOfNoteRecords = new ArrayList<>();
    private final List<AttachmentRecord> listOfAttachmentRecords = new ArrayList<>();
    private final List<EntityRelationRecord> listOfEntityRelationRecords = new ArrayList<>();
    private Integer changedByUserId;
    private LocalDateTime changedDate = LocalDateTime.now();
    private boolean latest = true;
    private boolean active = true;
    private Boolean prevActiveStatus = null;

    private static Integer prevChangedByUserId;
    private static LocalDateTime prevChangedDate;

    public AbstractEntity() {
    }

    public AbstractEntity(WebSession session) {
        this.webSession = session;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void addDataElement(AbstractDataElement dataElement) {
        listOfDataElements.add(dataElement);
    }

    public List<AbstractDataElement> getListOfDataElements() {
        return listOfDataElements;
    }

    public List<NoteRecord> getListOfEntityNotes() {
        return listOfNoteRecords;
    }

    public List<AttachmentRecord> getListOfEntityAttachments() {
        return listOfAttachmentRecords;
    }

    public List<EntityRelationRecord> getListOfEntityRelationRecords() {
        return listOfEntityRelationRecords;
    }

    abstract public EntityType getEntityType();
    abstract public String getCode();
    abstract public String getName();
    abstract public String getDescription();

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public void setCustomerId(String value) {
        if (value == null) {
            this.customerId = mapProjectIdToCustomerId(projectId);
        } else {

            try {
                this.customerId = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("CustomerId is invalid : " + value);
            }
        }
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setProjectId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ProjectId cannot be null");
        }

        try {
            this.projectId = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ProjectId is invalid : " + value);
        }
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setDateOfChange(LocalDateTime dateOfChange) {
        this.changedDate = dateOfChange;
    }

    public void setDateOfChange(String dateOfChange) {
        if (dateOfChange != null && !dateOfChange.isBlank()) {
            this.changedDate = LocalDateTime.parse(dateOfChange.trim());
            this.prevChangedDate = this.changedDate;
        } else {
            if (this.prevChangedDate != null) {
                this.changedDate = this.prevChangedDate;
            } else {
                this.changedDate = LocalDateTime.now();
            }
        }
    }
    public void setDateOfChange(Timestamp timestamp) {
        this.changedDate = (timestamp != null) ? LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault() ) : LocalDateTime.now();
    }

    public LocalDateTime getChangedDate() {
        return changedDate != null ? changedDate : LocalDateTime.now();
    }

    public void setChangedByUserId(Integer changedBy) {
        this.changedByUserId = changedBy;
    }

    public void setChangedByUserId(String changedBy) {
        if (changedBy != null && !changedBy.isBlank()) {
            try {
                this.changedByUserId = Integer.parseInt(changedBy);
                this.prevChangedByUserId = this.changedByUserId;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ChangedBy is invalid : " + changedBy);
            }
        } else {
            if (this.prevChangedByUserId != null) {
                this.changedByUserId = this.prevChangedByUserId;
            } else {
                this.changedByUserId = 1;
            }
        }
    }
    public Integer getChangedByUserId() {
        return changedByUserId;
    }
    public ChangedBy getChangedByUser() {
        ChangedBy changedBy = new ChangedBy(getWebSession());
        changedBy.setValue(changedByUserId);
        return changedBy;
    }

    public void setEntityId(String entityIdValue) {
        try {
            this.entityId = Integer.parseInt(entityIdValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Entity Id Value is invalid : " + entityIdValue);
        }
    }

    public void setEntityId(Integer entityIdValue) {
        this.entityId = entityIdValue;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setActive(String active) {
        this.active = Boolean.parseBoolean(active);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setPrevActiveStatus(Boolean prevActiveStatus) {
        this.prevActiveStatus = prevActiveStatus;
    }

    public boolean hasActiveStatusChanged() {
        if (prevActiveStatus == null) {
            return false;
        }
        return active != prevActiveStatus;
    }

    private Integer mapProjectIdToCustomerId(Integer projectId) {

        return EntityConfiguration.getInstance().getCustomerIdByProjectId(projectId);
    }

    public boolean validateEntity(boolean isNewEntity) {
        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException("Project Id is invalid : " + projectId);
        }
        if (customerId == null || customerId < 1) {
            throw new IllegalArgumentException("Customer Id is invalid : " + customerId);
        }

        if (getEntityType() == null ) {
            throw new IllegalArgumentException("Unknown entity type : " + "null");
        }
        if ( !isNewEntity && (entityId == null || entityId < 1) ) {
            throw new IllegalArgumentException("Entity Id is invalid : " + entityId);
        }
        if (version == null || version != 1) {
            throw new IllegalArgumentException("Version is invalid : " + entityId);
        }

        if (listOfDataElements.isEmpty()) {
            throw new IllegalArgumentException("List of data elements is empty: " + entityId);
        }

        if (changedByUserId == null || changedByUserId <-1) {
            throw new IllegalArgumentException("Changed By Id is invalid : " + this.toString());
        }

        if (changedDate == null) {
            throw new IllegalArgumentException("Changed Date is invalid : " + this.toString());
        }

        return true;
    }

    public void addNoteRecord(NoteRecord noteRecord) {
        if (noteRecord != null) {
            listOfNoteRecords.add(noteRecord);
        }
    }

    public void addAttachmentRecord(AttachmentRecord attachmentRecord) {
        if (attachmentRecord != null) {
            listOfAttachmentRecords.add(attachmentRecord);
        }
    }

    public void addEntityRelationRecord(EntityRelationRecord entityRelationRecord) {
        if (entityRelationRecord != null) {
            listOfEntityRelationRecords.add(entityRelationRecord);
        }
    }

    public String getSortKey() {
        return null;
    }

    @Override
    public String toString() {
        return "Entity{" + "customerId=" + customerId + ", projectId=" + projectId + ", entityId=" + entityId + ", version=" + version + ", listOfDataElements=" + listOfDataElements + ", changedByUserId=" + changedByUserId + ", changedDate=" + changedDate + ", latest=" + latest + '}';
    }

}
