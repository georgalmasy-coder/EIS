package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.time.LocalDateTime;
import java.util.Base64;

public class AttachmentRecord {
    private Integer entityAttachmentBlobPK;
    private final CustomerId customerId;
    private final ProjectId projectId;
    private final EntityId entityId;
    private final Version version;
    private final EntityType entityType;
    private final String fileName;
    private final String contentType;
    private final Integer fileSize;
    private final String fileData;
    private final String description;

    private ChangedBy uploadedBy;
    private final ChangedDateTime uploadedDateTime = new ChangedDateTime(LocalDateTime.now());

    private final Boolean deleted;

    public AttachmentRecord(AbstractEntity entity,
                            Integer entityAttachmentBlobPK,
                            String fileName,
                            String contentType,
                            Integer fileSize,
                            String fileData,
                            String description,
                            Integer uploadedByUserId,
                            LocalDateTime uploadedDateTime,
                            Boolean deleted) {
        this.entityAttachmentBlobPK = entityAttachmentBlobPK;
        this.customerId = entity.getCustomerId();
        this.projectId = entity.getProjectId();
        this.entityId = entity.getEntityId();
        this.version = entity.getVersion();
        this.entityType = entity.getEntityType();
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.fileData = fileData;
        this.description = description;
        this.uploadedBy = new ChangedBy(entity.getWebSession());
        this.uploadedBy.setValue(uploadedByUserId != null ? uploadedByUserId : entity.getChangedByUser().getValue());
        this.uploadedDateTime.setValue(uploadedDateTime != null ? uploadedDateTime : LocalDateTime.now());
        this.deleted = deleted;
    }

    public Integer getEntityAttachmentBlobPK() {
        return entityAttachmentBlobPK;
    }

    public void setEntityAttachmentBlobPK(Integer entityAttachmentBlobPK) {
        this.entityAttachmentBlobPK = entityAttachmentBlobPK;
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
    public ChangedBy getChangedByUserId() {
        return uploadedBy;
    }
    public ChangedDateTime getChangedDate() {
        return uploadedDateTime;
    }

    public String getFileName() {
        return fileName;
    }
    public String getContentType() {
        return contentType;
    }
    public Integer getFileSize() {
        return fileSize;
    }
    public String getFileData() {
        return fileData;
    }

    public byte[] getFileDataAsBinary() {
        return fileData != null ? Base64.getDecoder().decode(fileData) : null;
    }

    public String getDescription() {
        return description;
    }
    public boolean isFileDeleted() {
        return deleted;
    }
}
