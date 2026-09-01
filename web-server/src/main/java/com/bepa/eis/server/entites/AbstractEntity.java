package com.bepa.eis.server.entites;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.AttachmentRecord;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.entities.common.LinkRecord;
import com.bepa.eis.server.dataprovider.entities.common.NoteRecord;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.booleans.Latest;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
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
import java.util.Objects;

abstract public class AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(AbstractEntity.class);

    private WebSession webSession;
    private final List<AbstractDataElement> listOfDataElements = new ArrayList<>();
    private final List<NoteRecord> listOfNoteRecords = new ArrayList<>();
    private final List<LinkRecord> listOfLinkRecords = new ArrayList<>();
    private final List<AttachmentRecord> listOfAttachmentRecords = new ArrayList<>();
    private final List<EntityRelationRecord> listOfEntityRelationRecords = new ArrayList<>();
    private Boolean prevActiveStatus = null;

    private static LocalDateTime prevChangedDate;

    private CustomerId customerId;
    private ProjectId projectId;
    private EntityId entityId;
    private Version version;
    private Latest latest;
    private Active active;
    private ChangedDateTime changedDateTime;
    private ChangedBy changedBy;

    public AbstractEntity() {
        initFields();
    }

    public AbstractEntity(WebSession webSession) {
        this.webSession = webSession;
        initFields();
    }

    public AbstractEntity(WebSession webSession, EntityRecord entityRecord) {
        this.webSession = webSession;
        initFields();

        if (entityRecord != null) {
            customerId.setValue(entityRecord.getCustomerId());
            projectId.setValue(entityRecord.getProjectId());
            entityId.setValue(entityRecord.getEntityId());
            version.setValue(entityRecord.getVersion());
            latest.setValue(entityRecord.isLatest());
            active.setValue(entityRecord.isActive());
            changedBy.setValue(entityRecord.getChangedByUserId());
            changedDateTime.setValue(entityRecord.getChangedDateTime().toLocalDateTime());
        } else {
            customerId.setValue(getWebSession().getCustomerId());
            projectId.setValue(getWebSession().getProjectId());
            entityId.setValue(null);
            version.setValue(1);
            latest.setValue(true);
            active.setValue(true);
            changedBy.setValue(getWebSession().getUserId());
            changedDateTime.setValue(LocalDateTime.now());
        }
    }

    private void initFields() {
        customerId = new CustomerId();
        projectId = new ProjectId();
        entityId = new EntityId();
        version = new Version();
        latest = new Latest(true);
        active = new Active(true);
        changedDateTime = new ChangedDateTime( LocalDateTime.now());
        changedBy = new ChangedBy(getWebSession());
        initializeFields();
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

    public List<LinkRecord> getListOfEntityLinks() {
        return listOfLinkRecords;
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

    abstract public void initializeFields();
    abstract public void addAllFieldElementsForList(Entity entityElement);
    abstract public void addAllFieldElementsForEdit(Entity entityElement);
    abstract public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId);

    public void setCustomerId(Integer customerId) {
        this.customerId.setValue(customerId);
    }

    public void setCustomerId(String value) {
        if (value == null) {
            this.customerId.setValue(mapProjectIdToCustomerId(projectId.getValue()));
        } else {

            try {
                this.customerId.setValue(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("CustomerId is invalid : " + value);
            }
        }
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId.setValue(projectId);
    }

    public void setProjectId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ProjectId cannot be null");
        }

        try {
            this.projectId.setValue(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ProjectId is invalid : " + value);
        }
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public void setDateOfChange(LocalDateTime dateOfChange) {
        this.changedDateTime.setValue(dateOfChange);
    }

    public void setDateOfChange(String dateOfChange) {
        if (dateOfChange != null && !dateOfChange.isBlank()) {
            this.changedDateTime.setValue(LocalDateTime.parse(dateOfChange.trim()));
            this.prevChangedDate = this.changedDateTime.getValue();
        } else {
            this.changedDateTime.setValue(Objects.requireNonNullElseGet(this.prevChangedDate, LocalDateTime::now));
        }
    }
    public void setDateOfChange(Timestamp timestamp) {
        this.changedDateTime.setValue( (timestamp != null) ? LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault() ) : LocalDateTime.now());
    }


    public ChangedDateTime getChangedDate() {
        if (changedDateTime.getValue() == null) {
            this.changedDateTime.setValue(LocalDateTime.now());
        }
        return changedDateTime;
    }

    public void setChangedByUserId(Integer changedBy) {
        this.changedBy.setValue(changedBy);
    }

    public ChangedBy getChangedBy() {
        return changedBy;
    }

    public ChangedBy getChangedByUser() {
        if (changedBy.getValue() == null) {
            changedBy.setValue(getWebSession().getUserId());
        }
        return changedBy;
    }

    public void setEntityId(String entityId) {
        try {
            this.entityId.setValue(Integer.parseInt(entityId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Entity Id Value is invalid : " + entityId);
        }
    }

    public void setEntityId(Integer entityId) {
        this.entityId.setValue(entityId);
    }

    public EntityId getEntityId() {
        return entityId;
    }

    public void setVersion(Integer version) {
        this.version.setValue(version);
    }

    public Version getVersion() {
        return version;
    }

    public Latest getLatest() {
        return latest;
    }

    public boolean isLatest() {
        return latest.getValue();
    }

    public void setActive(String active) {
        this.active.setValue(Boolean.parseBoolean(active));
    }

    public void setActive(boolean active) {
        this.active.setValue(active);
    }

    public boolean isActive() {
        return active.getValue();
    }

    public Active getActive() {
        return active;
    }

    public void setPrevActiveStatus(Boolean prevActiveStatus) {
        this.prevActiveStatus = prevActiveStatus;
    }

    public boolean hasActiveStatusChanged() {
        if (prevActiveStatus == null) {
            return false;
        }
        return active.getValue() != prevActiveStatus;
    }

    private Integer mapProjectIdToCustomerId(Integer projectId) {

        return EntityConfiguration.getInstance().getCustomerIdByProjectId(projectId);
    }

    public boolean validateEntity(boolean isNewEntity) {
        if (projectId.getValue() == null || projectId.getValue() < 1) {
            throw new IllegalArgumentException("Project Id is invalid : " + projectId);
        }
        if (customerId.getValue() == null || customerId.getValue() < 1) {
            throw new IllegalArgumentException("Customer Id is invalid : " + customerId.getValue());
        }

        if (getEntityType() == null ) {
            throw new IllegalArgumentException("Unknown entity type : " + "null");
        }
        if ( !isNewEntity && (entityId.getValue() == null || entityId.getValue() < 1) ) {
            throw new IllegalArgumentException("Entity Id is invalid : " + entityId);
        }
        if (version.getValue() == null || version.getValue() != 1) {
            throw new IllegalArgumentException("Version is invalid : " + entityId);
        }

        if (listOfDataElements.isEmpty()) {
            throw new IllegalArgumentException("List of data elements is empty: " + entityId);
        }

        if (changedBy.getValue() == null || changedBy.getValue() <-1) {
            throw new IllegalArgumentException("Changed By Id is invalid : " + this);
        }

        if (changedDateTime.getValue() == null) {
            throw new IllegalArgumentException("Changed Date is invalid : " + this);
        }

        return true;
    }

    public void addNoteRecord(NoteRecord noteRecord) {
        if (noteRecord != null) {
            listOfNoteRecords.add(noteRecord);
        }
    }

    public void addLinkRecord(LinkRecord linkRecord) {
        if (linkRecord != null) {
            listOfLinkRecords.add(linkRecord);
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

    public String getSortKeyValue(String sortKey) {

        int index = indexOfFirstDigit(sortKey);

        if (index == -1) {
            return sortKey;
        }

        String numericPart = sortKey.substring(index);

        String[] numericValues = numericPart.split("\\.");
        String sortKeyValue = "";

        for (String numericValue : numericValues) {
            switch (numericValue.length()) {
                case 1:
                    numericValue = "0000" + numericValue;
                    break;
                case 2:
                    numericValue = "000" + numericValue;
                    break;
                case 3:
                    numericValue = "00" + numericValue;
                    break;
                case 4:
                    numericValue = "0" + numericValue;
                    break;

            }
            sortKeyValue = sortKeyValue + "." + numericValue;

        }

        for (int i = numericValues.length; i < 10; i++) {
            sortKeyValue = sortKeyValue + ".00000";
        }

        sortKeyValue = sortKeyValue.replace("..", ".");


        return sortKeyValue;
    }

    private int indexOfFirstDigit(String value) {
        if (value == null) {
            return -1;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                return i;
            }
        }

        return -1;
    }

    @Override
    public String toString() {
        return "Entity{" + "customerId=" + customerId + ", projectId=" + projectId + ", entityId=" + entityId + ", version=" + version + ", listOfDataElements=" + listOfDataElements + ", changedByUserId=" + changedBy + ", changedDate=" + changedDateTime + ", latest=" + latest + '}';
    }

}
