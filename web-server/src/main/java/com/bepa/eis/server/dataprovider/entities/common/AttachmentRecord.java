package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityType;

import java.time.LocalDateTime;
import java.util.Base64;

public class AttachmentRecord {
    private final Integer entityAttachmentPK;
    private final Integer customerId;
    private final Integer projectId;
    private final Integer entityId;
    private final Integer version;
    private final EntityType entityType;
    private final String fileName;
    private final String contentType;
    private final Integer fileSize;
    private final String fileData;
    private final String description;

    private final Integer uploadedByUserId;
    private final LocalDateTime uploadedDateTime;
    private final Boolean deleted;

    public AttachmentRecord(AbstractEntity entity,
                            Integer entityAttachmentPK,
                            String fileName,
                            String contentType,
                            Integer fileSize,
                            String fileData,
                            String description,
                            Integer uploadedByUserId,
                            LocalDateTime uploadedDateTime,
                            Boolean deleted) {
        this.entityAttachmentPK = entityAttachmentPK;
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
        this.uploadedByUserId = uploadedByUserId != null ? uploadedByUserId : entity.getChangedByUserId();
        this.uploadedDateTime = uploadedDateTime != null ? uploadedDateTime : LocalDateTime.now();
        this.deleted = deleted;
    }

    public Integer getEntityAttachmentPK() {
        return entityAttachmentPK;
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
    public Integer getChangedByUserId() {
        return uploadedByUserId;
    }
    public LocalDateTime getChangedDate() {
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
